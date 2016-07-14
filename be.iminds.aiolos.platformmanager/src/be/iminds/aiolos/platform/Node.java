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
package be.iminds.aiolos.platform;

import be.iminds.aiolos.cloud.api.VMInstance;
import be.iminds.aiolos.deployment.api.DeploymentManager;
import be.iminds.aiolos.info.NodeInfo;
import be.iminds.aiolos.monitor.node.api.NodeMonitor;
import be.iminds.aiolos.monitor.service.api.ServiceMonitor;
import be.iminds.aiolos.proxy.api.ProxyManager;

/**
 * A {@link Node} represents a connected OSGi runtime running
 * on a device connected in the network. The Node holds references
 * to all relevant AIOLOS services of the OSGi runtime
 * 
 */
public class Node {

	private NodeInfo info;
	private VMInstance instance = null;
	private volatile DeploymentManager deploymentManager;
	private volatile ProxyManager proxyManager;
	private volatile ServiceMonitor serviceMonitor;
	private volatile NodeMonitor nodeMonitor;

	public Node(NodeInfo info){
		this.info = info;
	}
	
	public String getNodeId(){
		return this.info.getNodeId();
	}

	public void setInfo(NodeInfo info){
		this.info = info;
	}
	
	public NodeInfo getInfo(){
		return this.info;
	}
	
	public void setDeploymentManager(DeploymentManager deploymentManager) {
		this.deploymentManager = deploymentManager;
	}

	public void setProxyManager(ProxyManager proxyManager) {
		this.proxyManager = proxyManager;
	}

	public void setServiceMonitor(ServiceMonitor serviceMonitor) {
		this.serviceMonitor = serviceMonitor;
	}
	
	public void setNodeMonitor(NodeMonitor nodeMonitor) {
		this.nodeMonitor = nodeMonitor;
	}
	
	public void setVMInstance(VMInstance instance) {
		this.instance = instance;
	}

	public DeploymentManager getDeploymentManager() {
		return deploymentManager;
	}

	public ProxyManager getProxyManager() {
		return proxyManager;
	}

	public ServiceMonitor getServiceMonitor() {
		return serviceMonitor;
	}
	
	public NodeMonitor getNodeMonitor() {
		return nodeMonitor;
	}
	
	public VMInstance getVMInstance() {
		return instance;
	}
}
