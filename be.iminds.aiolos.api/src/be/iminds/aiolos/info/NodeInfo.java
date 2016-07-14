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
package be.iminds.aiolos.info;

/**
 * Uniquely represents one Node running on the AIOLOS platform
 * 
 * A node is uniquely identifed by the node ID, which is a UUID String
 */
public class NodeInfo {

	private final String nodeId;
	private final String ip; // ip address of the node
	private final int rsaPort; // port to find endpoints
	private final int httpPort; // port on which http server is available
	private final String name; // human readable name (hostname?)
	private final String arch; // node architecture
	private final String os; // operating system
	
	public NodeInfo(String nodeId, String ip, int rsaPort, int httpPort,
			String name, String arch, String os){
		this.nodeId = nodeId;
		this.ip = ip;
		this.rsaPort = rsaPort;
		this.httpPort = httpPort;
		this.name = name;
		this.arch = arch;
		this.os = os; 
	}
	
	/**
	 * @return the UUID of the node
	 */
	public String getNodeId(){
		return nodeId;
	}
	
	public String getName(){
		return name;
	}
	
	public String getIP(){
		return ip;
	}
	
	public int getRsaPort(){
		return rsaPort;
	}
	
	public int getHttpPort(){
		return httpPort;
	}
	
	public String getOS(){
		return os;
	}
	
	public String getArch(){
		return arch;
	}
	
	public boolean equals(Object other){
		if(!(other instanceof NodeInfo))
			return false;
		
		NodeInfo n = (NodeInfo) other;
		return n.nodeId.equals(nodeId);
	}
	
	public int hashCode(){
		return nodeId.hashCode();
	}
	
	public String toString(){
		return nodeId+"@"+ip+":"+rsaPort;
	}
}
