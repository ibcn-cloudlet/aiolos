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
package be.iminds.aiolos.rsa.util;


/**
 * Utility class for parsing r-osgi uris
 */
public class URI {

	private String protocol;
	private String ip;
	private int port;
	private String serviceId;
	private boolean ipv6;
	
	public URI(final String uri){
		parse(uri);
	}

	private void parse(final String uriString) {
		try {
			int cs = 0;
			int ce = uriString.length();
			final int p1 = uriString.indexOf("://"); 
			if (p1 > -1) {
				protocol = uriString.substring(0, p1);
				cs = p1 + 3;
			} else {
				protocol = "r-osgi"; 
			}
			final int p2 = uriString.lastIndexOf('#'); 
			if (p2 > -1) {
				serviceId = uriString.substring(p2 + 1);
				ce = p2;
			}
			final int p3 = uriString.lastIndexOf(':');
			if (p3 > -1) {
				port = Integer.parseInt(uriString.substring(p3 + 1, ce));
				ce = p3;
			} else {
				if ("r-osgi".equals(protocol)) { 
					// FIXME: this should be the actual port of this instance
					// !?!
					port = 9278;
				} else if ("http".equals(protocol)) {
					port = 80;
				} else if ("https".equals(protocol)) { 
					port = 443;
				}
			}
			if(uriString.charAt(cs)=='[' && uriString.charAt(ce-1)==']'){
				ipv6 = true;
				cs++;
				ce--;
			} else {
				ipv6 = false;
			}
			
			
			ip = uriString.substring(cs, ce);
		} catch (final IndexOutOfBoundsException i) {
			throw new IllegalArgumentException(uriString + " caused " //$NON-NLS-1$
					+ i.getMessage());
		}
	}
	
	public String getProtocol(){
		return protocol;
	}
	
	public String getIP(){
		return ip;
	}
	
	public int getPort(){
		return port;
	}
	
	public String getServiceId(){
		return serviceId;
	}
	
	public String getAddress(){
		return (ipv6 ? "[" : "")
				+ ip 
				+ (ipv6 ? "]" : "")
				+ ":"+port;
	}
	
	public String toString() {
		return protocol + "://" + getAddress() + "#" +serviceId; 
	}

	public boolean equals(final Object other) {
		if (other instanceof String) {
			return equals(new URI((String) other));
		} else if (other instanceof URI) {
			final URI otherURI = (URI) other;
			return protocol.equals(otherURI.protocol)
					&& ip.equals(otherURI.ip)
					&& port == otherURI.port
					&& serviceId == otherURI.serviceId;
		} else {
			return false;
		}
	}
}
