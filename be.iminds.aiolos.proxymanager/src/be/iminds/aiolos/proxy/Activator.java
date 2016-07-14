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
package be.iminds.aiolos.proxy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.felix.service.command.CommandProcessor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.EventListenerHook;
import org.osgi.framework.hooks.service.FindHook;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import be.iminds.aiolos.proxy.api.ProxyManager;
import be.iminds.aiolos.proxy.api.ServiceProxyListener;
import be.iminds.aiolos.proxy.command.ProxyCommands;
import be.iminds.aiolos.util.log.Logger;

/**
 * The {@link BundleActivator} for the ProxyManager bundle. 
 */
public class Activator implements BundleActivator {

	public static Logger logger;
	
	ServiceTracker<ServiceProxyListener,ServiceProxyListener> serviceProxyListenerTracker;
	
	@Override
	public void start(final BundleContext context) throws Exception {
		logger = new Logger(context); 
		logger.open();
		
		final ProxyManagerImpl proxyManager = new ProxyManagerImpl(context);
	    Hashtable<String, Object> properties = new Hashtable<String, Object>();
			properties.put("service.exported.interfaces", new String[]{ProxyManager.class.getName()});
	    ServiceRegistration<ProxyManager> reg = context.registerService(ProxyManager.class,
	        		proxyManager, properties);
	    logger.setServiceReference(reg.getReference());
	        
		context.registerService(FindHook.class, proxyManager, null);
        context.registerService(EventListenerHook.class, proxyManager, null);
        
        // update all bundles with id greater than this one 
        // with possible services that should be proxied
        // this enables updating bundles at runtime
        for(Bundle b : context.getBundles()){
        	if(b.getBundleId()<=context.getBundle().getBundleId()){
        		// only generate proxies for bundles started after proxymanager?
        		continue;
        	}
        	ServiceReference<?>[] refs = b.getRegisteredServices();
        	if(refs!=null){
	        	for(ServiceReference<?> ref : refs){
	        		List<String> interfaces = new ArrayList<String>(Arrays.asList((String[])ref.getProperty(Constants.OBJECTCLASS)));
	        		proxyManager.filterInterfaces(interfaces);
	        		if(interfaces.size()>0){
	        			b.update();
	        			continue;
	        		}
	        	}
        	}
        }
    
        
        serviceProxyListenerTracker = new ServiceTracker<ServiceProxyListener,ServiceProxyListener>(context, ServiceProxyListener.class.getName(), new ServiceTrackerCustomizer<ServiceProxyListener,ServiceProxyListener>() {
			@Override
			public ServiceProxyListener addingService(ServiceReference<ServiceProxyListener> reference) {
				ServiceProxyListener l = context.getService(reference);
				proxyManager.addServiceProxyListener(l);
				return l;
			}

			@Override
			public void modifiedService(ServiceReference<ServiceProxyListener> reference,
					ServiceProxyListener service) {}

			@Override
			public void removedService(ServiceReference<ServiceProxyListener> reference,
					ServiceProxyListener service) {
				proxyManager.removeServiceProxyListener(service);
				context.ungetService(reference);
			}
		});
        serviceProxyListenerTracker.open();
        
        // GoGo Shell
     	// add shell commands (try-catch in case no shell available)
     	ProxyCommands commands = new ProxyCommands(proxyManager);
     	Dictionary<String, Object> commandProps = new Hashtable<String, Object>();
     	try {
     		commandProps.put(CommandProcessor.COMMAND_SCOPE, "proxy");
     		commandProps.put(CommandProcessor.COMMAND_FUNCTION, new String[] {"list","setpolicy"});
     		context.registerService(Object.class, commands, commandProps);
     	} catch (Throwable t) {
     		// ignore exception, in that case no GoGo shell available
     	}

	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		serviceProxyListenerTracker.close();
		logger.close();
	}

}
