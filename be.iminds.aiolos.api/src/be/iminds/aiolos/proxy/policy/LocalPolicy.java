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

import java.util.Collection;

import be.iminds.aiolos.info.ServiceInfo;
import be.iminds.aiolos.proxy.api.ProxyPolicy;

/**
 * The {@link LocalPolicy} prefers a {@link ServiceInstance} running
 * on the local OSGi framework, or the first one available when no local
 * instances available.
 */
public class LocalPolicy implements ProxyPolicy  {

	String nodeId;
	
	public LocalPolicy(String nodeId){
		this.nodeId = nodeId;
	}
	
	@Override
	public ServiceInfo selectTarget(Collection<ServiceInfo> targets, 
			String componentId, String serviceId, String method, Object[] args) {
		ServiceInfo target = null;
		for(ServiceInfo t : targets){
			if(t.getComponent().getNodeId().equals(nodeId)){
				target = t;
				break;
			}
		}
		if(target==null && targets.size()>0){
			target = targets.iterator().next();
		}
		return target;
	}

}
