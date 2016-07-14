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
import java.io.OutputStream;

import be.iminds.aiolos.rsa.serialization.api.SerializationException;
import be.iminds.aiolos.rsa.serialization.api.Serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.minlog.Log;

/**
 * {@link Serializer} using the Kryo library.
 */
public class KryoSerializer implements Serializer {

	private final Kryo kryo;
	private final Output output;
	private final OutputStream out;
	
	public KryoSerializer(OutputStream out){
		//com.esotericsoftware.minlog.Log.set(Log.LEVEL_TRACE);
		this.kryo = KryoFactory.createKryo();

		this.out = out;
		this.output = new Output(out);
	}
	
	public void finalize(){
		KryoFactory.removeKryo(kryo);
	}
	
	@Override
	public void writeObject(Object o) throws SerializationException, IOException {
		try {
			kryo.writeClassAndObject(output, o);
		} catch(KryoException e){
			e.printStackTrace();
			if(e.getCause()!=null && e.getCause() instanceof IOException){
				throw (IOException)e.getCause();
			} else {
				throw new SerializationException("Error serializing object", e);
			}
		} finally {
			kryo.reset();
		}
	}

	@Override
	public void writeString(String s) throws IOException {
		kryo.writeObject(output, s);
	}

	@Override
	public void writeInt(int i) throws IOException {
		kryo.writeObject(output, i);
	}

	@Override
	public void writeShort(short s) throws IOException {
		kryo.writeObject(output, s);
	}

	@Override
	public void writeLong(long l) throws IOException {
		kryo.writeObject(output, l);
	}

	@Override
	public void writeDouble(double d) throws IOException {
		kryo.writeObject(output, d);
	}

	@Override
	public void writeFloat(float f) throws IOException {
		kryo.writeObject(output, f);
	}

	@Override
	public void writeByte(byte b) throws IOException {
		kryo.writeObject(output, b);
	}

	@Override
	public void writeBoolean(boolean b) throws IOException {
		kryo.writeObject(output, b);
	}

	@Override
	public void flush() throws IOException {
		output.flush();
		out.flush();
	}

}
