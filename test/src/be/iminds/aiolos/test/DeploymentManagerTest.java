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

import static org.knowhowlab.osgi.testing.assertions.BundleAssert.assertBundleAvailable;
import static org.knowhowlab.osgi.testing.assertions.ServiceAssert.assertServiceAvailable;
import junit.framework.TestCase;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.repository.Repository;

import be.iminds.aiolos.deployment.api.DeploymentManager;
import be.iminds.aiolos.info.ComponentInfo;

public class DeploymentManagerTest extends TestCase {

	private final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();  
	
	private DeploymentManager dm;
	
	public void setUp(){
		assertServiceAvailable(Repository.class, 5000);
		
		ServiceReference ref = context.getServiceReference(DeploymentManager.class);
		assertNotNull(ref);
		dm = (DeploymentManager)context.getService(ref);
		assertNotNull(dm);
	}

	
	public void testStartStopComponent() throws Exception {
		
		ComponentInfo component = dm.startComponent("org.example.impls.hello");
		
		assertEquals("org.example.impls.hello", component.getComponentId());
		assertEquals(context.getProperty(Constants.FRAMEWORK_UUID), component.getNodeId());
		
		assertBundleAvailable("org.example.api");
		assertBundleAvailable("org.example.impls.hello");
		
		Bundle hello = null;
		
		for(Bundle b : context.getBundles()){
			if(b.getSymbolicName().equals("org.example.impls.hello"))
				hello = b;
		}
		
		dm.stopComponent(component);

		assertEquals(Bundle.UNINSTALLED, hello.getState());
    }
	
	
	public void testStartStopComponentVersion() throws Exception {
		
		ComponentInfo component = dm.startComponent("org.example.impls.hello", "2.0.0");
		
		assertEquals("org.example.impls.hello", component.getComponentId());
		assertEquals("2.0.0", component.getVersion());
		assertEquals(context.getProperty(Constants.FRAMEWORK_UUID), component.getNodeId());
		
		assertBundleAvailable("org.example.api");
		assertBundleAvailable("org.example.impls.hello");
		
		Bundle hello = null;
		
		for(Bundle b : context.getBundles()){
			if(b.getSymbolicName().equals("org.example.impls.hello"))
				hello = b;
		}
		
		dm.stopComponent(component);

		assertEquals(Bundle.UNINSTALLED, hello.getState());
    }
}
