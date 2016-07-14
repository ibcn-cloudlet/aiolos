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
 * {@link ROSGiMessage} capturing the result of a remote call.
 */
public final class RemoteCallResultMessage extends ROSGiMessage {

	private byte errorFlag;
	private Object result;
	private Throwable exception;

	public RemoteCallResultMessage(final Object result) {
		super(REMOTE_CALL_RESULT);
		
		this.result = result;
		errorFlag = 0;
	}

	public RemoteCallResultMessage(final Throwable t) {
		super(REMOTE_CALL_RESULT);
		
		this.exception = t;
		this.errorFlag = 1;
	}
	
	/**
	 * creates a new MethodResultMessage from network packet:
	 * 
	 *       0                   1                   2                   3
	 *       0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |       R-OSGi header (function = Result = 6)                   |
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |  error flag   | result or Exception                           \
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 */
	RemoteCallResultMessage(final Deserializer input) throws IOException,
			SerializationException {
		super(REMOTE_CALL_RESULT);
		errorFlag = input.readByte();
		if (errorFlag == 0) {
			result = input.readObject();
			exception = null;
		} else {
			exception = (Throwable) input.readObject();
			result = null;
		}
	}

	public void writeBody(final Serializer out) throws SerializationException, IOException {
		if (exception == null) {
			out.writeByte((byte)0);
			out.writeObject(result);
		} else {
			out.writeByte((byte)1);
			out.writeObject(exception);
		}
	}

	public boolean causedException() {
		return (errorFlag == 1);
	}

	public Object getResult() {
		return result;
	}

	public Throwable getException() {
		return exception;
	}

	public String toString() {
		final StringBuffer buffer = new StringBuffer();
		buffer.append("[REMOTE_CALL_RESULT] - XID: "); //$NON-NLS-1$
		buffer.append(xid);
		buffer.append(", errorFlag: "); //$NON-NLS-1$
		buffer.append(errorFlag);
		if (causedException()) {
			buffer.append(", exception: "); //$NON-NLS-1$
			buffer.append(exception.getMessage());
		} else {
			buffer.append(", result: "); //$NON-NLS-1$
			buffer.append(result);
		}
		return buffer.toString();
	}
}
