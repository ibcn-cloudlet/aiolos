/*******************************************************************************
 * AIOLOS  - Framework for dynamic distribution of software components at runtime.
 * Copyright (C) 2014-2016  iMinds - IBCN - UGent
 *
 * This file is part of AIOLOS.
 *
 * AIOLOS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *     Tim Verbelen, Steven Bohez, Elias Deconinck
 *******************************************************************************/
package be.iminds.aiolos.ui;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.Repository;
import org.osgi.service.repository.RepositoryContent;
import org.osgi.util.tracker.ServiceTracker;

import be.iminds.aiolos.info.ComponentInfo;
import be.iminds.aiolos.info.NodeInfo;
import be.iminds.aiolos.info.ServiceInfo;
import be.iminds.aiolos.monitor.node.api.NodeMonitorInfo;
import be.iminds.aiolos.monitor.service.api.MethodMonitorInfo;
import be.iminds.aiolos.monitor.service.api.ServiceMonitorInfo;
import be.iminds.aiolos.platform.api.PlatformManager;
import be.iminds.aiolos.proxy.api.ProxyInfo;
import be.iminds.aiolos.resource.CapabilityRequirementImpl;

public class DemoServlet extends HttpServlet {
	
	protected final BundleContext context;
	protected final ServiceTracker<PlatformManager, PlatformManager> platformTracker;
	protected final ServiceTracker<Repository, Repository> repositoryTracker;
	
	private final DecimalFormat floatFormat = new DecimalFormat("0.00"); 
	private final DecimalFormat intFormat = new DecimalFormat("0"); 
	
	public DemoServlet(BundleContext context){
		this.context = context;
		this.platformTracker = new ServiceTracker<PlatformManager, PlatformManager>(context, PlatformManager.class, null);
		this.repositoryTracker = new ServiceTracker<Repository, Repository>(context, Repository.class, null);
		this.platformTracker.open();
		this.repositoryTracker.open();
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		final String action = (String) request.getParameter("action");
		if(action!=null){
		    if(action.equals("repository")) {
		    	// get repository
		    	getRepositoryBundles(response.getWriter());
		    } else if(action.equals("status")){
		    	// get platform status ( = graph of nodes/components )
		    	getStatus(response.getWriter());
		    } else if(action.equals("details")){
		    	// get node/component details
		    	final String component = (String) request.getParameter("component");
		    	final String node = (String) request.getParameter("node");
		    	
		    	if(component!=null){
		    		int index2 = component.lastIndexOf('@');
		    		int index1 = component.substring(0, index2).lastIndexOf('-');
		    		String componentId = component.substring(0, index1);
		    		String version = component.substring(index1+1, index2);
		    		String nodeId = component.substring(index2+1);
		    		getComponentDetails(response.getWriter(),
		    				componentId, version, nodeId);
		    	} else if(node!=null){
		    		getNodeDetails(response.getWriter(), node);
		    	}
		    } else if(action.equals("start")){
		    	final String component = (String) request.getParameter("component");
		    	final String target = (String) request.getParameter("target");
		    	
		    	int index1 = component.lastIndexOf('-');
		    	String componentId = component.substring(0, index1);
	    		String version = component.substring(index1+1);
	    		
	    		startComponent(response.getWriter(), componentId, version, target);
		    	
		    } else if(action.equals("migrate")){
		    	final String component = (String) request.getParameter("component");
		    	final String to = (String) request.getParameter("target");
		    	
		    	int index2 = component.lastIndexOf('@');
	    		int index1 = component.substring(0, index2).lastIndexOf('-');
	    		String componentId = component.substring(0, index1);
	    		String version = component.substring(index1+1, index2);
	    		String from = component.substring(index2+1);
		    	
	    		migrateComponent(response.getWriter(), componentId, version, from, to);
		    } else if(action.equals("stop")){
		    	final String component = (String) request.getParameter("component");
		    	int index2 = component.lastIndexOf('@');
	    		int index1 = component.substring(0, index2).lastIndexOf('-');
	    		String componentId = component.substring(0, index1);
	    		String version = component.substring(index1+1, index2);
	    		String nodeId = component.substring(index2+1);
	    		
	    		stopComponent(response.getWriter(), componentId, version, nodeId);
		    } else {
		    	response.sendRedirect("demo/aiolos.html");
		    }
		} else {
			response.sendRedirect("demo/aiolos.html");
		}
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
	
	}

	
	private void getRepositoryBundles(PrintWriter writer){
		JSONArray components = new JSONArray();
		
		Repository[] repos = new Repository[] {};
		repos = repositoryTracker.getServices(repos);
		for (Repository repo : repos) {
			try {
				CapabilityRequirementImpl requirement = new CapabilityRequirementImpl(
						"osgi.identity", null);
				requirement.addDirective("filter",String.format("(%s=%s)", "osgi.identity", "*"));

				Map<Requirement, Collection<Capability>> result = repo
						.findProviders(Collections.singleton(requirement));

				for (Capability c : result.values().iterator().next()) {
					String type = (String) c.getAttributes().get("type");
					if (type != null && type.equals("osgi.bundle")) {
						String componentId = (String) c.getAttributes().get("osgi.identity");
						String version = c.getAttributes().get("version").toString();
						String name = null;
						String description = null;
						try {
							RepositoryContent content = (RepositoryContent) c.getResource();	
							JarInputStream jar = new JarInputStream(content.getContent());
							Manifest mf = jar.getManifest();
							Attributes attr = mf.getMainAttributes();
							name = attr.getValue("Bundle-Name");
							description = attr.getValue("Bundle-Description");
						} catch(Exception e){
							e.printStackTrace();
						}
						
						JSONObject component = new JSONObject();
						component.put("componentId", componentId);
						component.put("version", version);
						component.put("name", name);
						component.put("description", description);
						components.add(component);
					}
				}
			} catch (Exception e) {}
		}
		
		writer.write(components.toJSONString());
	}
	
