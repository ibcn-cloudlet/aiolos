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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

import be.iminds.aiolos.cloud.api.CloudManager;
import be.iminds.aiolos.cloud.api.VMInstance;
import be.iminds.aiolos.deployment.api.DeploymentManager;
import be.iminds.aiolos.info.ComponentInfo;
import be.iminds.aiolos.info.NodeInfo;
import be.iminds.aiolos.info.ServiceInfo;
import be.iminds.aiolos.monitor.node.api.NodeMonitor;
import be.iminds.aiolos.monitor.node.api.NodeMonitorInfo;
import be.iminds.aiolos.monitor.service.api.ServiceMonitor;
import be.iminds.aiolos.monitor.service.api.ServiceMonitorInfo;
import be.iminds.aiolos.platform.api.PlatformManager;
import be.iminds.aiolos.platform.exception.CloudManagerNotFoundException;
import be.iminds.aiolos.platform.exception.DeploymentManagerNotFoundException;
import be.iminds.aiolos.platform.exception.NodeNotFoundException;
import be.iminds.aiolos.proxy.api.ProxyInfo;
import be.iminds.aiolos.proxy.api.ProxyManager;
import be.iminds.aiolos.proxy.api.ProxyPolicy;
import be.iminds.aiolos.topology.api.TopologyManager;

/**
 * The {@link PlatformManagerImpl} keeps track of all available nodes
 * and all application components running on those nodes. Also provides
 * interface to start/stop/scale/migrate components between nodes and
 * start/stop nodes (if running on the cloud).
 *
 */
public class PlatformManagerImpl implements PlatformManager {

	private BundleContext context;
	private int TIMEOUT = 5000; // which timeout? for now same as rsa.timeout

	private final Map<String, Node> nodes = Collections.synchronizedMap( new HashMap<String, Node>());
	
	public PlatformManagerImpl(BundleContext context){
		this.context = context;
		
		String timeout = context.getProperty("rsa.timeout"); 
		if(timeout!=null){
			TIMEOUT = Integer.parseInt(timeout);
		}
	}

	@Override
	public ComponentInfo startComponent(String componentId, String nodeId)
			throws Exception {
		return startComponent(componentId, null, nodeId);
	}
	
	@Override
	public ComponentInfo startComponent(String componentId, String version, String nodeId)
			throws Exception {
		Node node = nodes.get(nodeId);
		if(node == null)
			throw new NodeNotFoundException(nodeId);
		
		if(node.getDeploymentManager()==null){
			throw new DeploymentManagerNotFoundException(nodeId);
		}
		
		if(version!=null){
			return node.getDeploymentManager().startComponent(componentId, version);
		} else {
			return node.getDeploymentManager().startComponent(componentId);
		}
	}


	@Override
	public void stopComponent(ComponentInfo component)
			throws Exception {
		String nodeId = component.getNodeId();
		Node node = nodes.get(nodeId);
		if(node == null)
			throw new NodeNotFoundException(nodeId);
		
		if(node.getDeploymentManager()==null){
			throw new DeploymentManagerNotFoundException(nodeId);
		}
		
		node.getDeploymentManager().stopComponent(component);
	}

	@Override
	public Collection<ComponentInfo> scaleComponent(String componentId,
			int requestedInstances, boolean forceNew) throws Exception {
		return scaleComponent(componentId, null, requestedInstances, forceNew);
	}
	
