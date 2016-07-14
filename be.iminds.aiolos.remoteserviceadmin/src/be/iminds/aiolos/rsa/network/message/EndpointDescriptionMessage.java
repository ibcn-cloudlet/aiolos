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
package be.iminds.aiolos.rsa.network.message;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.service.remoteserviceadmin.EndpointDescription;

import be.iminds.aiolos.rsa.serialization.api.Deserializer;
import be.iminds.aiolos.rsa.serialization.api.SerializationException;
import be.iminds.aiolos.rsa.serialization.api.Serializer;

/**
 * {@link ROSGiMessage} for exchanging an {@link EndpointDescription}.
 */
public class EndpointDescriptionMessage extends ROSGiMessage {

	private EndpointDescription endpointDescription;

	public EndpointDescriptionMessage(EndpointDescription endpointDescription){
		super(ENDPOINT_DESCRIPTION);
		
		this.endpointDescription = endpointDescription;
	}
	
	/**
	 * creates a new EndpointDescription message from network packet:
	 * when invalid endpoint requested, endpointId is "null"
	 *      
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |       R-OSGi header (function = EndpointDesc = 16)            |
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |   	endpointID   (String)                                   \
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |   	service ID   (Long)                                     \
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |   	framework UUID   (String)                               \
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |  number of interfaces   |  interface Strings                  \
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |  number of config types |  config type Strings                \
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |  number of intents      |  intent Strings                     \
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |  number of other properties  |  key String | value        ... \
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      
	 * 
	 */
	public EndpointDescriptionMessage(Deserializer input) throws SerializationException, IOException {
		super(ENDPOINT_DESCRIPTION);
		
		Map<String, Object> endpointProperties = new HashMap<String, Object>();
		// endpoint id
		String endpointId = input.readString();
		
		if(endpointId.equals("null")){
			endpointDescription = null;
			return;
		}
			
		endpointProperties.put("endpoint.id", endpointId);
		// service id
		long serviceId = input.readLong();
		endpointProperties.put("endpoint.service.id", serviceId);
		// framework uuid
		String frameworkUuid = input.readString();
		endpointProperties.put("endpoint.framework.uuid", frameworkUuid);
		// objectClass interfaces
		short noInterfaces = input.readShort();
		String[] interfaces = new String[noInterfaces];
		for(int i=0;i<noInterfaces;i++){
			interfaces[i] = input.readString();
		}
		endpointProperties.put("objectClass", interfaces);
		// configs imported
		short noConfigs = input.readShort();
		String[] configs = new String[noConfigs];
		for(int i=0;i<noConfigs;i++){
			configs[i] = input.readString();
		}
		endpointProperties.put("service.imported.configs", configs);
		// intents
		short noIntents = input.readShort();
		String[] intents = new String[noIntents];
		for(int i=0;i<noIntents;i++){
			intents[i] = input.readString();
		}
		endpointProperties.put("service.intents", intents);
		// other properties
		short noProperties = input.readShort();
		
		for(int i=0;i<noProperties;i++){
			String key = input.readString();
			Object value = input.readObject();
			endpointProperties.put(key, value);
		}
		
		endpointDescription = new EndpointDescription(endpointProperties);
	}
	
	@Override
	protected void writeBody(Serializer output) throws SerializationException, IOException {
		if(endpointDescription==null){
			output.writeString("null");
			return;
		}
		
		// endpoint id
		output.writeString(endpointDescription.getId());
		// service id
		output.writeLong(endpointDescription.getServiceId());
		// framework uuid
		output.writeString(endpointDescription.getFrameworkUUID());
		// objectClass interfaces
		output.writeShort((short) endpointDescription.getInterfaces().size());
		for(String iface : endpointDescription.getInterfaces()){
			output.writeString(iface);
		}
		// configs imported
		output.writeShort((short) endpointDescription.getConfigurationTypes().size());
		for(String config : endpointDescription.getConfigurationTypes()){
			output.writeString(config);
		}
		// intents
		output.writeShort((short) endpointDescription.getIntents().size());
		for(String intent : endpointDescription.getIntents()){
			output.writeString(intent);
		}
		// other properties
		// TODO for now only String values are allowd ... should be extended..
		Map<String, Object> properties = new HashMap<String, Object>();
		for(String key : endpointDescription.getProperties().keySet()){
			if(!filteredKeys.contains(key)){
				properties.put(key, endpointDescription.getProperties().get(key));
			}
		}
		output.writeShort((short) properties.size());
		Iterator<Entry<String, Object>> it = properties.entrySet().iterator();
		while(it.hasNext()){
			Entry<String, Object> entry = it.next();
			output.writeString(entry.getKey());
			output.writeObject(entry.getValue());
		}
	}
	
	private List<String> filteredKeys = Arrays.asList(new String[]{
		"endpoint.id",
		"endpoint.service.id",
		"endpoint.framework.uuid",
		"objectClass",
		"service.imported.configs",
		"service.intents"
	});

	
	public EndpointDescription getEndpointDescription(){
		return endpointDescription;
	}
	
	public String toString() {
		final StringBuffer buffer = new StringBuffer();
		buffer.append("[ENDPOINT DESCR] - XID: ");
		buffer.append(xid);
		buffer.append(endpointDescription.getId());
		return buffer.toString();
	}
}
