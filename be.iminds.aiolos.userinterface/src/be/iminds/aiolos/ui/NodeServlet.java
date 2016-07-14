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
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.log.LogService;
import org.osgi.service.repository.Repository;

import be.iminds.aiolos.info.ComponentInfo;
import be.iminds.aiolos.info.NodeInfo;
import be.iminds.aiolos.platform.api.PlatformManager;
import be.iminds.aiolos.resource.CapabilityRequirementImpl;

public class NodeServlet extends CommonServlet {
	

	private static final long serialVersionUID = 6255542952954924848L;
	private static final String LABEL = "aiolos-nodes";
	private static final String TITLE = "%aiolos.nodes.pluginTitle";
	private static final String CATEGORY = "Aiolos";
	private static final String CSS[] = { "/" + LABEL + "/res/ui/aiolos.css" };
    private static final String TEMPLATE = "/templates/nodes.html";
	
	public NodeServlet(BundleContext bundleContext) {
		super(LABEL, TITLE, CATEGORY, CSS, TEMPLATE, bundleContext);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		final PlatformManager platformManager = platformTracker.getService();
		final String action = (String) request.getParameter("action");
		
		Future<?> task = null;
		if (action != null)
		if (action.equals("custom")) {
			final String bndrun = request.getParameter("bndrun");
			Activator.logger.log(LogService.LOG_INFO, "Starting custom node: " + bndrun);
			task = executor.submit(new Runnable() {
				@Override
				public void run() {
					try {
						NodeInfo node = platformManager.startNode(bndrun);
						Activator.logger.log(LogService.LOG_INFO, "New node started: " + node.getNodeId());
					} catch (Exception e) {
						Activator.logger.log(LogService.LOG_ERROR, "Error starting new node", e);
					}
				}
			});
		} else if (action.equals("stop")) {
			final String nodeId = (String) request.getParameter("id");
			task = executor.submit(new Runnable() {
				@Override
				public void run() {
					try {
						platformManager.stopNode(nodeId);
						Activator.logger.log(LogService.LOG_INFO, "Stopped node " + nodeId);
					} catch (Exception e) {
						Activator.logger.log(LogService.LOG_ERROR, "Error stopping node " + nodeId, e);
					}
				}
			});
		} else if (action.equals("startComponent")) {
			final String component = request.getParameter("component");
			final String nodeId = (String) request.getParameter("id");
			String[] split = component.split(" ");
			final String componentId = split[0];
			final String version = split[1].substring(1, split[1].length()-1);
			task = executor.submit(new Runnable() {
				@Override
				public void run() {
					try {
						platformManager.startComponent(componentId, version, nodeId);
					} catch (Exception e) {
						Activator.logger.log(LogService.LOG_ERROR, "Error starting component "+componentId+" on "+nodeId+" "+ e.getLocalizedMessage(), e);
					}
				}
			});
		} else if (action.equals("stopComponent")) {
			final String component = request.getParameter("component");
			final String nodeId = (String) request.getParameter("id");
			String[] split = component.split(" ");
			final String componentId = split[0];
			final String version = split[1].substring(1, split[1].length()-1);
			task = executor.submit(new Runnable() {
				@Override
				public void run() {
					try {
						// TODO does not work ...
						platformManager.stopComponent(new ComponentInfo(componentId, version, nodeId));
					} catch (Exception e) {
						Activator.logger.log(LogService.LOG_ERROR, "Error stopping component "+componentId+" on "+nodeId+" "+ e.getLocalizedMessage(), e);
					}
				}
			});
		}
		
		try {
			if (task != null) {
				task.get(); //result not important just wait.
			}
			renderJSON(response, request.getLocale());
		} catch (InterruptedException e) {
			Activator.logger.log(LogService.LOG_ERROR, "Action interrupted: " + action, e);
			super.doPost(request, response);
		} catch (ExecutionException e) {
			Activator.logger.log(LogService.LOG_ERROR, "Action failed to execute: " + action, e);
			super.doPost(request, response);
		} 
	}
	
	@SuppressWarnings("unchecked")
	protected void writeJSON(final Writer w, final Locale locale) throws IOException {
		final Collection<NodeInfo> allNodes = this.getNodes();
		final Object[] status = getStatus(allNodes);

		JSONObject obj = new JSONObject();

		JSONArray stat = new JSONArray();
		for (int i=0; i < status.length; i++) stat.add(status[i]);
		obj.put("status", stat);
		
		JSONArray nodes = new JSONArray();
		for (NodeInfo node : allNodes) {
			JSONObject nodeObj = new JSONObject();
			JSONArray components = new JSONArray();
			
			// TODO extend functionality
			nodeObj.put("id", node.getNodeId());
			nodeObj.put("state", "Unknown"); // TODO hardcoded in javascript
			components.addAll(getComponents(node.getNodeId()));
			nodeObj.put("components", components);
			nodeObj.put("ip", node.getIP());
//			nodeObj.put("proxy", node.getProxyInfoMap());
//			nodeObj.put("hostname", node.getHostname());
			if(node.getHttpPort()!=-1)
				nodeObj.put("console", "http://"+node.getIP()+":"+node.getHttpPort()+"/system/console");
//			nodeObj.put("stoppable", (node.getVMInstance()!=null));
			nodes.add(nodeObj);
		}
		obj.put("nodes", nodes);
		
		JSONArray components = new JSONArray();
		components.addAll(getComponents());
		obj.put("components", components);
		
		try {
			JSONArray bndruns = new JSONArray();
			bndruns.addAll(cloudTracker.getService().getBndrunFiles());
			obj.put("bndruns", bndruns);
		} catch (NullPointerException e) {}
		
		w.write(obj.toJSONString());
	}
	
	private Object[] getStatus(Collection<NodeInfo> allNodes) {
		Object[] ret = new Object[1];
		int nodes = allNodes.size();
        ret[0] = nodes;
        return ret;
	}

	private Collection<NodeInfo> getNodes() {
		List<NodeInfo> nodes = new ArrayList<NodeInfo>();
		PlatformManager platformManager = platformTracker.getService();
		if(platformManager!=null){
			nodes.addAll(platformManager.getNodes());
		}
		Collections.sort(nodes, new Comparator<NodeInfo>(){
			@Override
			public int compare(NodeInfo o1, NodeInfo o2) {
				return o1.getNodeId().compareTo(o2.getNodeId());
			}
		});
		return nodes;
	}
	
	private Collection<String> getComponents(String nodeId) {
		Set<String> components = new HashSet<String>();

		PlatformManager platformManager = platformTracker.getService();
		for(ComponentInfo info : platformManager.getComponents(nodeId)){
			components.add(info.getComponentId()+" ("+info.getVersion()+")");
		}
		
		List<String> sortedList = new ArrayList<String>(components);
		Collections.sort(sortedList);
		return sortedList;
	}
	
	private Collection<String> getComponents(){
		Collection<String> components = new HashSet<String>();
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
						components.add(componentId+" ("+version+")");
					}
				}
			} catch (Exception e) {}
		}
		return components;
	}
}
