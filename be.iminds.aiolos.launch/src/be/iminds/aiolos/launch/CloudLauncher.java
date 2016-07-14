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
package be.iminds.aiolos.launch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.osgi.service.log.LogService;

import be.iminds.aiolos.cloud.api.CloudManager;
import be.iminds.aiolos.cloud.api.VMInstance;

public class CloudLauncher {

	private CloudManager cloudManager;
	
	public CloudLauncher(CloudManager cm){
		this.cloudManager = cm;
	}
	
	public void launch(){
		Activator.logger.log(LogService.LOG_INFO, "Launching AIOLOS ...");
		List<String> resources = new ArrayList<String>();
		File dir = new File("resources");
		for(String name : dir.list()){
			resources.add("resources/" + name);
		}
		try {
			VMInstance instance = cloudManager.startVM(Activator.bndrun, resources);
			System.out.println("Succesfully initialized AIOLOS management instance - Access is available through the webinterface (default: http://"+instance.getPublicAddresses().iterator().next()+":8080/system/console/aiolos-nodes user:pass admin:admin)");
		} catch(Exception e){
			Activator.logger.log(LogService.LOG_ERROR, "Failed to launch", e);
		}
	}
	
	public void kill(){
		Activator.logger.log(LogService.LOG_INFO, "Killing all cloud VMs");
		cloudManager.stopVMs();
	}
}