	private void getStatus(PrintWriter writer){
		JSONObject status = new JSONObject();
		JSONArray nodes = new JSONArray();
		JSONArray components = new JSONArray();
		JSONArray links = new JSONArray();
		
		PlatformManager platform = platformTracker.getService();
		if(platform!=null){
			// add all nodes
			for(NodeInfo node : platform.getNodes()){
				JSONObject n = new JSONObject();
				n.put("id", node.getNodeId());
				n.put("name", node.getName());
				n.put("os", node.getOS());
				n.put("arch", node.getArch());
				nodes.add(n);
			}
			// add all components
			for(ComponentInfo c : platform.getComponents()){
				String componentId = c.getComponentId();
				String version = c.getVersion();
				String frameworkId = c.getNodeId();
				String name = c.getName();
				
				JSONObject component = new JSONObject();
				component.put("componentId", componentId);
				component.put("version", version);
				component.put("frameworkId", frameworkId);
				component.put("name", name);
				components.add(component);
			}
			// fetch links using proxies and their using bundles
			for(ProxyInfo p : platform.getProxies()){
				List<String> from = new ArrayList<String>();
				List<String> to = new ArrayList<String>();
				for(ServiceInfo i : p.getInstances()){
					to.add(i.getComponentId()+"-"+i.getVersion()+"@"+i.getNodeId());
				}
				for(ComponentInfo c : p.getUsers()){
					from.add(c.getComponentId()+"-"+c.getVersion()+"@"+c.getNodeId());
				}
				
				for(String f : from){
					for(String t : to){
						JSONObject link = new JSONObject();
						link.put("from", f);
						link.put("to", t);
						links.add(link);
					}
				}
			}
			
		}
		status.put("nodes", nodes);
		status.put("components", components);
		status.put("links", links);

		writer.write(status.toJSONString());
	}
	
