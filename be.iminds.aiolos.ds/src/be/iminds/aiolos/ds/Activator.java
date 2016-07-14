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
package be.iminds.aiolos.ds;

import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;

import be.iminds.aiolos.ds.description.ComponentDescription;
import be.iminds.aiolos.ds.util.ComponentDescriptionParser;

public class Activator implements BundleActivator {

	private ComponentManager manager;
	private ComponentBundleListener listener;
	
	@Override
	public void start(BundleContext context) throws Exception {
		manager = new ComponentManager();
		listener = new ComponentBundleListener();
		
		for(Bundle b : context.getBundles()){
			if(b.getBundleContext()!=context){
				if(b.getState()==Bundle.ACTIVE){
					// do a started event for all active bundles
					BundleEvent started = new BundleEvent(BundleEvent.STARTED, b);
					listener.bundleChanged(started);
				}
			}
		}
		
		context.addBundleListener(listener);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		
		context.removeBundleListener(listener);
		
		// TODO better just unregister all components instead of iterating over bundles
		for(Bundle b : context.getBundles()){
			manager.unregisterComponents(b);
		}
	}

	
	private class ComponentBundleListener implements SynchronousBundleListener {

		@Override
		public void bundleChanged(BundleEvent event) {
			Bundle bundle = event.getBundle();
			switch(event.getType()){
			case BundleEvent.STARTED:
				try {
					List<ComponentDescription> descriptions = ComponentDescriptionParser.loadComponentDescriptors(bundle);
					for(ComponentDescription description : descriptions){
						manager.registerComponent(bundle, description);
					}
				} catch(Exception e){
					e.printStackTrace();
				}
				break;
			case BundleEvent.STOPPING:
				manager.unregisterComponents(bundle);
				break;
			}
		}
		
	}
}
