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
package be.iminds.aiolos.rsa.serialization.api;

import java.io.IOException;

/**
 * Interface for reading raw bytes into objects.
 */
public interface Deserializer {

	public Object readObject() throws IOException, SerializationException;
	
	public String readString() throws IOException, SerializationException;
	
	public int readInt() throws IOException, SerializationException;
	
	public short readShort() throws IOException, SerializationException;
	
	public long readLong() throws IOException, SerializationException;
	
	public double readDouble() throws IOException, SerializationException;
	
	public float readFloat() throws IOException, SerializationException;
	
	public byte readByte() throws IOException, SerializationException;
	
	public boolean readBoolean() throws IOException, SerializationException;
}
