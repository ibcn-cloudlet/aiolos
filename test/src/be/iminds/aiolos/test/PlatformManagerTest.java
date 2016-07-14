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
package be.iminds.aiolos.test;

import static org.knowhowlab.osgi.testing.assertions.ServiceAssert.assertServiceAvailable;

import java.lang.reflect.Method;
import java.util.Collection;

import junit.framework.TestCase;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.repository.Repository;

import be.iminds.aiolos.deployment.api.DeploymentManager;
import be.iminds.aiolos.info.ComponentInfo;
import be.iminds.aiolos.info.NodeInfo;
import be.iminds.aiolos.info.ServiceInfo;
import be.iminds.aiolos.monitor.node.api.NodeMonitor;
import be.iminds.aiolos.monitor.service.api.ServiceMonitor;
import be.iminds.aiolos.platform.api.PlatformManager;
import be.iminds.aiolos.proxy.api.ProxyInfo;
import be.iminds.aiolos.proxy.api.ProxyManager;
import be.iminds.aiolos.proxy.api.ProxyPolicy;

public class PlatformManagerTest extends TestCase {

	private final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();  
	
	private PlatformManager pm;
	
	public void setUp(){
		assertServiceAvailable(Repository.class, 5000);
		
		ServiceReference ref = context.getServiceReference(PlatformManager.class);
		assertNotNull(ref);
		pm = (PlatformManager)context.getService(ref);
		assertNotNull(pm);
	}
	
	public void testNode() throws Exception {
		// initially only one node
		Collection<NodeInfo> nodes = pm.getNodes();
		
		assertEquals(1, nodes.size());
		NodeInfo node = nodes.iterator().next();
		assertEquals(context.getProperty(Constants.FRAMEWORK_UUID),node.getNodeId());
		
		// start new node
		NodeInfo newNode = pm.startNode();
		
		nodes = pm.getNodes();
		assertEquals(2, nodes.size());
		
		// check if all services are available
		ServiceReference[] refs = context.getAllServiceReferences(DeploymentManager.class.getName(), null);
		assertNotNull(refs);
		assertEquals(2, refs.length);
		
		refs = context.getAllServiceReferences(ProxyManager.class.getName(), null);
		assertNotNull(refs);
		assertEquals(2, refs.length);
		
		refs = context.getAllServiceReferences(NodeMonitor.class.getName(), null);
		assertNotNull(refs);
		assertEquals(2, refs.length);
		
		refs = context.getAllServiceReferences(ServiceMonitor.class.getName(), null);
		assertNotNull(refs);
		assertEquals(2, refs.length);
		
		// stop the node
		pm.stopNode(newNode.getNodeId());
		
		Thread.sleep(1000);
		
		nodes = pm.getNodes();
		assertEquals(1, nodes.size());
	}
	
	public void testComponent() throws Exception {
		NodeInfo node1 = pm.startNode();
		
		assertEquals(0, pm.getComponents(node1.getNodeId()).size());
		
		ComponentInfo component = pm.startComponent("org.example.impls.hello", "2.0.0", node1.getNodeId());
		
		assertEquals(2, pm.getComponents(node1.getNodeId()).size());
		
		NodeInfo node2 = pm.startNode();
		
		pm.startComponent("org.example.impls.bye", "1.0.0", node2.getNodeId());
		
		assertEquals(2, pm.getComponents(node2.getNodeId()).size());
		
		for(ComponentInfo c : pm.getComponents(node1.getNodeId())){
			if(!c.getComponentId().equals("org.example.api")){
				assertEquals("2.0.0", c.getVersion());
				assertEquals(node1.getNodeId(), c.getNodeId());
				assertEquals("org.example.impls.hello", c.getComponentId());
			}
		}
		
		for(ComponentInfo c : pm.getComponents(node2.getNodeId())){
			if(!c.getComponentId().equals("org.example.api")){
				assertEquals("1.0.0", c.getVersion());
				assertEquals(node2.getNodeId(), c.getNodeId());
				assertEquals("org.example.impls.bye", c.getComponentId());
			}
		}
		
		int noComponents = pm.getComponents().size();

		pm.stopComponent(component);
		
		assertEquals(noComponents-1, pm.getComponents().size());
	
		// clean up
		pm.stopNode(node1.getNodeId());
		
		Thread.sleep(1000);
		
		pm.stopNode(node2.getNodeId());
	
		Thread.sleep(1000);
		
	}
	
	public void testProxies() throws Exception {
		NodeInfo node1 = pm.startNode();
		
		assertEquals(0, pm.getComponents(node1.getNodeId()).size());
		
		pm.startComponent("org.example.impls.hello", "2.0.0", node1.getNodeId());
		
		assertEquals(2, pm.getComponents(node1.getNodeId()).size());
		
		NodeInfo node2 = pm.startNode();
		
		pm.startComponent("org.example.impls.hello", "2.0.0", node2.getNodeId());
		ComponentInfo component = pm.startComponent("org.example.impls.bye", "1.0.0", node2.getNodeId());
		
		Collection<ProxyInfo> proxies = pm.getProxies(component);
		assertEquals(1, proxies.size());
		
		ProxyInfo proxy = proxies.iterator().next();
		
		Thread.sleep(100);
		
		Collection<ProxyInfo> allProxies = pm.getProxies();

		assertEquals(3, allProxies.size());
		
		pm.stopComponent(component);
		
		Thread.sleep(100);
		
		allProxies = pm.getProxies();
		assertEquals(2, allProxies.size());
		
		// need to fetch service once, as with the lazy service import topologymanager
		// only after the first getServiceReference the service will be imported
		ServiceReference ref = context.getServiceReference("org.example.api.Greeting");
		Thread.sleep(1000);
		
		// now there should be one proxy on this node
		assertEquals(1, pm.getProxies(context.getProperty(Constants.FRAMEWORK_UUID)).size());
		
		// use setPolicy on the new proxy on this node 
		TestProxyPolicy policy = new TestProxyPolicy();
		ProxyInfo p = pm.getProxies(context.getProperty(Constants.FRAMEWORK_UUID)).iterator().next();
		pm.setProxyPolicy(p, policy);
		
		// now perform a method call
		ref = context.getServiceReference("org.example.api.Greeting");
		if(ref!=null){
			Object o = context.getService(ref);
			for(Method m : o.getClass().getMethods()){
				if(m.getName().equals("greet") && m.getParameterTypes()[0].equals(String.class)){
					System.out.println(m.invoke(o, "test"));
				}
			}
		}
		assertTrue(policy.called);
		
		pm.stopNode(node1.getNodeId());
		Thread.sleep(1000);

		pm.stopNode(node2.getNodeId());
		Thread.sleep(1000);
	}
	
    private class TestProxyPolicy implements ProxyPolicy {
		boolean called = false;
		
		@Override
		public ServiceInfo selectTarget(Collection<ServiceInfo> targets,
				String componentId, String serviceId, String method,
				Object[] args) {
			called=true;
			return targets.iterator().next();
		}
		
	}
}