	@Override
	public Collection<ComponentInfo> scaleComponent(final String componentId, final String version,
			final int requestedInstances, final boolean forceNew) throws Exception {
		List<ComponentInfo> result = new ArrayList<ComponentInfo>();
		
		// TODO better implement this ...
		int currentInstances = 0;
		
		for(Node n : this.getNodes0()){
			if(n.getDeploymentManager()!=null){
				ComponentInfo component = n.getDeploymentManager().hasComponent(componentId, version);
				if(component!=null){
					currentInstances++;
					result.add(component);
				}
			}
		}
		
		if(currentInstances>requestedInstances){
			// scale down
			final int toStop = currentInstances-requestedInstances;
			Activator.logger.log(LogService.LOG_INFO, "Scaling down - removing "+toStop+" instances of "+componentId);
			// TODO how to determine which one to stop?
			int stop = 0;
			for(Node n : getNodes0()){
				if(n.getDeploymentManager()!=null){
					ComponentInfo  component = n.getDeploymentManager().hasComponent(componentId, version);
					if(component!=null){
						try {
							this.stopComponent(component);
							result.remove(component);
							stop++;
						} catch(Exception e){
							Activator.logger.log(LogService.LOG_ERROR, "Error stopping component", e);
						}
						
						if(stop==toStop)
							break;
					}
				}
			}
			Activator.logger.log(LogService.LOG_INFO, "Scaled down to "+requestedInstances+" instances of component "+componentId);

		} else if(currentInstances<requestedInstances){
			// scale up
			final int toStart = requestedInstances-currentInstances;
			Activator.logger.log(LogService.LOG_INFO, "Scaling up - starting "+toStart+" instances of "+componentId);
			
			// TODO should this contain logic to set ProxyPolicy? or do it automatically in ProxyManager (as is tried now...)
			
			int started = 0;
			if(!forceNew){
				// try to put on existing nodes
				for(Node n : this.getNodes0()){
					if(n.getDeploymentManager()!=null){
						ComponentInfo component = n.getDeploymentManager().hasComponent(componentId, version);
						if(component==null){
							try {
								component = this.startComponent(componentId, version, n.getInfo().getNodeId());
								result.add(component);
								started++;
							}catch(Exception e){
								Activator.logger.log(LogService.LOG_ERROR, "Error starting component "+component, e);
							}
									
							if(started==toStart)
								break;
						}
					}
				}
			}
			
			// if still not enough instances, start new servers (in parallel on different threads)
			int requiredInstances = toStart-started;
			if(requiredInstances>0){	
				List<Future<ComponentInfo>> components = new ArrayList<Future<ComponentInfo>>();
				ExecutorService executor = Executors.newFixedThreadPool(requiredInstances);
				for(int i=0;i<requiredInstances;i++){
					Callable<ComponentInfo> startInstance = new Callable<ComponentInfo>() {
						@Override
						public ComponentInfo call() throws Exception {
							try {
								NodeInfo node = startNode();
								return startComponent(componentId, version, node.getNodeId());
							} catch(Exception e){
								Activator.logger.log(LogService.LOG_ERROR, "Error starting component "+componentId+" on new node", e);
								return null;
							}
						}
					};
					Future<ComponentInfo> componentResult = executor.submit(startInstance);
					components.add(componentResult);
				}
				for(Future<ComponentInfo> componentResult : components){
					try {
						ComponentInfo component = componentResult.get();
						if(component!=null){
							started++;
							result.add(component);
						}
					} catch (Exception e) {
					}
				}
			}
			Activator.logger.log(LogService.LOG_INFO, "Scaled up to "+started+" instances of component "+componentId+", "+(toStart-started)+" failed.");
		}
		
		return result;
	}

	@Override
	public void migrateComponent(ComponentInfo component, String target) throws Exception {
		Activator.logger.log(LogService.LOG_INFO, "Migrating component "+component+" to "+target);

		String componentId = component.getComponentId();
		String version = component.getVersion();
		
	    // start component at target
		startComponent(componentId, version, target);
		
		// wait until imported on node where migrating from (if service is in use over there)
		long t1 = System.currentTimeMillis();
		boolean started = false;
		while(!started && System.currentTimeMillis()-t1 < TIMEOUT){
			started = true;
			for(ProxyInfo p : getProxies(component)){
				if(!p.getServiceId().contains("-") && !p.getUsers().isEmpty()){ // don't take into account services with instanceid
					boolean targetRegistered = false;
					for(ServiceInfo si : p.getInstances()){
						if(si.getNodeId().equals(target)){
							targetRegistered = true;
							break;
						}
					}
					if(!targetRegistered){
						started = false;
					}
				}
			}
			
			if(!started) {
				Thread.sleep(100); // TODO which wait period?
			}
		}
		
		if(!started){
			Activator.logger.log(LogService.LOG_ERROR, "Error migrating component "+componentId+" to node "+target+" (timeout)" );
			throw new Exception( "Error migrating component "+componentId+" to node "+target+" (timeout)");
		}
		
		// stop component at from
		stopComponent(component);
	}

	@Override
	public NodeInfo startNode() throws Exception {
		return startNode("run-vm-empty.bndrun");
	}

