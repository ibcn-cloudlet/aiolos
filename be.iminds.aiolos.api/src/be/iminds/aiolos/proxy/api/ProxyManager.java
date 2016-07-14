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

import be.iminds.aiolos.info.ComponentInfo;
import be.iminds.aiolos.info.ServiceInfo;

/**
 * Provides an API to query information about all the instantiated proxies,
 * and allows to setup a new policy for each proxy.
 * 
 * Each component service is proxied, where one proxy is responsible for a given service interface of 
 * a certain bundle. A bundle is identified by its Bundle-SymbolicName, unless the aiolos.component.id is set.
 * A service is identified by the fully qualified name of the service interface, unless different instances
 * of the same service interface are possible, in which case an instance identifier is added to the serviceId.
 * The instance identifier can be set using the aiolos.instance.id.
 * 
 * The proxy forwards each call to a known service instance (represented by {@link ServiceInfo}) implementing 
 * the proxied service which is uniquely identified by the componentId (= Bundle-SymbolicName or 
 * aiolos.component.id), its version, the node where the proxy is deployed and the serviceId (= serviceInterface+"-"+aiolos.instance.id). 
 * Multiple such instances can exist (i.e. one local and N remote instances).
 * 
 * For each method call the proxy forwards the call to one of the known instances, according to a given
 * {@link ProxyPolicy}. Also before and after each method call all {@link ServiceProxyListener}s are notified,
 * which can be used to gather monitor information about the component (e.g. call time, number of calls, etc.).
 *  
 */
public interface ProxyManager {

	/**
	 * Fetch info about all proxies managed by this ProxyManager
	 * @return All proxies
	 */
	public Collection<ProxyInfo> getProxies();
	
	/**
	 * Fetch info about all proxies managed by this ProxyManager for a given component instance
	 * @param component 	The component instance for which proxies are queried
	 * @return All proxies for services registered by the given component
	 */
	public Collection<ProxyInfo> getProxies(ComponentInfo component);
	
	/**
	 * Set the policy of a proxy
	 * @param proxy 	The proxy of which to change the policy
	 * @param policy 	The new policy to enforce
	 */
	public void setProxyPolicy(ProxyInfo proxy, ProxyPolicy policy);
	
	/**
	 * Query all service instaces on this node that are proxied by the ProxyManager
	 * @return A collection of service instances
	 */
	public Collection<ServiceInfo> getServices();
	
	/**
	 * Query all service instances registered by a given component that are proxied
	 * by the ProxyManager
	 * @param component 	The component for which services are queried
	 * @return A collection of service instances
	 */
	public Collection<ServiceInfo> getServices(ComponentInfo component);
}
