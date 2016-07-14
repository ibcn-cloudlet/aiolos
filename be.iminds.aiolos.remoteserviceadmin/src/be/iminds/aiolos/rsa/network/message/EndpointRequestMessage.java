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
import java.util.ArrayList;
import java.util.List;

import org.osgi.service.remoteserviceadmin.EndpointDescription;

import be.iminds.aiolos.rsa.serialization.api.Deserializer;
import be.iminds.aiolos.rsa.serialization.api.SerializationException;
import be.iminds.aiolos.rsa.serialization.api.Serializer;

/**
 * {@link ROSGiMessage} for requesting a matching {@link EndpointDescription}.
 */
public class EndpointRequestMessage extends ROSGiMessage {

	private String endpointId;
	private List<String> endpointInterfaces;
	
	public EndpointRequestMessage(String endpointId, 
			List<String> endpointInterfaces){
		super(ENDPOINT_REQUEST);
		
		this.endpointId = endpointId;
		this.endpointInterfaces = endpointInterfaces;
	}
	
	
	/**
	 * creates a new InvokeMethodMessage from network packet:
	 *       0                   1                   2                   3
	 *       0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |       R-OSGi header (function = EndpointReq = 15)             |
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |   	endpointID String                                       \
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |   number of interfaces     |      Interface Strings           \
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 * @throws IOException 
	 * 
	 */
	public EndpointRequestMessage(Deserializer input) throws SerializationException, IOException{
		super(ENDPOINT_REQUEST);
		
		endpointId = input.readString();
		int noInterfaces = input.readShort();
		endpointInterfaces = new ArrayList<String>();
		for(int i=0;i<noInterfaces;i++){
			endpointInterfaces.add(input.readString());
		}
	}
	
	@Override
	protected void writeBody(Serializer output) throws SerializationException, IOException {
		output.writeString(endpointId);
		output.writeShort((short) endpointInterfaces.size());
		for(String iface : endpointInterfaces){
			output.writeString(iface);
		}
	}
	
	public String getEndpointId(){
		return endpointId;
	}
	
	public List<String> getInterfaces(){
		return endpointInterfaces;
	}
	
	public String toString() {
		final StringBuffer buffer = new StringBuffer();
		buffer.append("[ENDPOINT REQ] - XID: ");
		buffer.append(xid);
		buffer.append(endpointId);
		return buffer.toString();
	}
}