	private void getNodeDetails(PrintWriter writer, String nodeId){
		JSONObject nodeInfo = new JSONObject();
		
		PlatformManager platform = platformTracker.getService();
		if(platform!=null){
			NodeInfo ni = platform.getNode(nodeId);
			NodeMonitorInfo nmi = platform.getNodeMonitorInfo(nodeId);
			
			if(ni!=null){
				nodeInfo.put("nodeId", ni.getNodeId());
				nodeInfo.put("name", ni.getName());
				nodeInfo.put("ip", ni.getIP());
				nodeInfo.put("cores", nmi.getNoCpuCores());
				nodeInfo.put("cpu", floatFormat.format(nmi.getCpuUsage()));
				nodeInfo.put("os", ni.getOS());
				nodeInfo.put("arch", ni.getArch());
			}
		}
		
		writer.write(nodeInfo.toJSONString());
	}
	
	private void getComponentDetails(PrintWriter writer, String componentId, String version, String nodeId){
		JSONObject componentInfo = new JSONObject();
		
		componentInfo.put("componentId", componentId);
		componentInfo.put("version", version);
		
		PlatformManager platform = platformTracker.getService();
		if(platform!=null){
			ComponentInfo c = platform.getComponent(componentId, version, nodeId);
			componentInfo.put("name", c.getName());
			
			// use name as id instead of UUID
			NodeInfo ni = platform.getNode(nodeId);
			componentInfo.put("nodeId", ni.getName());
			
			JSONArray services = new JSONArray();
			for(ProxyInfo p : platform.getProxies(c)){
				JSONObject serviceInfo = new JSONObject();
				String serviceId = p.getServiceId();
				// make the serviceId cleaner for demo
				StringTokenizer st = new StringTokenizer(serviceId, ",");
				String cleanServiceId = "";
				while(st.hasMoreTokens()){
					String service = st.nextToken();
					int startIndex = service.lastIndexOf('.');
					int endIndex = service.indexOf('-');
					if(startIndex!=-1 && endIndex!=-1){
						cleanServiceId += service.substring(startIndex+1, endIndex);
					} else if(startIndex!=-1){
						cleanServiceId += service.substring(startIndex+1);
					}
					if(st.hasMoreTokens()){
						cleanServiceId +=",";
					}
				}
				
				serviceInfo.put("serviceId", cleanServiceId);
				
				ServiceInfo s = null;
				for(ServiceInfo si : p.getInstances()){
					if(si.getComponent().equals(c)){
						s = si;
						break;
					}
				}
				
				if(s!=null){
					ServiceMonitorInfo smi = platform.getServiceMonitorInfo(s);
					
					if(smi!=null){
						JSONArray methods = new JSONArray();
						for(MethodMonitorInfo mmi : smi.getMethods()){
							JSONObject method = new JSONObject();
							method.put("methodName", mmi.getName());
							method.put("time", floatFormat.format(mmi.getTime()));
							method.put("arg", intFormat.format(mmi.getArgSize()));
							method.put("ret", intFormat.format(mmi.getRetSize()));
							
							methods.add(method);
						}
						serviceInfo.put("methods", methods);
					}
				}
				
				services.add(serviceInfo);
			}
			
			componentInfo.put("services", services);
		}
		
		writer.write(componentInfo.toJSONString());
	}
	
	private void startComponent(PrintWriter writer, String componentId, String version, String nodeId){
		
		JSONObject result = new JSONObject();
		PlatformManager platform = platformTracker.getService();
		if(platform!=null){
			try {
				platform.startComponent(componentId, version, nodeId);
			} catch(Exception e){
				e.printStackTrace();
			}
		}
		
		writer.write(result.toJSONString());
	}
	
	private void migrateComponent(PrintWriter writer, String componentId, String version, String from, String to){
		
		JSONObject result = new JSONObject();
		PlatformManager platform = platformTracker.getService();
		if(platform!=null){
			try {
				platform.migrateComponent(new ComponentInfo(componentId, version, from), to);
			} catch(Exception e){
				e.printStackTrace();
			}
		}
		
		writer.write(result.toJSONString());
	}
	
private void stopComponent(PrintWriter writer, String componentId, String version, String nodeId){
		
		JSONObject result = new JSONObject();
		PlatformManager platform = platformTracker.getService();
		if(platform!=null){
			try {
				platform.stopComponent(new ComponentInfo(componentId, version, nodeId));
			} catch(Exception e){
				e.printStackTrace();
			}
		}
		
		writer.write(result.toJSONString());
	}
}
