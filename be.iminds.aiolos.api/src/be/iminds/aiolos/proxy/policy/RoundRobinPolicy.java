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
package be.iminds.aiolos.proxy.policy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import be.iminds.aiolos.info.NodeInfo;
import be.iminds.aiolos.info.ServiceInfo;
import be.iminds.aiolos.proxy.api.ProxyPolicy;

/**
 * The {@link RoundRobinPolicy} is a {@link ProxyPolicy} 
 * that distributes calls in a round robin fashion
 *
 */
public class RoundRobinPolicy implements ProxyPolicy {

	private volatile int index = 0;
	
	private final List<String> candidates;
	
	/**
	 * Round robin between all service instances available
	 */
	public RoundRobinPolicy(){
		candidates = null;
	}
	
	/**
	 * Restrict round robin to the nodes specified in @nodes
	 */
	public RoundRobinPolicy(List<NodeInfo> nodes){
		this.candidates = new ArrayList<String>();
		for(NodeInfo node : nodes){
			candidates.add(node.getNodeId());
		}
	}
	
	@Override
	public ServiceInfo selectTarget(Collection<ServiceInfo> targets, String componentId,
			String serviceId, String method, Object[] args) {
		ServiceInfo result = null;
		index++;
		if(index >= targets.size())
			index = 0;
		if(targets.size()>0){
			Iterator<ServiceInfo> it = targets.iterator();
			int i = -1;
			do {
				ServiceInfo next = it.next();
				if(candidates==null || candidates.contains(next.getNodeId()))
					result = next;
				i++;
			} while( (i<index || (candidates!=null && !candidates.contains(result.getNodeId())))
					&& it.hasNext());
			index = i;
		}
		return result;
	}

}
