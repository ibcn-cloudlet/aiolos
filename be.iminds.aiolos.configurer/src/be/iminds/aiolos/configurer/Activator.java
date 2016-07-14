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
package be.iminds.aiolos.configurer;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator {

	private ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> cmTracker;
	
	private BundleTracker<Bundle> bundleTracker;
	
	@Override
	public void start(final BundleContext context) throws Exception {
		cmTracker = new ServiceTracker<ConfigurationAdmin, ConfigurationAdmin>(
				context, ConfigurationAdmin.class, 
				new ServiceTrackerCustomizer<ConfigurationAdmin, ConfigurationAdmin>() {

					@Override
					public ConfigurationAdmin addingService(
							ServiceReference<ConfigurationAdmin> reference) {
						ConfigurationAdmin cm = (ConfigurationAdmin) context.getService(reference);
						if(cm!=null){
							bundleTracker = new BundleTracker<Bundle>(context, Bundle.ACTIVE | Bundle.STARTING | Bundle.STOPPING, new Configurer(cm));
							bundleTracker.open();
						}
						return cm;
					}

					@Override
					public void modifiedService(
							ServiceReference<ConfigurationAdmin> reference,
							ConfigurationAdmin service) {}

					@Override
					public void removedService(
							ServiceReference<ConfigurationAdmin> reference,
							ConfigurationAdmin service) {
						bundleTracker.close();
					}
		});
		cmTracker.open();
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		if(bundleTracker!=null){
			bundleTracker.close();
		}
		cmTracker.close();
	}

}
