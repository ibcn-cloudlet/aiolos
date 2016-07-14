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
package be.iminds.aiolos.platform;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.felix.service.command.CommandProcessor;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import be.iminds.aiolos.deployment.api.DeploymentManager;
import be.iminds.aiolos.info.NodeInfo;
import be.iminds.aiolos.monitor.node.api.NodeMonitor;
import be.iminds.aiolos.monitor.service.api.ServiceMonitor;
import be.iminds.aiolos.platform.api.PlatformManager;
import be.iminds.aiolos.platform.command.PlatformCommands;
import be.iminds.aiolos.proxy.api.ProxyManager;
import be.iminds.aiolos.util.log.Logger;

public class Activator implements BundleActivator {

	public static Logger logger;
	
	private ServiceTracker<EndpointListener, EndpointListener> nodeTracker;
	
	private ServiceTracker<DeploymentManager, DeploymentManager> dmTracker;
	private ServiceTracker<ProxyManager, ProxyManager> proxyTracker;
	private ServiceTracker<ServiceMonitor, ServiceMonitor> serviceMonitorTracker;
	private ServiceTracker<NodeMonitor, NodeMonitor> nodeMonitorTracker;

	private BundleContext context;
	
	@Override
	public void start(final BundleContext context) throws Exception {
		this.context = context;
		logger = new Logger(context);
		logger.open();
		
		/*
		 * For now this just starts a VM and starts (all) application
		 * bundles of the repository on that one VM instance
		 * 
		 * Should do something more intelligent...
		 *  i.e. scaling / offloading / migrating /...
		 */
		Hashtable<String, Object> properties = new Hashtable<String, Object>();
		properties.put("service.exported.interfaces", new String[] { PlatformManager.class.getName() });

		
		final PlatformManagerImpl platformManager = new PlatformManagerImpl(context);

		ServiceRegistration<PlatformManager> reg = context.registerService(PlatformManager.class, platformManager, properties);
		logger.setServiceReference(reg.getReference());
		
		
		// Node tracker to be able to recognize nodes that 
		// are not started by the app manager itself, but
		// for example discovered in the network
		nodeTracker = new ServiceTracker<EndpointListener, EndpointListener>(context, EndpointListener.class, 
				new ServiceTrackerCustomizer<EndpointListener, EndpointListener>() {
					@Override
					public EndpointListener addingService(
							ServiceReference<EndpointListener> reference) {
						EndpointListener endpointListener = context.getService(reference);
						String nodeId = getNodeId(reference);
						String ip = getIP(reference);
						int rsaPort = getRSAPort(reference);
						String name = (String) reference.getProperty("node.name");
						String arch = (String) reference.getProperty("node.arch");
						String os = (String) reference.getProperty("node.os");
						if(nodeId!=null)
							try {
								platformManager.addNode(new NodeInfo(nodeId, ip, rsaPort, -1, name, arch, os));
							} catch(Exception e){
								logger.log(LogService.LOG_ERROR, "Error adding node", e);
							}
						return endpointListener;
					}

					@Override
					public void modifiedService(
							ServiceReference<EndpointListener> reference,
							EndpointListener service) {}

					@Override
					public void removedService(
							ServiceReference<EndpointListener> reference,
							EndpointListener service) {
						String nodeId = getNodeId(reference);
						if(nodeId!=null)
							platformManager.removeNode(nodeId);
						
						context.ungetService(reference);
					}
			
				});
		nodeTracker.open();
		
		dmTracker = new ServiceTracker<DeploymentManager, DeploymentManager>(context, DeploymentManager.class, 
				new ServiceTrackerCustomizer<DeploymentManager, DeploymentManager>() {

					@Override
					public DeploymentManager addingService(
							ServiceReference<DeploymentManager> reference) {
						DeploymentManager deploymentManager = context.getService(reference);
						String frameworkId = getNodeId(reference);
						if(frameworkId!=null)
							platformManager.setDeploymentManager(frameworkId, deploymentManager);
							
						return deploymentManager;
					}

					@Override
					public void modifiedService(
							ServiceReference<DeploymentManager> reference,
							DeploymentManager service) {}

					@Override
					public void removedService(
							ServiceReference<DeploymentManager> reference,
							DeploymentManager service) {
						String frameworkId = getNodeId(reference);
						if(frameworkId!=null)
							platformManager.unsetDeploymentManager(frameworkId);
						
						context.ungetService(reference);
					}
				});
		dmTracker.open();
		
		proxyTracker = new ServiceTracker<ProxyManager, ProxyManager>(context, ProxyManager.class, 
				new ServiceTrackerCustomizer<ProxyManager, ProxyManager>() {

					@Override
					public ProxyManager addingService(
							ServiceReference<ProxyManager> reference) {
						ProxyManager proxyManager = context.getService(reference);
						String frameworkId = getNodeId(reference);
						if(frameworkId!=null)
							platformManager.setProxyManager(frameworkId, proxyManager);
							
						return proxyManager;
					}

					@Override
					public void modifiedService(
							ServiceReference<ProxyManager> reference,
							ProxyManager service) {}

					@Override
					public void removedService(
							ServiceReference<ProxyManager> reference,
							ProxyManager service) {
						String frameworkId = getNodeId(reference);
						if(frameworkId!=null)
							platformManager.unsetProxyManager(frameworkId);
						
						context.ungetService(reference);
					}
				});
		proxyTracker.open();
		
		serviceMonitorTracker = new ServiceTracker<ServiceMonitor, ServiceMonitor>(context, ServiceMonitor.class, 
				new ServiceTrackerCustomizer<ServiceMonitor, ServiceMonitor>() {

					@Override
					public ServiceMonitor addingService(
							ServiceReference<ServiceMonitor> reference) {
						ServiceMonitor monitor = context.getService(reference);
						String frameworkId = getNodeId(reference);
						if(frameworkId!=null)
							platformManager.setServiceMonitor(frameworkId, monitor);
							
						return monitor;
					}

					@Override
					public void modifiedService(
							ServiceReference<ServiceMonitor> reference,
							ServiceMonitor service) {}

					@Override
					public void removedService(
							ServiceReference<ServiceMonitor> reference,
							ServiceMonitor service) {
						String frameworkId = getNodeId(reference);
						if(frameworkId!=null)
							platformManager.unsetServiceMonitor(frameworkId);
						
						context.ungetService(reference);
					}
				});
		serviceMonitorTracker.open();
		
		nodeMonitorTracker = new ServiceTracker<NodeMonitor, NodeMonitor>(context, NodeMonitor.class, 
				new ServiceTrackerCustomizer<NodeMonitor, NodeMonitor>() {

					@Override
					public NodeMonitor addingService(
							ServiceReference<NodeMonitor> reference) {
						NodeMonitor monitor = context.getService(reference);
						String frameworkId = getNodeId(reference);
						if(frameworkId!=null)
							platformManager.setNodeMonitor(frameworkId, monitor);
							
						return monitor;
					}

					@Override
					public void modifiedService(
							ServiceReference<NodeMonitor> reference,
							NodeMonitor service) {}

					@Override
					public void removedService(
							ServiceReference<NodeMonitor> reference,
							NodeMonitor service) {
						String frameworkId = getNodeId(reference);
						if(frameworkId!=null)
							platformManager.unsetNodeMonitor(frameworkId);
						
						context.ungetService(reference);
					}
				});
		nodeMonitorTracker.open();
	
		// GoGo Shell
		// add shell commands (try-catch in case no shell available)
		PlatformCommands commands = new PlatformCommands(platformManager);
		Dictionary<String, Object> commandProps = new Hashtable<String, Object>();
		try {
			commandProps.put(CommandProcessor.COMMAND_SCOPE, "aiolos");
			commandProps.put(CommandProcessor.COMMAND_FUNCTION, new String[] {"nodes", "start", "stop", "scale"});
			context.registerService(Object.class, commands, commandProps);
		} catch (Throwable t) {
			// ignore exception, in that case no GoGo shell available
		}

	}

