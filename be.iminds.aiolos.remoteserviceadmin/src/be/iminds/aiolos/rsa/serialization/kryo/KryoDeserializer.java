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
package be.iminds.aiolos.rsa.serialization.kryo;

import java.io.IOException;
import java.io.InputStream;

import be.iminds.aiolos.rsa.serialization.api.Deserializer;
import be.iminds.aiolos.rsa.serialization.api.SerializationException;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;

/**
 * {@link Deserializer} using the Kryo library.
 */
public class KryoDeserializer implements Deserializer {

	private final Kryo kryo;
	private final Input input;
	
	public KryoDeserializer(InputStream in){
		//com.esotericsoftware.minlog.Log.set(Log.LEVEL_TRACE);
		this.kryo = KryoFactory.createKryo();
		
		this.input = new Input(in);
	}
	
	public void finalize(){
		KryoFactory.removeKryo(kryo);
	}
	
	@Override
	public Object readObject() throws IOException, SerializationException {
		try {
			return kryo.readClassAndObject(input);
		} catch(Throwable e ){
			e.printStackTrace();
			if(e.getCause()!=null && e.getCause() instanceof IOException){
				throw (IOException)e.getCause();
			} else {
				if(e.getMessage().equals("Buffer underflow.")){
					throw new IOException(e);
				}
				throw new SerializationException("Error serializing object", e);
			}
		} finally {
			kryo.reset();
		}
	}

	@Override
	public String readString() throws IOException, SerializationException {
		try {
			return kryo.readObject(input, String.class);
		} catch(Throwable e) {
			if(e.getCause()!=null && e.getCause() instanceof IOException){
				throw (IOException)e.getCause();
			} else {
				if(e.getMessage().equals("Buffer underflow.")){
					throw new IOException(e);
				}
				throw new SerializationException("Error serializing string", e);
			}
		}
	}

	@Override
	public int readInt() throws IOException, SerializationException{
		try {
			return kryo.readObject(input, Integer.class);
		} catch(Throwable e ){
			if(e.getCause()!=null && e.getCause() instanceof IOException){
				throw (IOException)e.getCause();
			} else {
				if(e.getMessage().equals("Buffer underflow.")){
					throw new IOException(e);
				}
				throw new SerializationException("Error serializing object", e);
			}
		}
	}

	@Override
	public short readShort() throws IOException , SerializationException {
		try {
			return kryo.readObject(input, Short.class);
		} catch(Throwable e ){
			if(e.getCause()!=null && e.getCause() instanceof IOException){
				throw (IOException)e.getCause();
			} else {
				if(e.getMessage().equals("Buffer underflow.")){
					throw new IOException(e);
				}
				throw new SerializationException("Error serializing object", e);
			}
		}
	}

	@Override
	public long readLong() throws IOException , SerializationException{
		try {
			return kryo.readObject(input, Long.class);
		} catch(Throwable e ){
			if(e.getCause()!=null && e.getCause() instanceof IOException){
				throw (IOException)e.getCause();
			} else {
				if(e.getMessage().equals("Buffer underflow.")){
					throw new IOException(e);
				}
				throw new SerializationException("Error serializing object", e);
			}
		}
	}

	@Override
	public double readDouble() throws IOException, SerializationException {
		try {
			return kryo.readObject(input, Double.class);
		} catch(Throwable e ){
			if(e.getCause()!=null && e.getCause() instanceof IOException){
				throw (IOException)e.getCause();
			} else {
				if(e.getMessage().equals("Buffer underflow.")){
					throw new IOException(e);
				}
				throw new SerializationException("Error serializing object", e);
			}
		}
	}

	@Override
	public float readFloat() throws IOException , SerializationException {
		try {
			return kryo.readObject(input, Float.class);
		} catch(Throwable e ){
			if(e.getCause()!=null && e.getCause() instanceof IOException){
				throw (IOException)e.getCause();
			} else {
				if(e.getMessage().equals("Buffer underflow.")){
					throw new IOException(e);
				}
				throw new SerializationException("Error serializing object", e);
			}
		}
	}

	@Override
	public byte readByte() throws IOException, SerializationException {
		try {
			return kryo.readObject(input, Byte.class);
		} catch(Throwable e ){
			if(e.getCause()!=null && e.getCause() instanceof IOException){
				throw (IOException)e.getCause();
			} else {
				if(e.getMessage().equals("Buffer underflow.")){
					throw new IOException(e);
				}
				throw new SerializationException("Error serializing object", e);
			}
		}
	}

	@Override
	public boolean readBoolean() throws IOException, SerializationException {
		try {
			return kryo.readObject(input, Boolean.class);
		} catch(Throwable e ){
			if(e.getCause()!=null && e.getCause() instanceof IOException){
				throw (IOException)e.getCause();
			} else {
				if(e.getMessage().equals("Buffer underflow.")){
					throw new IOException(e);
				}
				throw new SerializationException("Error serializing object", e);
			}
		}
	}

}
