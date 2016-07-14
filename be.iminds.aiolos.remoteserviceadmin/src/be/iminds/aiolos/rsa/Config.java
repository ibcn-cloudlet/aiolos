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
package be.iminds.aiolos.rsa;

/**
 * Configuration object for the {@link ROSGiServiceAdmin}
 */
public class Config {

	public enum SerializationStrategy {
		JAVA,
		KRYO
	}
	
	public static String CONFIG_ROSGI = "be.iminds.aiolos.r-osgi";
	
	public static String PROP_INTERFACE = "rsa.interface";
	public static String PROP_IP = "rsa.ip";
	public static String PROP_PORT = "rsa.port";
	public static String PROP_IPV6 = "rsa.ipv6";
	public static String PROP_SERIALIZATION = "rsa.serialization";
	public static String PROP_TIMEOUT = "rsa.timeout";
	
	
	public static int PORT = 9278;  // rsa.port
	public static String NETWORK_INTERFACE = "eth0";  //rsa.interface
	public static String IP = null; // rsa.ip
	public static int TIMEOUT = 15000; // rsa.timeout
	public static boolean IPV6 = false; // rsa.ipv6
	
	public static SerializationStrategy SERIALIZATION = SerializationStrategy.KRYO; // rsa.serialization

}
