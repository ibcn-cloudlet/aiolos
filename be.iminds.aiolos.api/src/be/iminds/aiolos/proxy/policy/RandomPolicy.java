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
import java.util.Iterator;
import java.util.Random;

import be.iminds.aiolos.info.ServiceInfo;
import be.iminds.aiolos.proxy.api.ProxyPolicy;

/**
 * The {@link RandomPolicy} is a {@link ProxyPolicy} 
 * that chooses a random instance to forward the call to.
 *
 */
public class RandomPolicy implements ProxyPolicy {

	private final Random random = new Random(System.currentTimeMillis());
	
	@Override
	public ServiceInfo selectTarget(Collection<ServiceInfo> targets, String componentId,
			String serviceId, String method, Object[] args) {
		ServiceInfo result = null;
		if(targets.size()>0){
			int index = random.nextInt(targets.size());
			Iterator<ServiceInfo> it = targets.iterator();
			for(int i=0;i<index;i++){
				result = it.next();
			}
		}
		return result;
	}

}
