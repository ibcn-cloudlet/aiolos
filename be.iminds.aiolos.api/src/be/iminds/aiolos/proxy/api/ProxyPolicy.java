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
package be.iminds.aiolos.proxy.api;

import java.util.Collection;

import be.iminds.aiolos.info.ServiceInfo;

/**
 * The {@link ProxyPolicy} decides to which instance a method call is forwarded
 */
public interface ProxyPolicy {

	/**
	 * Determines which instance the method call should be forwarded
	 * 
	 * @param targets		Collection of possible target instances
	 * @param componentId	Identifier of the component providing the service
	 * @param serviceId		Identifier of the service providing the method
	 * @param method		Name of the method that is called
	 * @param args			The arguments the method is called with
	 * @return	The instance to which the call should be forwarded according to the policy
	 */
	public ServiceInfo selectTarget(Collection<ServiceInfo> targets, 
			String componentId, String serviceId, String method, Object[] args);
	
}
