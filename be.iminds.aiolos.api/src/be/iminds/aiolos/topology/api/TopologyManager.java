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
package be.iminds.aiolos.topology.api;

import java.util.List;

import org.osgi.service.remoteserviceadmin.EndpointDescription;

import be.iminds.aiolos.info.NodeInfo;

/**
 * The TopologyManager provides an interface to manage the topology
 * of the platform. It allows to connect to other nodes, and find service 
 * endpoints on other nodes in the topology.
 *
 */
public interface TopologyManager {

	public NodeInfo connect(String ip, int port);
	
	public void disconnect(String nodeId);
	
	public List<EndpointDescription> find(String service, String filter);
}