	@Override
	public NodeInfo startNode(String bndrun)
			throws Exception {
		// Get reference to the CloudManager and start new VM instance
		ServiceReference<CloudManager> cloudRef = context.getServiceReference(CloudManager.class);
		if(cloudRef==null){
			throw new CloudManagerNotFoundException();
		}
		CloudManager cloudMgr = context.getService(cloudRef);
		
		// copy resources from mgmt?!
		List<String> resources = new ArrayList<String>();
		File dir = new File("resources");
		for(String name : dir.list()){
			resources.add("resources/" + name);
		}
		VMInstance instance = cloudMgr.startVM(bndrun, resources);
		context.ungetService(cloudRef);
		
		// Although the instance is started,
		// we do not know when the VM Runtime is fully initialized
		// and the remote endpoint is available ... try to connect 
		// every second until ok or timeout
	
		int count = 0;
		int timeout = 60;
		NodeInfo temp = null;
		
		String ip = instance.getPublicAddresses().iterator().next();
		int port = instance.getOsgiPort();
		
		while(temp==null){
			// Now connect to the created instance
			ServiceReference<TopologyManager> refRemote = context.getServiceReference(TopologyManager.class);
			if(refRemote==null){
				throw new Exception("No RemoteServiceAdmin available!");
			}
			TopologyManager topologyManager = context.getService(refRemote);
			
			temp = topologyManager.connect(ip, port);
			
			context.ungetService(refRemote);
			
			if(temp==null){
				if(count > timeout){
					throw new Exception("Failed to import EndpointListener");
				} else {
					count++;
					Thread.sleep(1000);
				}
			}
			
		}
	
		String id = temp.getNodeId();

		// also try to wait until DeploymentManager is available
		// this ensures that one can install components after initNode()
		DeploymentManager dm;
		do{
			dm = nodes.get(id).getDeploymentManager();
			if(dm==null){
				if(count > timeout){
					throw new DeploymentManagerNotFoundException(id);
				} else {
					count++;
					Thread.sleep(1000);
				}
			}
		} while(dm == null);
		
		NodeInfo nodeInfo = new NodeInfo(id, ip, 
				instance.getOsgiPort(), 
				instance.getHttpPort(),
				temp.getName(),
				temp.getArch(),
				temp.getOS());
		Node node = addNode(nodeInfo);
		node.setVMInstance(instance);

		return nodeInfo;
	}

	@Override
	public void stopNode(String nodeId) throws Exception {
		Node node = nodes.get(nodeId);
		if(node == null)
			throw new NodeNotFoundException(nodeId);
		
		
		ServiceReference<CloudManager> cloudRef = context.getServiceReference(CloudManager.class);
		if(cloudRef==null){
			throw new CloudManagerNotFoundException();
		}
		
		// TODO server id =/= node id
		VMInstance vmInstance = nodes.get(nodeId).getVMInstance();
		
		// could be null when not running in cloud environment
		if(vmInstance!=null) {
			
			String vmInstanceID = vmInstance.getId();
			
			// first try to stop the osgi runtime
			if(node.getDeploymentManager()!=null){
				node.getDeploymentManager().stopComponent(null);
			}
			
			// then kill the node
			CloudManager cloudMgr = context.getService(cloudRef);
			cloudMgr.stopVM(vmInstanceID);
			context.ungetService(cloudRef);
		}
	}

	@Override
	public Collection<NodeInfo> getNodes() {
		List<NodeInfo> result = new ArrayList<NodeInfo>();
		synchronized(nodes){
			for(Node n : nodes.values()){
				result.add(n.getInfo());
			}
		}
		return Collections.unmodifiableList(result);
	}
	
	@Override
	public NodeInfo getNode(String nodeId) {
		NodeInfo result = null;
		synchronized(nodes){
			Node n = nodes.get(nodeId);
			if(n!=null){
				result = n.getInfo();
			}
		}
		return result;
	}
	
	@Override
	public Collection<ComponentInfo> getComponents() {
		List<ComponentInfo> result = new ArrayList<ComponentInfo>();

		for(Node n : getNodes0()){
			result.addAll(getComponents(n.getNodeId()));
		}
		return Collections.unmodifiableList(result);
	}

