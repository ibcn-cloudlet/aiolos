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
package be.iminds.aiolos.topology.command;

import be.iminds.aiolos.info.NodeInfo;
import be.iminds.aiolos.topology.TopologyManagerImpl;

public class TopologyManagerCommands {
	
private final TopologyManagerImpl topologyManager;

	public TopologyManagerCommands(TopologyManagerImpl tm){
		this.topologyManager = tm;
	}

	public void connect(String ip){
		connect(ip, 9278);
	}
	
	public void connect(String ip, int port){
		try {
			NodeInfo n = topologyManager.connect(ip, port);
			if(n==null){
				System.out.println("Failed to connect to "+ip+":"+port);
			}
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void disconnect(String nodeId){
		try {
			topologyManager.disconnect(nodeId);
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
}
