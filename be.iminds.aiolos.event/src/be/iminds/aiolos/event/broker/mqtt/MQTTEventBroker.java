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
package be.iminds.aiolos.event.broker.mqtt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import be.iminds.aiolos.event.broker.AbstractEventBroker;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.JavaSerializer;

public class MQTTEventBroker extends AbstractEventBroker implements MqttCallback {

	private MqttClient mqtt;
	private Kryo kryo;
	
	public MQTTEventBroker(BundleContext context) throws Exception {
		super(context);
		
		String server = context.getProperty("aiolos.event.mqtt.server");
		if(server==null){
			throw new Exception("No MQTT server specified");
		}
		try {
			mqtt = new MqttClient(server, frameworkId);
		} catch(MqttException e){
			e.printStackTrace();
			throw new Exception("Failed to connect to MQTT Server "+server, e);
		}
		
		kryo = new Kryo();
		// we call reset ourselves after each readObject
		kryo.setAutoReset(false);
		// redirect to this bundle classloader
		kryo.setClassLoader(MQTTEventBroker.class.getClassLoader());
		// Sometimes problems with serializing exceptions in Kryo (e.g. Throwable discrepance between android/jdk)
		kryo.addDefaultSerializer(Throwable.class, JavaSerializer.class);
		// required to instantiate classes without no-arg constructor
		kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
	}

	public void start(){
		super.start();
		
		try {
			mqtt.connect();
			mqtt.setCallback(this);
		} catch (MqttSecurityException e) {
			e.printStackTrace();
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}
	
	public void stop(){
		super.stop();
		
		try {
			mqtt.disconnect();
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}
	
	public void addTopic(String topic){
		super.addTopic(topic);
		try {
			mqtt.subscribe("aiolos/"+topic.replaceAll("\\*", "#"));
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}
	
	public void removeTopic(String topic){
		super.removeTopic(topic);
		try {
			mqtt.unsubscribe("aiolos/"+topic.replaceAll("\\*", "#"));
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void forwardEvent(Event event) {
		Output out = null;
		try {
			Map<String, Object> properties = new HashMap<String, Object>();
			
			for(String key : event.getPropertyNames()){
				properties.put(key, event.getProperty(key));
			}
			properties.put(Constants.FRAMEWORK_UUID, frameworkId);
		
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			out = new Output(baos);
			kryo.writeClassAndObject(out, properties);
			out.flush();
			
			MqttMessage message = new MqttMessage(baos.toByteArray());
			mqtt.publish("aiolos/"+event.getTopic(), message);
			
		} catch(Exception e){
			e.printStackTrace();
		} finally {
			kryo.reset();
			out.close();
		}
	}

	@Override
	public void connectionLost(Throwable ex) {
		// TODO handle this
		ex.printStackTrace();
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {	
		// TODO handle this
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		EventAdmin ea = eventAdminTracker.getService();
		if(ea!=null){
			//  remove first aiolos/
			String osgiTopic = topic.substring(7);
			
			// parse osgi event
			Input in = null;
			try {
				in = new Input(new ByteArrayInputStream(message.getPayload()));
				Map<String, Object> properties = (Map<String, Object>) kryo.readClassAndObject(in);
	
				if(!properties.get(Constants.FRAMEWORK_UUID).equals(frameworkId)){
					Event event = new Event(osgiTopic, properties);
					ea.postEvent(event);
				}
			
			} catch(Exception e){
				e.printStackTrace();
			} finally{
				kryo.reset();
				in.close();
			}
		}
	}
}
