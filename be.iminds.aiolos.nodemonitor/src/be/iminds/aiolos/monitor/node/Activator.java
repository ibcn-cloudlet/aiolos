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
package be.iminds.aiolos.monitor.node;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

import be.iminds.aiolos.monitor.node.api.NodeMonitor;
import be.iminds.aiolos.util.log.Logger;

public class Activator implements BundleActivator {

	NodeMonitorImpl nodeMonitor = null;
	public static Logger logger; 
	
	@Override
	public void start(BundleContext context) throws Exception {
		logger = new Logger(context);
		logger.open();
		String frameworkId = context.getProperty(Constants.FRAMEWORK_UUID);
		
		int interval = 1;
		
		String i = context.getProperty("aiolos.nodemonitor.interval");
		if(i!=null)
			interval = Integer.parseInt(i);
		
		String netinterface = context.getProperty("aiolos.nodemonitor.netinterface");
		
		nodeMonitor = new NodeMonitorImpl(frameworkId, interval, netinterface);
		nodeMonitor.start();
		
	    Hashtable<String, Object> properties = new Hashtable<String, Object>();
		properties.put("service.exported.interfaces", new String[]{NodeMonitor.class.getName()});
		ServiceRegistration<NodeMonitor> reg = context.registerService(NodeMonitor.class, nodeMonitor, properties);
		logger.setServiceReference(reg.getReference());
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if(nodeMonitor!=null)
			nodeMonitor.stop();
		logger.close();
	}

}
