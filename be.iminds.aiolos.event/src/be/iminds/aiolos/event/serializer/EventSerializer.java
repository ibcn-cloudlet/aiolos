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
package be.iminds.aiolos.event.serializer;

import java.util.HashMap;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventProperties;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class EventSerializer extends Serializer<Event> {

	@Override
	public Event read(Kryo kryo, Input input, Class<Event> clazz) {
		String topic = input.readString();
		HashMap<String, Object> properties = (HashMap<String, Object>) kryo.readClassAndObject(input);
		Event e = new Event(topic, new EventProperties(properties));
		return e;
	}

	@Override
	public void write(Kryo kryo, Output output, Event event) {
		String topic = event.getTopic();
		HashMap<String, Object> properties = new HashMap<String, Object>();
		for(String k : event.getPropertyNames()){
			properties.put(k, event.getProperty(k));
		}
		output.writeString(topic);
		kryo.writeClassAndObject(output, properties);
	}

}