	@Override
	public Collection<ComponentInfo> getComponents(String nodeId) {
		List<ComponentInfo> result = new ArrayList<ComponentInfo>();
		
		Node node = nodes.get(nodeId);
		// TODO throw exceptions here?
		if(node == null){
			return Collections.EMPTY_LIST;
		} 
		if(node.getDeploymentManager()==null){
			return Collections.EMPTY_LIST;
		}
		result.addAll(node.getDeploymentManager().getComponents());
		
		return Collections.unmodifiableCollection(result);
	}
	
	@Override
	public Collection<ComponentInfo> getComponents(String componentId,
			String version) {
		// TODO use dedicated method in DeploymentManager
		List<ComponentInfo> result = new ArrayList<ComponentInfo>();
		for(ComponentInfo component : getComponents()){
			if(component.getComponentId().equals(componentId)
					&& component.getVersion().equals(version)){
				result.add(component);
			}
		}
		return result;
	}
	
	@Override
	public ComponentInfo getComponent(String componentId,
			String version, String nodeId) {
		// TODO use dedicated method in DeploymentManager
		Node node = nodes.get(nodeId);
		// TODO throw exceptions here?
		if(node == null){
			return null;
		} 
		if(node.getDeploymentManager()==null){
			return null;
		}
		
		for(ComponentInfo c : node.getDeploymentManager().getComponents()){
			if(c.getComponentId().equals(componentId) && c.getVersion().equals(version)){
				return c;
			}
		}
		return null;
	}
	
	@Override
	public Collection<ServiceInfo> getServices() {
		List<ServiceInfo> result = new ArrayList<ServiceInfo>();

		for(Node n : getNodes0()){
			result.addAll(getServices(n.getNodeId()));
		}
		
		return Collections.unmodifiableList(result);
	}

	@Override
	public Collection<ServiceInfo> getServices(String nodeId) {
		List<ServiceInfo> result = new ArrayList<ServiceInfo>();
		
		Node node = nodes.get(nodeId);
		// TODO throw exceptions here?
		if(node == null){
			return Collections.EMPTY_LIST;
		} 
		if(node.getProxyManager()==null){
			return Collections.EMPTY_LIST;
		}
		result.addAll(node.getProxyManager().getServices());
		
		return Collections.unmodifiableCollection(result);
	}

	@Override
	public Collection<ServiceInfo> getServices(ComponentInfo component) {
		List<ServiceInfo> result = new ArrayList<ServiceInfo>();
		
		Node node = nodes.get(component.getNodeId());
		// TODO throw exceptions here?
		if(node == null){
			return Collections.EMPTY_LIST;
		} 
		if(node.getProxyManager()==null){
			return Collections.EMPTY_LIST;
		}
		result.addAll(node.getProxyManager().getServices(component));
		
		return Collections.unmodifiableCollection(result);
	}

	@Override
	public Collection<ProxyInfo> getProxies() {
		List<ProxyInfo> result = new ArrayList<ProxyInfo>();

		for(Node n : getNodes0()){
			result.addAll(getProxies(n.getNodeId()));
		}
		
		return Collections.unmodifiableList(result);
	}

	@Override
	public Collection<ProxyInfo> getProxies(String nodeId) {
		List<ProxyInfo> result = new ArrayList<ProxyInfo>();
		
		Node node = nodes.get(nodeId);
		// TODO throw exceptions here?
		if(node == null){
			return Collections.EMPTY_LIST;
		} 
		if(node.getProxyManager()==null){
			return Collections.EMPTY_LIST;
		}
		result.addAll(node.getProxyManager().getProxies());
		
		return Collections.unmodifiableCollection(result);
	}


	@Override
	public Collection<ProxyInfo> getProxies(ComponentInfo component) {
		List<ProxyInfo> result = new ArrayList<ProxyInfo>();
		
		Node node = nodes.get(component.getNodeId());
		// TODO throw exceptions here?
		if(node == null){
			return Collections.EMPTY_LIST;
		} 
		if(node.getProxyManager()==null){
			return Collections.EMPTY_LIST;
		}
		result.addAll(node.getProxyManager().getProxies(component));
		
		return Collections.unmodifiableCollection(result);
	}

	@Override
	public void setProxyPolicy(ProxyInfo proxy, ProxyPolicy policy) {
		String nodeId = proxy.getNodeId();
		Node node = nodes.get(nodeId);
		// TODO throw exceptions here?
		if(node == null){
			return;
		} 
		if(node.getProxyManager()==null){
			return;
		}
		node.getProxyManager().setProxyPolicy(proxy, policy);
		
	}
	
