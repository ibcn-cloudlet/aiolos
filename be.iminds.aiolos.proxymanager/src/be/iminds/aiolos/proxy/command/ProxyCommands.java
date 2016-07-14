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

import java.util.Collection;

import be.iminds.aiolos.info.ServiceInfo;
import be.iminds.aiolos.proxy.api.ProxyInfo;
import be.iminds.aiolos.proxy.api.ProxyManager;

/**
 * CLI Commands for the ProxyManager
 */
public class ProxyCommands {

	private final ProxyManager proxyManager;
	
	public ProxyCommands(ProxyManager mgr){
		this.proxyManager = mgr;
	}
	
	public void list(){
		Collection<ProxyInfo> infos = this.proxyManager.getProxies();
		if(infos.size()==0){
			System.out.println("No proxies available");
			return;
		}
		for(ProxyInfo info : infos){
			printProxyInfo(info);
		}
	}
	
	public void list(String componentId){
		Collection<ProxyInfo> infos = this.proxyManager.getProxies();
		if(infos==null){
			System.err.println("No proxies available for component "+componentId);
			return;
		}
		for(ProxyInfo info : infos){
			if(info.getComponentId().equals(componentId))
				printProxyInfo(info);
		}
	}
	
	public void setpolicy(String componentId, String serviceId, String policy){
		Collection<ProxyInfo> infos = this.proxyManager.getProxies();
		if(infos==null){
			System.err.println("No proxies available for component "+componentId);
			return;
		}
		for(ProxyInfo info : infos){
			if(info.getComponentId().equals(componentId)
					&& info.getServiceId().equals(serviceId)){
				this.proxyManager.setProxyPolicy(info, ProxyPolicyFactory.createPolicy(policy));
			}
				
		}
		
	}
	
	private void printProxyInfo(ProxyInfo info){
		System.out.println("Proxy of "+info.getComponentId()+" "+info.getServiceId());
		System.out.println("Instances :");
		for(ServiceInfo instance : info.getInstances()){
			System.out.println(" * "+instance.getNodeId()+" - "+instance.getComponentId());
		}
		System.out.println("Policy : "+info.getPolicy());
		System.out.println("------------------------------");
	}
}
