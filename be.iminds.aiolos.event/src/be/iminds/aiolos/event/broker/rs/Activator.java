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
package be.iminds.aiolos.event.broker.rs;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import be.iminds.aiolos.event.broker.AbstractEventBroker;
import be.iminds.aiolos.event.broker.rs.RSEventBroker;
import be.iminds.aiolos.event.serializer.EventSerializer;

import com.esotericsoftware.kryo.Serializer;


public class Activator implements BundleActivator {

	private AbstractEventBroker broker;
	
	@Override
	public void start(final BundleContext context) throws Exception {
		Dictionary<String, Object> serializerProperties = new Hashtable<String, Object>();
		serializerProperties.put("kryo.serializer.class", Event.class.getName());
		context.registerService(Serializer.class, new EventSerializer(), serializerProperties);
		
		broker = new RSEventBroker(context);
		broker.start();
		Dictionary<String, Object> eventHandlerProperties = new Hashtable<String, Object>();
		eventHandlerProperties.put(EventConstants.EVENT_TOPIC,"*");
		context.registerService(EventHandler.class, broker, eventHandlerProperties);
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		broker.stop();
	}

}
