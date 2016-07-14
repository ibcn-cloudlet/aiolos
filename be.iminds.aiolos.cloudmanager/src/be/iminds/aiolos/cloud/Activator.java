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
package be.iminds.aiolos.cloud;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedServiceFactory;

import be.iminds.aiolos.cloud.api.CloudManager;
import be.iminds.aiolos.util.log.Logger;

/**
 * The {@link BundleActivator} of the CloudManager bundle. 
 */
public class Activator implements BundleActivator {

	private ServiceRegistration<ManagedServiceFactory> cmcService;
	public static Logger logger;

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		logger = new Logger(bundleContext);
		logger.open();
		
		CloudManagerConfigurator cmc = new CloudManagerConfigurator(bundleContext);
		Dictionary<String,Object> properties = new Hashtable<String,Object>();
		properties.put(Constants.SERVICE_PID, cmc.getName());

		cmcService = bundleContext.registerService(ManagedServiceFactory.class, cmc, properties);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (cmcService != null) {
			cmcService.unregister();
			cmcService = null;
		}
		logger.close();
	}
}
