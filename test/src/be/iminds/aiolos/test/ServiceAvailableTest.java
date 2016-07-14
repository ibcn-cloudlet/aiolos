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
import static org.knowhowlab.osgi.testing.assertions.BundleAssert.assertBundleState;
import static org.knowhowlab.osgi.testing.assertions.ServiceAssert.assertServiceAvailable;

import java.io.File;

import junit.framework.TestCase;

import org.osgi.framework.Bundle;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;
import org.osgi.service.repository.Repository;

import be.iminds.aiolos.cloud.api.CloudManager;
import be.iminds.aiolos.deployment.api.DeploymentManager;
import be.iminds.aiolos.monitor.node.api.NodeMonitor;
import be.iminds.aiolos.monitor.service.api.ServiceMonitor;
import be.iminds.aiolos.platform.api.PlatformManager;
import be.iminds.aiolos.proxy.api.ProxyManager;

public class ServiceAvailableTest extends TestCase {

    public void testRemoteServiceAdmin() throws Exception {
    	assertBundleAvailable("be.iminds.aiolos.remoteserviceadmin");
    	assertBundleState(Bundle.ACTIVE, "be.iminds.aiolos.remoteserviceadmin");
    	assertServiceAvailable(RemoteServiceAdmin.class);
    }
    
    public void testProxyManager() throws Exception {
    	assertBundleAvailable("be.iminds.aiolos.proxymanager");
    	assertBundleState(Bundle.ACTIVE, "be.iminds.aiolos.proxymanager");
    	assertServiceAvailable(ProxyManager.class);
    }

    public void testDeploymentManager() throws Exception {
    	assertBundleAvailable("be.iminds.aiolos.deploymentmanager");
    	assertBundleState(Bundle.ACTIVE, "be.iminds.aiolos.deploymentmanager");
    	assertServiceAvailable(DeploymentManager.class);
    }

    public void testComponentMonitor() throws Exception {
    	assertBundleAvailable("be.iminds.aiolos.servicemonitor");
    	assertBundleState(Bundle.ACTIVE, "be.iminds.aiolos.servicemonitor");
    	assertServiceAvailable(ServiceMonitor.class);
    }
    
    public void testNodeMonitor() throws Exception {
    	assertBundleAvailable("be.iminds.aiolos.nodemonitor");
    	assertBundleState(Bundle.ACTIVE, "be.iminds.aiolos.nodemonitor");
    	assertServiceAvailable(NodeMonitor.class);
    }
    
    public void testPlatformManager() throws Exception {
    	assertBundleAvailable("be.iminds.aiolos.platformmanager");
    	assertBundleState(Bundle.ACTIVE, "be.iminds.aiolos.platformmanager");
    	assertServiceAvailable(PlatformManager.class);
    }
    
    public void testRepository() throws Exception {
    	assertBundleAvailable("be.iminds.aiolos.repository");
    	assertBundleState(Bundle.ACTIVE, "be.iminds.aiolos.repository");
    	assertServiceAvailable(Repository.class, 5000);
    }
    
    public void testCloudManager() throws Exception {
    	assertBundleAvailable("be.iminds.aiolos.cloudmanager");
    	assertBundleState(Bundle.ACTIVE, "be.iminds.aiolos.cloudmanager");
    	assertServiceAvailable(CloudManager.class, 5000);
    }
    
    public void testUI() throws Exception {
    	assertBundleAvailable("be.iminds.aiolos.userinterface");
    	assertBundleState(Bundle.ACTIVE, "be.iminds.aiolos.userinterface");
    }
}
