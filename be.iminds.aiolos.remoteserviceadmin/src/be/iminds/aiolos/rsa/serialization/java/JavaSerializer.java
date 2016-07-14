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
package be.iminds.aiolos.rsa.serialization.java;

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import be.iminds.aiolos.rsa.serialization.api.SerializationException;
import be.iminds.aiolos.rsa.serialization.api.Serializer;

/**
 * {@link Serializer} using the default Java Serialization mechanism.
 */
public class JavaSerializer implements Serializer {

	private final ObjectOutputStream output;
	
	public JavaSerializer(OutputStream out) throws IOException{
		output = new ObjectOutputStream(out);
	}
	
	public void writeObject(Object o) throws SerializationException, IOException{
		try {
			output.writeObject(o);
		} catch(InvalidClassException e){
			throw new SerializationException("Error serializing object", e);
		} catch(NotSerializableException e){
			throw new SerializationException("Error serializing object", e);
		}
	}
	
	public void writeString(String s) throws IOException{
		output.writeUTF(s);
	}
	
	public void writeInt(int i) throws IOException{
		output.writeInt(i);
	}
	
	public void writeShort(short s) throws IOException{
		output.writeShort(s);
	}
	
	public void writeLong(long l) throws IOException{
		output.writeLong(l);
	}
	
	public void writeDouble(double d) throws IOException{
		output.writeDouble(d);
	}
	
	public void writeFloat(float f) throws IOException{
		output.writeFloat(f);
	}
	
	public void writeByte(byte b) throws IOException{
		output.writeByte(b);
	}
	
	public void writeBoolean(boolean b) throws IOException{
		output.writeBoolean(b);
	}
	
	public void flush() throws IOException{
		output.flush();
		output.reset();
	}
	
}