	@Override
	public Collection<NodeMonitorInfo> getNodeMonitorInfo() {
		List<NodeMonitorInfo> result = new ArrayList<NodeMonitorInfo>();
		for(Node n : getNodes0()){
			result.add(getNodeMonitorInfo(n.getNodeId()));
		}
		
		return Collections.unmodifiableList(result);
	}

	@Override
	public NodeMonitorInfo getNodeMonitorInfo(String nodeId) {
		Node node = nodes.get(nodeId);
		// TODO throw exceptions here?
		if(node == null){
			return null;
		} 
		return node.getNodeMonitor().getNodeMonitorInfo();
	}

	@Override
	public Collection<ServiceMonitorInfo> getServiceMonitorInfo() {
		List<ServiceMonitorInfo> result = new ArrayList<ServiceMonitorInfo>();
	
		for(Node n : getNodes0()){
			if(n.getProxyManager()==null){
				continue;
			}			
			for(ProxyInfo proxy : n.getProxyManager().getProxies()){
				for(ServiceInfo service : proxy.getInstances()){
					ServiceMonitorInfo monitorInfo = getServiceMonitorInfo(n.getNodeId(), service);
					if(monitorInfo!=null)
						result.add(monitorInfo);
				}
			}
		}
		
		return Collections.unmodifiableList(result);
	}


	@Override
	public ServiceMonitorInfo getServiceMonitorInfo(ServiceInfo service) {
		return getServiceMonitorInfo(service.getNodeId(), service);
	}
	
	// get monitored method calls to service coming from nodeInfo
	@Override
	public ServiceMonitorInfo getServiceMonitorInfo(String nodeId, ServiceInfo service) {
		Node node = nodes.get(nodeId);
		// TODO throw exceptions here?
		if(node == null){
			return null;
		} 
		if(node.getServiceMonitor()==null){
			return null;
		}
		return node.getServiceMonitor().getServiceMonitorInfo(service);
	}
	
	private Collection<Node> getNodes0(){
		// take a copy 
		Collection<Node> copy;
		synchronized(nodes){
			copy = new ArrayList<Node>(nodes.values());
		}
		return copy;
	}

	// dynamically set and remove nodes and their
	// DeploymentManager, ProxyManager and/or BundleMonitor refs
	Node addNode(NodeInfo info) throws Exception {
		Node node;
		synchronized(nodes){
			node = nodes.get(info.getNodeId());
			if(node==null){
				node = new Node(info);
			} else {
				// update NodeInfo (e.g. to update http port when started from cloudmgr)
				// TODO should all nodeinfo properties be given via service properties
				// of the EndpointListener (or another) remote service?
				node.setInfo(info);
			}
			
			nodes.put(info.getNodeId(), node);
		}
		return node;
	}

	void removeNode(String id){
		nodes.remove(id);
	}
	
	public void setDeploymentManager(String id, DeploymentManager deploymentManager) {
		Node n = nodes.get(id);
		if(n!=null)
			n.setDeploymentManager(deploymentManager);
	}

	public void setProxyManager(String id, ProxyManager proxyManager) {
		Node n = nodes.get(id);
		if(n!=null)
			n.setProxyManager(proxyManager);
	}

	public void setServiceMonitor(String id, ServiceMonitor bundleMonitor) {
		Node n = nodes.get(id);
		if(n!=null)
			n.setServiceMonitor(bundleMonitor);
	}
	
	public void setNodeMonitor(String id, NodeMonitor nodeMonitor) {
		Node n = nodes.get(id);
		if(n!=null)
			n.setNodeMonitor(nodeMonitor);
	}

	public void unsetDeploymentManager(String id) {
		Node n = nodes.get(id);
		if(n!=null)
			n.setDeploymentManager(null);
	}

	public void unsetProxyManager(String id) {
		Node n = nodes.get(id);
		if(n!=null)
			n.setProxyManager(null);
	}

	public void unsetServiceMonitor(String id) {
		Node n = nodes.get(id);
		if(n!=null)
			n.setServiceMonitor(null);
	}
	
	public void unsetNodeMonitor(String id) {
		Node n = nodes.get(id);
		if(n!=null)
			n.setNodeMonitor(null);
	}

}
