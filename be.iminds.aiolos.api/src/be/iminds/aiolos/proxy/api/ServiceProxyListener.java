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

import be.iminds.aiolos.info.ServiceInfo;

/**
 * Callback interface that is called each time before a method is called and before the method returns.
 *
 */
public interface ServiceProxyListener {

	/**
	 * Called when a proxied method is called
	 * 
	 * @param service		Instance of the service called
	 * @param methodName	Name of the method called
	 * @param threadId		Identifier of the thread executing the method call
	 * @param args			Arguments with which the method is called
	 * @param timestamp		Timestamp when the method is called
	 */
	public void methodCalled(ServiceInfo service, String methodName, 
			long threadId, Object[] args, long timestamp);
	
	/**
	 * Called when a proxied method returns
	 * 
	 * @param service		Instance of the service called
	 * @param methodName	Name of the method called
	 * @param threadId		Identifier of the thread that executed the method call
	 * @param ret			Return value
	 * @param timestamp		Timestamp when the method returned
	 */
	public void methodReturned(ServiceInfo service, String methodName, 
			long threadId, Object ret, long timestamp);
	
}
