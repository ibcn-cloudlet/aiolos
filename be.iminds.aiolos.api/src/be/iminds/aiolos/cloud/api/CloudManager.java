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
package be.iminds.aiolos.cloud.api;

import java.util.Collection;
import java.util.Dictionary;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * The {@link CloudManager} interfaces with the Cloud management system
 * and offers an interface to start/stop virtual machines, execute scripts
 * on them etc.
 */
public interface CloudManager {
	
	/**
	 * List all running VM instances on the cloud
	 * @return a list of VM instances
	 */
    List<VMInstance> listVMs();
    
    /**
     * Starts a new VM instance and initializes an OSGi runtime with given
     * bndrun configuration
     * @param bndrun the bndrun configuration to initialize the OSGi runtime
     * @param resources a list of resource files to use for the OSGi runtime (e.g. configuration files)
     * @return reference to the instance started
     * @throws CloudException
     * @throws TimeoutException 
     */
    VMInstance startVM(String bndrun, List<String> resources) throws CloudException, TimeoutException;
    
    /**
     * Starts a number of new VM instance and initializes an OSGi runtime with given
     * bndrun configuration
     * @param bndrun the bndrun configuration to initialize the OSGi runtime
     * @param resources a list of resource files to use for the OSGi runtime (e.g. configuration files)
     * @param count the number of VM instances to start 
     * @return list of references to the started VM instances
     * @throws CloudException
     * @throws TimeoutException 
     */
    List<VMInstance> startVMs(String bndrun, List<String> resources, int count) throws CloudException, TimeoutException;
    
    
    /**
     * Stop a VM instance with given id
     * @param id the id of the VM instance to stop
     * @return reference to the stopped VM instance
     */
    VMInstance stopVM(String id);
    
    /**
     * Stop a VM instances with given ids
     * @param ids an array of ids of the VM instances to stop
     * @return references to the stopped VM instances
     */
    List<VMInstance> stopVMs(String[] ids);
    
    /**
     * Stop all VM instances started by this cloudmanager
     * @return references to all stopped VM instances
     */
    List<VMInstance> stopVMs();
    
	/**
	 * List all bndrun files accessible by this framework. Only files starting with 'run' and ending with '.bndrun' are shown.
	 * @return A collection of accessible bndrun files.
	 */
	Collection<String> getBndrunFiles();
   
}
