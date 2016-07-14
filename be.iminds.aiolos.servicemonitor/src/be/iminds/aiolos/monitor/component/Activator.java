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
package be.iminds.aiolos.monitor.component;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import be.iminds.aiolos.monitor.service.api.ServiceMonitor;
import be.iminds.aiolos.proxy.api.ServiceProxyListener;
import be.iminds.aiolos.util.log.Logger;

/**
 * The {@link BundleActivator} of the BundleMonitor bundle. 
 */
public class Activator implements BundleActivator {

	public static Logger logger;
	
	@Override
	public void start(final BundleContext context) throws Exception {
		logger = new Logger(context);
		
		// global property to avoid calculating size for performance
		boolean monitorSize = true;
		String size = (String)context.getProperty("aiolos.monitor.service.size.ignore");
		if(size!=null){
			monitorSize = false;
		}
		
		ServiceMonitorImpl bundleMonitor = new ServiceMonitorImpl(monitorSize);
		
        Hashtable<String, Object> properties = new Hashtable<String, Object>();
		properties.put("service.exported.interfaces", new String[]{ServiceMonitor.class.getName()});
		ServiceRegistration<ServiceMonitor> reg = context.registerService(ServiceMonitor.class,
        		bundleMonitor, properties);
		logger.setServiceReference(reg.getReference());
		
        context.registerService(ServiceProxyListener.class,
        		bundleMonitor, null);
        
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		logger.close();
	}

}
