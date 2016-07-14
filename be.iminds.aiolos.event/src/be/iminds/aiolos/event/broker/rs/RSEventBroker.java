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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import be.iminds.aiolos.event.broker.AbstractEventBroker;
import be.iminds.aiolos.event.broker.api.EventBroker;

public class RSEventBroker extends AbstractEventBroker {

	private ServiceTracker<EventBroker, EventBroker> eventBrokerTracker;
	private Map<EventBroker, String[]> brokers = Collections.synchronizedMap(new HashMap<EventBroker, String[]>());
	
	private ExecutorService notificationThread = Executors.newSingleThreadExecutor();
	private Dictionary<String, Object> eventBrokerProperties = new Hashtable<String, Object>();
	private ServiceRegistration reg;
	
	// Buffer to optionally keep events until a remote EventBroker comes available
	private String[] bufferFilters = null;
	private ArrayList<Event> buffer = new ArrayList<Event>(0);
	
	public RSEventBroker(final BundleContext context){
		super(context);
		
		// provide some topics that should be buffered until a remote EventBroker shows up
		Object property = context.getProperty("aiolos.event.rs.buffer");
		if(property!=null){
			bufferFilters = ((String)property).split(",");
		}
		
		eventBrokerTracker = new ServiceTracker<EventBroker, EventBroker>(context, EventBroker.class, 
				new ServiceTrackerCustomizer<EventBroker, EventBroker>() {

			@Override
			public EventBroker addingService(
					ServiceReference<EventBroker> reference) {
				EventBroker broker = context.getService(reference);
				if(broker!=RSEventBroker.this){
					String[] topics = (String[])reference.getProperty("event.topics");
					brokers.put(broker, topics);
					
					if(bufferFilters!=null){
						sendBuffer(broker, topics);
					}
				} 
				return broker;
			}

			@Override
			public void modifiedService(ServiceReference<EventBroker> reference,
					EventBroker broker) {
				if(broker!=RSEventBroker.this){
					brokers.put(broker, (String[])reference.getProperty("event.topics"));
				} 
			}

			@Override
			public void removedService(ServiceReference<EventBroker> reference,
					EventBroker broker) {
				brokers.remove(broker);
			}
		});
		
		eventBrokerProperties.put("service.exported.interfaces",new String[]{EventBroker.class.getName()});
		eventBrokerProperties.put("event.topics", getTopics());
		reg = context.registerService(EventBroker.class, this, eventBrokerProperties);
	}
	
	public void start(){
		super.start();
		eventBrokerTracker.open();
	}
	
	public void stop(){
		super.stop();
		eventBrokerTracker.close();
	}
	
	protected void addTopic(String topic){
		super.addTopic(topic);
		eventBrokerProperties.put("event.topics", getTopics());
		reg.setProperties(eventBrokerProperties);
	}
	
	protected void removeTopic(String topic){
		super.removeTopic(topic);
		eventBrokerProperties.put("event.topics", getTopics());
		reg.setProperties(eventBrokerProperties);
	}
	
	@Override
	public void forwardEvent(Event event) {
		// if event from this runtime, add framework uuid and forward
		if(event.getProperty(Constants.FRAMEWORK_UUID)==null){
			final Map<String, Object> properties = new HashMap<String, Object>();
			for(String key : event.getPropertyNames()){
				properties.put(key, event.getProperty(key));
			}
			properties.put(Constants.FRAMEWORK_UUID, frameworkId);
			final Event e = new Event(event.getTopic(), properties);
			
			// Notify all brokers on a separate thread
			Runnable notification = new Runnable(){
				@Override
				public void run() {
					synchronized(brokers){
						boolean toBuffer = false;
						if(bufferFilters!=null){
							for(String bufferFilter : bufferFilters){
								if(wildCardMatch(e.getTopic(), bufferFilter)){
									toBuffer = true;
									break;
								}
							}
						}
						for(Entry<EventBroker, String[]> b : brokers.entrySet()){
							if(sendEvent(e, b.getKey(), b.getValue())){
								toBuffer = false;
							}
						}
						if(toBuffer){
							synchronized(buffer){
								buffer.add(e);
							}
						}
					}
				}
			};
			notificationThread.execute(notification);
		} 
		// else remote event, publish to EventAdmin
		else {
			EventAdmin ea = eventAdminTracker.getService();
			if(ea!=null){
				ea.postEvent(event);
			}
		}
	}

	private void sendBuffer(final EventBroker broker, final String[] topics){
		Runnable notification = new Runnable(){
			@Override
			public void run() {
				synchronized(buffer){
					Iterator<Event> it = buffer.iterator();
					while(it.hasNext()){
						Event e = it.next();
						if(sendEvent(e, broker, topics)){
							it.remove();
						}
					}
				}
			}
		};
		notificationThread.execute(notification);
	}
	
	
	private boolean sendEvent(Event e, EventBroker b, String[] topics){
		try {
			for(String topic : topics){
				if(wildCardMatch(e.getTopic(), topic)){
					b.forwardEvent(e);
					return true;
				} 
			}
		} catch(Exception ex){
			ex.printStackTrace();
		}
		return false;
	}

	
	private String[] getTopics(){
		String[] t = null;
		synchronized(topics){
			t = new String[topics.size()];
			int i=0;
			for(String topic : topics.keySet()){
				t[i++] = topic;
			}
		}
		return t;
	}
	
	private static boolean wildCardMatch(String text, String pattern) {
	    String [] tokens = pattern.split("\\*");
	    for (String token : tokens) {
	        int idx = text.indexOf(token);
	        if(idx == -1) {
	            return false;
	        }
	        text = text.substring(idx + token.length());
	    }
	    return true;
	}
}
