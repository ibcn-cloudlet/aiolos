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
package be.iminds.aiolos.proxy.command;

import be.iminds.aiolos.proxy.api.ProxyPolicy;
import be.iminds.aiolos.proxy.policy.RandomPolicy;
import be.iminds.aiolos.proxy.policy.RoundRobinPolicy;

/**
 * Factory to convert Strings given as CLI argument
 * to {@link ProxyPolicy} objects.
 */
public class ProxyPolicyFactory {

	public static ProxyPolicy createPolicy(String policy){
		ProxyPolicy p = null;
		if(policy.equals("random")){
			p = new RandomPolicy();
		} else if(policy.equals("roundrobin")){
			p = new RoundRobinPolicy();
		}
		return p;
	}
}
