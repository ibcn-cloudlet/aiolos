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
package be.iminds.aiolos.platform.command;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.osgi.service.log.LogService;

import be.iminds.aiolos.info.ComponentInfo;
import be.iminds.aiolos.info.NodeInfo;
import be.iminds.aiolos.monitor.service.api.MethodMonitorInfo;
import be.iminds.aiolos.monitor.service.api.ServiceMonitorInfo;
import be.iminds.aiolos.platform.Activator;
import be.iminds.aiolos.platform.PlatformManagerImpl;

/**
 * CLI Commands for the PlatformManager
 */
public class PlatformCommands {

	private final PlatformManagerImpl platformManager;
	
	private final ExecutorService executor = Executors.newCachedThreadPool();
	
	
	public PlatformCommands(PlatformManagerImpl am){
		this.platformManager = am;
	}
	
	public void nodes(){
		for(NodeInfo nodeInfo : platformManager.getNodes()){
			System.out.println(nodeInfo.getNodeId());
			for(ComponentInfo component : platformManager.getComponents(nodeInfo.getNodeId())){
				System.out.println(" * "+component);
			}
			System.out.println("");
		}
	}
	
	public void start(String componentId, String nodeId){
		try {
			platformManager.startComponent(componentId, nodeId);
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void start(String componentId, String version, String nodeId){
		try {
			platformManager.startComponent(componentId, version, nodeId);
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void stop(String componentId, String version, String nodeId){
		try {
			platformManager.stopComponent(new ComponentInfo(componentId, version, nodeId));
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void stop(String nodeId){
		try {
			platformManager.stopNode(nodeId);
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void scale(final String componentId, final int requestedInstances){
		executor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					platformManager.scaleComponent(componentId, requestedInstances, true);
					Activator.logger.log(LogService.LOG_INFO, "Application succesfully scaled");
				}catch(Exception e){
					e.printStackTrace();
					Activator.logger.log(LogService.LOG_ERROR, "Error scaling application", e);
				}
			}
		});
	}
}
