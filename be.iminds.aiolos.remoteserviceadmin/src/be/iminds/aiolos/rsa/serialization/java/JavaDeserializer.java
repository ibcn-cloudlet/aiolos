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
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.OptionalDataException;
import java.io.UTFDataFormatException;

import be.iminds.aiolos.rsa.serialization.api.Deserializer;
import be.iminds.aiolos.rsa.serialization.api.SerializationException;

/**
 * {@link Deserializer} using the default Java Serialization mechanism.
 */
public class JavaDeserializer implements Deserializer{

	private final ObjectInputStream input;
	
	public JavaDeserializer(InputStream in) throws IOException{
		input = new ObjectInputStream(in);
	}
	
	public Object readObject() throws SerializationException, IOException{
		try {
			Object o = input.readObject();
			return o;
		} catch(ClassNotFoundException e){
			throw new SerializationException("Error reading object",e);
		} catch(InvalidClassException e){
			throw new SerializationException("Error reading object",e);
		} catch(OptionalDataException e){
			throw new SerializationException("Error reading object",e);
		}
	}
	
	public String readString() throws SerializationException, IOException{
		try {
			return input.readUTF();
		} catch(UTFDataFormatException e){
			throw new SerializationException("Error reading string", e);
		}
	}
	
	public int readInt() throws IOException{
		return input.readInt();
	}
	
	public short readShort() throws IOException{
		return input.readShort();
	}
	
	public long readLong() throws IOException{
		return input.readLong();
	}
	
	public double readDouble() throws IOException{
		return input.readDouble();
	}
	
	public float readFloat() throws IOException{
		return input.readFloat();
	}
	
	public byte readByte() throws IOException{
		return input.readByte();
	}
	
	public boolean readBoolean() throws IOException{
		return input.readBoolean();
	}
}
