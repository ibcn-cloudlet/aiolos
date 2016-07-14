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
package be.iminds.aiolos.deployment.command;

import java.util.Collection;
import java.util.Iterator;

import be.iminds.aiolos.deployment.api.DeploymentManager;
import be.iminds.aiolos.info.ComponentInfo;

/**
 * CLI Commands for the DeploymentManager
 */
public class DeploymentCommands {

	private final DeploymentManager deploymentManager;
	
	public DeploymentCommands(DeploymentManager dm){
		this.deploymentManager = dm;
	}
	
	public void start(String componentId){
		try {
			deploymentManager.startComponent(componentId);
			System.out.println(componentId+" started!");
		} catch (Exception e) {
			System.err.println("Error starting "+componentId);
			e.printStackTrace();
		}
	}
	
	public void start(String componentId, String version){
		try {
			deploymentManager.startComponent(componentId, version);
			System.out.println(componentId+" started!");
		} catch (Exception e) {
			System.err.println("Error starting "+componentId);
			e.printStackTrace();
		}
	}
	
	public void stop(String componentId){
		try {
			Iterator<ComponentInfo> it = deploymentManager.getComponents().iterator();
			while(it.hasNext()){
				ComponentInfo component = it.next();
				if(component.getComponentId().equals(componentId)){
					deploymentManager.stopComponent(component);
				}
			}
			System.out.println(componentId+" stopped!");
		} catch (Exception e) {
			System.err.println("Error stopping "+componentId);
			e.printStackTrace();
		}
	}
	
	public void stop(String componentId, String version){
		try {
			Iterator<ComponentInfo> it = deploymentManager.getComponents().iterator();
			while(it.hasNext()){
				ComponentInfo component = it.next();
				if(component.getComponentId().equals(componentId)
						&& component.getVersion().equals(version)){
					deploymentManager.stopComponent(component);
				}
			}
			System.out.println(componentId+"-"+version+" stopped!");
		} catch (Exception e) {
			System.err.println("Error stopping "+componentId);
			e.printStackTrace();
		}
	}
	
	public void list(){
		Collection<ComponentInfo> components = deploymentManager.getComponents();
		if(components.isEmpty()){
			System.out.println("No application components installed");
			return;
		}
		
		System.out.println("Installed application components: ");
		for(ComponentInfo component : components){
			System.out.println(" * "+component);
		}
	}
}
