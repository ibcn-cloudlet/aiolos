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
package be.iminds.aiolos.discovery;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminListener;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import be.iminds.aiolos.discovery.jslp.jSLPDiscovery;
import be.iminds.aiolos.topology.api.TopologyManager;
import be.iminds.aiolos.util.log.Logger;

public class Activator implements BundleActivator {

	public static Logger logger;

	Discovery discovery = null; 
	
	ServiceTracker<RemoteServiceAdmin, RemoteServiceAdmin> rsaTracker;
	ServiceTracker<TopologyManager, TopologyManager> topologyTracker;
	
	@Override
	public void start(final BundleContext context) throws Exception {
		logger = new Logger(context);
		logger.open();
		
		discovery = new jSLPDiscovery(context);
		
		context.registerService(RemoteServiceAdminListener.class, new RemoteServiceAdminListener() {
			@Override
			public void remoteAdminEvent(RemoteServiceAdminEvent event) {
				switch(event.getType()){
				case RemoteServiceAdminEvent.EXPORT_REGISTRATION:
					if(event.getException()==null){
						EndpointDescription endpoint = event.getExportReference().getExportedEndpoint();
						if(endpoint.getInterfaces().contains(EndpointListener.class.getName())){
							discovery.register(uriFromEndpointId(endpoint.getId()));
						}
					}
					break;
				case RemoteServiceAdminEvent.EXPORT_UNREGISTRATION:
					EndpointDescription endpoint = event.getExportReference().getExportedEndpoint();
					if(endpoint.getInterfaces().contains(EndpointListener.class.getName())){
						discovery.deregister(uriFromEndpointId(endpoint.getId()));
					}
					break;
				}
				
			}
		}, null);
		
		rsaTracker = new ServiceTracker<RemoteServiceAdmin, RemoteServiceAdmin>(
				context, RemoteServiceAdmin.class, new ServiceTrackerCustomizer<RemoteServiceAdmin, RemoteServiceAdmin>() {

					@Override
					public RemoteServiceAdmin addingService(
							ServiceReference<RemoteServiceAdmin> reference) {
						RemoteServiceAdmin rsa = context.getService(reference);
						for(ExportReference ref : rsa.getExportedServices()){
							if(ref.getExportedEndpoint().getInterfaces().contains(EndpointListener.class.getName())){
								discovery.register(uriFromEndpointId(ref.getExportedEndpoint().getId()));
							}
						}
						return rsa;
					}

					@Override
					public void modifiedService(
							ServiceReference<RemoteServiceAdmin> reference,
							RemoteServiceAdmin rsa) {}

					@Override
					public void removedService(
							ServiceReference<RemoteServiceAdmin> reference,
							RemoteServiceAdmin rsa) {
						context.ungetService(reference);
					}
				});
		rsaTracker.open();
		
		topologyTracker = new ServiceTracker<TopologyManager, TopologyManager>(
				context, TopologyManager.class, 
				new ServiceTrackerCustomizer<TopologyManager, TopologyManager>(){

			@Override
			public TopologyManager addingService(
					ServiceReference<TopologyManager> reference) {
				if(reference.getProperty("service.imported")==null){
					TopologyManager topologyManager = context.getService(reference);
					discovery.setTopologyManager(topologyManager);
					return topologyManager;
				}
				return null;
			}

			@Override
			public void modifiedService(
					ServiceReference<TopologyManager> reference,
					TopologyManager topologyManager) {
			}

			@Override
			public void removedService(
					ServiceReference<TopologyManager> reference,
					TopologyManager topologyManager) {
				if(topologyManager!=null){
					discovery.setTopologyManager(null);
				
					context.ungetService(reference);
				}
			}
			
		});
		topologyTracker.open();
		
		discovery.start();
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		rsaTracker.close();
		topologyTracker.close();
		
		discovery.stop();
		logger.close();
	}

	
	private String uriFromEndpointId(String endpointId){
		return "service:node:"+endpointId.substring(0, endpointId.lastIndexOf('#'));
	}
}
