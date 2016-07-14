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

import be.iminds.aiolos.rsa.serialization.api.Deserializer;
import be.iminds.aiolos.rsa.serialization.api.SerializationException;
import be.iminds.aiolos.rsa.serialization.api.Serializer;

/**
 * This class is based on RemoteOSGiMessage from R-OSGi project
 * Only REMOTE_CALL and REMOTE_CALL_RESULT messages are implemented
 * 
 * ENDPOINT_REQUEST and ENDPOINT_DESCRIPTION are added to check
 * whether a valid interface is provided at the import description
 * and to fetch all endpoint properties set at the server side
 */
public abstract class ROSGiMessage {
	
	public static final short REMOTE_CALL = 5;
	public static final short REMOTE_CALL_RESULT = 6;
	
	public static final short ENDPOINT_REQUEST = 15;
	public static final short ENDPOINT_DESCRIPTION = 16;
	public static final short INTERRUPT = 17;
	
	private short funcID;
	protected int xid;

	ROSGiMessage(final short funcID) {
		this.funcID = funcID;
	}

	public final int getXID() {
		return xid;
	}

	public void setXID(final int xid) {
		this.xid = xid;
	}

	public final short getFuncID() {
		return funcID;
	}

	/**
	 * reads in a network packet and constructs the corresponding subtype of
	 * R-OSGiMessage from it. The header is:
	 *   0                   1                   2                   3
	 *   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *  |    Version    |         Function-ID           |     XID       |
	 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *  |    XID cntd.  | 
	 *  +-+-+-+-+-+-+-+-+
	 *  
	 *  The body is added by the message subclasses
	 */
	public static ROSGiMessage parse(final Deserializer input)
			throws SerializationException, IOException {
		input.readByte(); // version, currently unused
		final short funcID = input.readByte();
		final int xid = input.readInt();

		ROSGiMessage msg = null;
		switch (funcID) {
		case REMOTE_CALL:
			msg = new RemoteCallMessage(input);
			break;
		case REMOTE_CALL_RESULT:
			msg = new RemoteCallResultMessage(input);
			break;
		case ENDPOINT_REQUEST:
			msg = new EndpointRequestMessage(input);
			break;
		case ENDPOINT_DESCRIPTION:
			msg = new EndpointDescriptionMessage(input);
			break;
		case INTERRUPT:
			msg = new InterruptMessage(input);
			break;
		default:
			// unsupported funcID
			return null;
		}
		msg.funcID = funcID;
		msg.xid = xid;
		return msg;
	}

	public final void send(final Serializer out) throws SerializationException, IOException {
		synchronized (out) {
			out.writeByte((byte)1);
			out.writeByte((byte)funcID);
			out.writeInt(xid);
			writeBody(out);
			out.flush();
		}
	}

	protected abstract void writeBody(final Serializer output)
			throws SerializationException, IOException;

}
