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
package be.iminds.aiolos.rsa;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.service.command.CommandProcessor;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import be.iminds.aiolos.rsa.command.RSACommands;
import be.iminds.aiolos.rsa.serialization.kryo.KryoDeserializer;
import be.iminds.aiolos.rsa.serialization.kryo.KryoFactory;

/**
 * The {@link BundleActivator} for the ProxyManager bundle. 
 */
public class Activator implements BundleActivator {

	ROSGiServiceAdmin rsa = null;

	private ServiceReference<RemoteServiceAdmin> ref = null;
	private ServiceTracker<LogService, LogService> logService;
	
	private ServiceTracker kryoSerializerTracker;
	
	public static Logger logger;
	
	public class Logger {		
		public synchronized void log(int level, String message, Throwable exception){
			LogService log  = logService.getService();
			if(log!=null) {
				log.log(ref, level, message, exception);
			}
		}
		
		public void log(int level, String message){
			log(level, message, null);
		}
	}
	
	@Override
	public void start(final BundleContext context) throws Exception {
		logService = new ServiceTracker<LogService,LogService>(context, LogService.class, null);
		logService.open();
		logger = new Logger();
		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put("remote.configs.supported", new String[]{Config.CONFIG_ROSGI});
		
		rsa = new ROSGiServiceAdmin(context);
		rsa.activate();
			
		ref  = context.registerService(RemoteServiceAdmin.class,rsa, props).getReference();
			
		ROSGiBundleListener listener = new ROSGiBundleListener(rsa);
		context.addBundleListener(listener);
			
		// add Shell commands
		// GoGo Shell
		// add shell commands (try-catch in case no shell available)
		RSACommands commands = new RSACommands(context, rsa);
			
		Dictionary<String, Object> commandProps = new Hashtable<String, Object>();
		try {
			commandProps.put(CommandProcessor.COMMAND_SCOPE, "rsa");
			commandProps.put(CommandProcessor.COMMAND_FUNCTION, new String[] {"endpoints", "importEndpoint", "exportEndpoint", "channels"});
			context.registerService(Object.class, commands, commandProps);
		} catch(Throwable t){
			// ignore exception, in that case no GoGo shell available
		}
		
		// Track kryo serializer services
		// This is very dirty... should be done more nicely ...
		kryoSerializerTracker = new ServiceTracker(context, context.createFilter("(objectClass=com.esotericsoftware.kryo.Serializer)"), new ServiceTrackerCustomizer() {

			@Override
			public Object addingService(ServiceReference reference) {
				Object serializer = context.getService(reference);
				String clazz = (String)reference.getProperty("kryo.serializer.class");
				if(reference.getProperty("kryo.serializer.id")!=null){
					Object i = (String)reference.getProperty("kryo.serializer.id");
					int id = 0;
					if(i instanceof String){
						id = Integer.parseInt((String)i);
					} else if(i instanceof Integer){
						id = (Integer)i;
					}
					KryoFactory.addSerializer(clazz, serializer, id);
				} else {
					KryoFactory.addSerializer(clazz, serializer);
				}
				return serializer;
			}

			@Override
			public void modifiedService(ServiceReference reference,
					Object serializer) {}

			@Override
			public void removedService(ServiceReference reference,
					Object serializer) {
				String clazz = (String)reference.getProperty("kryo.serializer.class");
				KryoFactory.removeSerializer(clazz, serializer);
			}
		});
		kryoSerializerTracker.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		rsa.deactivate();
		logService.close();
		
		kryoSerializerTracker.close();
	}

}
