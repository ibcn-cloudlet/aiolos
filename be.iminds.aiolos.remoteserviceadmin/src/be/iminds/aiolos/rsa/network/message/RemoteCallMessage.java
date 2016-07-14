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

import be.iminds.aiolos.rsa.serialization.api.Deserializer;
import be.iminds.aiolos.rsa.serialization.api.SerializationException;
import be.iminds.aiolos.rsa.serialization.api.Serializer;

/**
 * {@link ROSGiMessage} for executing a remote call.
 */
public final class RemoteCallMessage extends ROSGiMessage {

	private String serviceId;
	private String methodSignature;
	private Object[] arguments;

	public RemoteCallMessage(String serviceId, String methodSignature, Object[] args) {
		super(REMOTE_CALL);
		
		this.serviceId = serviceId;
		this.methodSignature = methodSignature;
		this.arguments = args;
	}
	
	/**
	 * creates a new InvokeMethodMessage from network packet:
	 *       0                   1                   2                   3
	 *       0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |       R-OSGi header (function = InvokeMsg = 5)                |
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |   	serviceId String                                        \
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |       MethodSignature String                                  \
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |   number of param blocks      |     Param blocks (if any)     \
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 * 
	 */
	RemoteCallMessage(final Deserializer input) throws SerializationException, IOException {
		super(REMOTE_CALL);

		serviceId = input.readString();
		methodSignature = input.readString();
		final short argLength = input.readShort();
		arguments = new Object[argLength];
		for (short i = 0; i < argLength; i++) {
			arguments[i] = input.readObject();
		}
	}

	public void writeBody(final Serializer out) throws SerializationException, IOException {
		out.writeString(serviceId);
		out.writeString(methodSignature);
		if(arguments!=null){
			out.writeShort((short) arguments.length);
			for (short i = 0; i < arguments.length; i++) {
				out.writeObject(arguments[i]);
			}
		} else {
			out.writeShort((short) 0);
		}
	}

	public String getServiceId() {
		return serviceId;
	}

	public Object[] getArgs() {
		return arguments;
	}

	public String getMethodSignature() {
		return methodSignature;
	}

	public String toString() {
		final StringBuffer buffer = new StringBuffer();
		buffer.append("[REMOTE_CALL] - XID: ");
		buffer.append(xid);
		buffer.append(", serviceID: ");
		buffer.append(serviceId);
		buffer.append(", methodName: ");
		buffer.append(methodSignature);
		buffer.append(", params: ");
		buffer.append(arguments == null ? "" : Arrays.asList(arguments)
				.toString());
		return buffer.toString();
	}
}
