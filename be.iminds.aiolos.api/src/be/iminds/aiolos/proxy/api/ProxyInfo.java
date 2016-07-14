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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import be.iminds.aiolos.info.ComponentInfo;
import be.iminds.aiolos.info.ServiceInfo;

/**
 * Information about a proxy instance.
 */
public class ProxyInfo {

	private final String serviceId;
	private final String componentId;
	private final String version;
	private final String nodeId;
	private final List<ServiceInfo> instances;
	private final List<ComponentInfo> users;
	private final String policy;
	
	public ProxyInfo(String serviceId, String componentId,
			String version, String nodeId,
			Collection<ServiceInfo> instances, 
			Collection<ComponentInfo> users, String policy){
		this.serviceId = serviceId;
		this.componentId = componentId;
		this.version = version;
		this.nodeId = nodeId;
		List<ServiceInfo> services = new ArrayList<ServiceInfo>();
		services.addAll(instances);
		this.instances = Collections.unmodifiableList(services);
		List<ComponentInfo> components = new ArrayList<ComponentInfo>();
		components.addAll(users);
		this.users = Collections.unmodifiableList(components);
		this.policy = policy;
	}

	public String getServiceId() {
		return serviceId;
	}

	public String getComponentId() {
		return componentId;
	}
	
	public String getVersion(){
		return version;
	}
	
	public String getNodeId(){
		return nodeId;
	}

	public Collection<ServiceInfo> getInstances() {
		return instances;
	}
	
	public Collection<ComponentInfo> getUsers(){
		return users;
	}

	public String getPolicy() {
		return policy;
	}
	
	public boolean equals(Object other){
		if(!(other instanceof ProxyInfo))
			return false;
		
		ProxyInfo p = (ProxyInfo) other;
		return(p.serviceId.equals(serviceId)
				&& p.componentId.equals(componentId)
				&& p.version.equals(version)
				&& p.nodeId.equals(nodeId)
				);
	}
	
	public int hashCode(){
		return (serviceId+componentId+version+nodeId).hashCode();
	}
	
	public String toString(){
		return "Proxy "+serviceId+":"+componentId+"-"+version+"@"+nodeId;
	}
}
