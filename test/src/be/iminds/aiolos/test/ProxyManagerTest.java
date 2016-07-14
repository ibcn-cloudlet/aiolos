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

import java.util.Dictionary;
import java.util.EventListener;
import java.util.Hashtable;
import java.util.UUID;

import junit.framework.TestCase;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.osgi.service.repository.Repository;

import be.iminds.aiolos.deployment.api.DeploymentManager;
import be.iminds.aiolos.info.ComponentInfo;
import be.iminds.aiolos.info.ServiceInfo;
import be.iminds.aiolos.proxy.api.ProxyInfo;
import be.iminds.aiolos.proxy.api.ProxyManager;

public class ProxyManagerTest  extends TestCase {

	private final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();  
	
	private DeploymentManager dm;
	private ProxyManager pm;

	public void setUp(){
		assertServiceAvailable(Repository.class, 5000);
		
		ServiceReference ref1 = context.getServiceReference(DeploymentManager.class);
		assertNotNull(ref1);
		dm = (DeploymentManager)context.getService(ref1);
		assertNotNull(dm);
		
		ServiceReference ref2 = context.getServiceReference(ProxyManager.class);
		assertNotNull(ref2);
		pm = (ProxyManager)context.getService(ref2);
		assertNotNull(pm);
	}
	
	public void testProxyComponent() throws Exception {
		
		ComponentInfo component = dm.startComponent("org.example.impls.hello");
	
		assertTrue(1 <= pm.getProxies().size());
		assertEquals(1, pm.getProxies(component).size());
		
		ProxyInfo proxy = pm.getProxies(component).iterator().next();
		
		assertEquals("org.example.api.Greeting", proxy.getServiceId());
		assertEquals(component.getComponentId(), proxy.getComponentId());
		assertEquals(component.getNodeId(), proxy.getNodeId());
		assertEquals(component.getVersion(), proxy.getVersion());
		
		assertEquals(1, pm.getServices(component).size());
		ServiceInfo service = pm.getServices(component).iterator().next();
		
		assertEquals(1, proxy.getInstances().size());
		ServiceInfo instance = proxy.getInstances().iterator().next();
		assertEquals(service, instance);
		
		ServiceReference[] refs = context.getAllServiceReferences("org.example.api.Greeting", null);
		assertEquals(1, refs.length);
	}
	
	public void testCallbackServiceProxy() throws Exception {
		
		int before = pm.getProxies().size();
		
		EventListener service = new EventListener() {
		};
		Hashtable<String, Object> properties = new Hashtable<String, Object>();
		properties.put("aiolos.callback", EventListener.class.getName());
		ServiceRegistration registration = context.registerService(EventListener.class.getName(), service, properties);
		
		int after = pm.getProxies().size();
		assertEquals(before+1, after);
		
		ServiceReference[] refs = context.getAllServiceReferences(EventListener.class.getName(), null);
		assertEquals(1, refs.length);
		
		registration.unregister();
		assertEquals(before, pm.getProxies().size());
		
	}
	
}
