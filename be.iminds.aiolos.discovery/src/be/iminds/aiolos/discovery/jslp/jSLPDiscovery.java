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
package be.iminds.aiolos.discovery.jslp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

import be.iminds.aiolos.discovery.Activator;
import be.iminds.aiolos.discovery.Discovery;
import ch.ethz.iks.slp.Advertiser;
import ch.ethz.iks.slp.Locator;
import ch.ethz.iks.slp.ServiceLocationEnumeration;
import ch.ethz.iks.slp.ServiceType;
import ch.ethz.iks.slp.ServiceURL;

public class jSLPDiscovery extends Discovery {

	private BundleContext context;
	
	private Map<String, Timer> advertiseTimers = new HashMap<String, Timer>();
	
	public jSLPDiscovery(BundleContext context){
		this.context = context;
	}
	
	@Override
	public void registerURI(final String uri) {
		if(uri.contains("[")){
			Activator.logger.log(LogService.LOG_WARNING, "jSLP does not support IPv6 URI: "+uri);
			return;
		}
		
		if(advertiseTimers.containsKey(uri))
			return;
		
		Timer timer = new Timer();
		advertiseTimers.put(uri, timer);
		
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				ServiceReference advRef = context.getServiceReference("ch.ethz.iks.slp.Advertiser");

				if (advRef != null) {
					try {
						Advertiser advertiser = (Advertiser) context.getService(advRef);
						advertiser.register(new ServiceURL(uri, INTERVAL), null);
					} catch(Exception e){
						e.printStackTrace();
					} finally {
						context.ungetService(advRef);
					}
				}
				
			}
		}, 0, 60000);
	}

	@Override
	public void deregisterURI(final String uri){
		Timer timer = advertiseTimers.get(uri);
		if(timer==null)
			return;
		
		timer.cancel();
		
		ServiceReference advRef = context.getServiceReference("ch.ethz.iks.slp.Advertiser");

		if (advRef != null) {
			try {
				Advertiser advertiser = (Advertiser) context.getService(advRef);
				advertiser.deregister(new ServiceURL(uri, 0));
			} catch(Exception e){
				e.printStackTrace();
			} finally {
				context.ungetService(advRef);
			}
		}
	}

	@Override
	protected Set<String> discoverURIs() {
		Set<String> result = new HashSet<String>();
		
		ServiceReference locRef = context.getServiceReference("ch.ethz.iks.slp.Locator");
		
		if (locRef != null) {
			try {
				Locator locator = (Locator) context.getService(locRef);
	
				ServiceLocationEnumeration slenum = locator.findServices(
						new ServiceType("service:node"), null, null);
				while (slenum.hasMoreElements()) {
					ServiceURL url = (ServiceURL) slenum.nextElement();
					result.add(url.toString());
				}
			} catch(Exception e){
				e.printStackTrace();
			} finally {
				context.ungetService(locRef);
			}
		}
		return result;
	}

}