	@Override
	public void stop(BundleContext context) throws Exception {
		
		serviceMonitorTracker.close();
		proxyTracker.close();
		serviceMonitorTracker.close();
		nodeMonitorTracker.close();
		
		nodeTracker.close();
		
		logger.close();
	}

	private String getNodeId(ServiceReference<?> reference){
		// if remote, then framework id is set by endpoint.framework.uuid
		String frameworkId = (String)reference.getProperty("endpoint.framework.uuid");
		if(frameworkId==null && context!=null){
			// else service is running locally
			frameworkId = context.getProperty("org.osgi.framework.uuid");
		}
		return frameworkId;
	}
	
	private String getIP(ServiceReference<?> reference){
		String ip = (String)reference.getProperty("endpoint.id");
		if(ip==null){
			ip =  getLocalIP();
		} else {
			ip = ip.substring(ip.lastIndexOf("/")+1, ip.lastIndexOf(":"));
		}
		return ip;
	}
	
	private String getLocalIP(){
		String ip = null;
		try {
			Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
			for (NetworkInterface netint : Collections.list(nets)){
				Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
				for (InetAddress inetAddress : Collections.list(inetAddresses)) {
					 if(inetAddress instanceof Inet4Address){
						 if((inetAddress.isLoopbackAddress() || inetAddress.isAnyLocalAddress()))
							 break;  //only set loopbackadres if no other possible
						 else {	 
							 ip = inetAddress.getHostAddress();
				     		 break;
						 }
				     }
				}
				if(ip!=null){ 
					break;
				}
		    }
		}catch(Exception e){}
		
		if(ip==null){
			ip = "localhost";
		}
		return ip;
	}
	
	private int getRSAPort(ServiceReference<?> reference){
		String ip = (String)reference.getProperty("endpoint.id");
		if(ip==null){
			// TODO get local port?
			String port = context.getProperty("rsa.port");
			if(port!=null){
				return Integer.parseInt(port);
			}
		} else {
			String port = ip.substring(ip.lastIndexOf(":")+1, ip.lastIndexOf("#"));
			return Integer.parseInt(port);
		}
		return -1;
	}
}
