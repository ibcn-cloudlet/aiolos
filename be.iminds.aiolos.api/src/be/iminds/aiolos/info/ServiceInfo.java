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
 * Uniquely represents one Service instance deployed on the AIOLOS platform
 *
 * A service instance is uniquely identified by:
 * - the identifier of the service (= the service interface + (optionally) an aiolos.instanceId)
 * - the component instance that registered the service
 */
public class ServiceInfo {

	private final String serviceId;
	private final ComponentInfo component;
	
	public ServiceInfo(String serviceId, ComponentInfo component){
		this.serviceId = serviceId;
		this.component = component;
	}
	
	/**
	 * @return the identifier of the service
	 */
	public String getServiceId(){
		return serviceId;
	}
	
	/**
	 * @return the component instance that registered the service
	 */
	public ComponentInfo getComponent(){
		return component;
	}
	
	public String getComponentId(){
		return component.getComponentId();
	}
	
	public String getVersion(){
		return component.getVersion();
	}
	
	public String getNodeId(){
		return component.getNodeId();
	}
	
	public boolean equals(Object other){
		if(!(other instanceof ServiceInfo))
			return false;
		
		ServiceInfo s = (ServiceInfo) other;
		return (s.serviceId.equals(serviceId)
				&& s.component.equals(component));
	}
	
	public int hashCode(){
		return serviceId.hashCode()+component.hashCode();
	}
	
	public String toString(){
		return serviceId+":"+component;
	}
}
