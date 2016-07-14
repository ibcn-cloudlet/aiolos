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
package be.iminds.aiolos.event.broker;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import be.iminds.aiolos.event.broker.api.EventBroker;

public abstract class AbstractEventBroker implements EventBroker, EventHandler {

	protected final BundleContext context;
	protected final String frameworkId;
	
	// service trackers
	protected ServiceTracker<EventAdmin,EventAdmin> eventAdminTracker;
	protected ServiceTracker<EventHandler,EventHandler> eventHandlerTracker;
	// we have EventHandlers for these topics locally
	protected Map<String, AtomicInteger> topics = Collections.synchronizedMap(new HashMap<String, AtomicInteger>()); 
	
	public AbstractEventBroker(final BundleContext context){
		this.context = context;
		this.frameworkId = context.getProperty(Constants.FRAMEWORK_UUID);
		
		eventHandlerTracker = new ServiceTracker<EventHandler, EventHandler>(context, EventHandler.class, 
				new ServiceTrackerCustomizer<EventHandler, EventHandler>() {

			@Override
			public EventHandler addingService(
					ServiceReference<EventHandler> reference) {
				EventHandler handler = context.getService(reference);
				if(!(handler instanceof EventBroker)){  // ignore eventbrokers that subscribe to all
					Object t = reference.getProperty(EventConstants.EVENT_TOPIC);
					if(t instanceof String){
						addTopic((String)t);
					} else {
						for(String s : (String[])t){
							addTopic(s);
						}
					}
				}
				return handler;
			}

			@Override
			public void modifiedService(ServiceReference<EventHandler> reference,
					EventHandler handler) {}

			@Override
			public void removedService(ServiceReference<EventHandler> reference,
					EventHandler handler) {
				if(!(handler instanceof EventBroker)){
					Object t = reference.getProperty(EventConstants.EVENT_TOPIC);
					if(t instanceof String){
						removeTopic((String)t);
					} else {
						for(String s : (String[])t){
							removeTopic(s);
						}
					}
				}
			}
		});
		
		eventAdminTracker = new ServiceTracker<EventAdmin, EventAdmin>(context, EventAdmin.class, null);
	}
	
	public void start(){
		eventAdminTracker.open();
		eventHandlerTracker.open();
	}
	
	public void stop(){
		eventAdminTracker.close();
		eventHandlerTracker.close();
	}
	
	protected void addTopic(String topic){
		synchronized(topics){
			AtomicInteger i = topics.get(topic);
			if(i==null){
				i = new AtomicInteger(0);
				topics.put(topic, i);
			}
			i.incrementAndGet();
		}
	}
	
	protected void removeTopic(String topic){
		synchronized(topics){
			AtomicInteger i = topics.get(topic);
			if(i!=null){
				if(i.decrementAndGet()==0){
					topics.remove(topic);
				}
			}
		}
	}

	@Override
	public void handleEvent(Event event) {
		// TODO forward all except xxx vs forward only xxx strategy?
		
		// ignore all osgi namespace events (these often contain unserializable stuff like sevicereferences)
		if(event.getTopic().startsWith("org/osgi")){
			return;
		}
		
		// distribute to other event handlers
		if(event.getProperty(Constants.FRAMEWORK_UUID)==null){
			forwardEvent(event);
		}
		
	}

}
