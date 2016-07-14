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
package be.iminds.aiolos.deployment.api;

import java.util.Collection;

import be.iminds.aiolos.info.ComponentInfo;

/**
 * The {@link DeploymentManager} provides an interface to start/stop/migrate components
 *
 */
public interface DeploymentManager {
	
	/**
	 * Installs a component providing the given package.
	 * 
	 * @param packageName 	The package that is required.
	 * @return component 	The component instance started.
	 * @throws Exception when no bundle is found providing the specific package or an {@link Exception} 
	 * occured during installation.
	 */
	public ComponentInfo installPackage(String packageName) throws DeploymentException;
	
	/**
	 * Installs a component providing the given package with a version range. 
	 * 
	 * @param packageName 	The package that is required.
	 * @param version 		The version range allowed e.g. "[1.0.0,2.0.0)".
	 * @return component 	The component instance started.
	 * @throws Exception when no bundle is found providing the specific package or an {@link Exception} 
	 * occured during installation.
	 */
	public ComponentInfo installPackage(String packageName, String version) throws DeploymentException;
	
	/**
	 * Starts a component with the given id.
	 * 
	 * @param componentId 	The identifier of the component to start.
	 * @return component	The component instance started.
	 * @throws Exception when the bundle cannot be resolved or an {@link Exception} occured during installation.
	 */
	public ComponentInfo startComponent(String componentId) throws DeploymentException;
	
	/**
	 * Starts a component with the given id and with a specific version range.
	 * 
	 * @param componentId 	The identifier of the component to start.
	 * @param version 		The version range allowed e.g. "[1.0.0,2.0.0)"
	 * @return component 	The component instance started.
	 * @throws Exception when the bundle cannot be resolved or an {@link Exception} occured during installation.
	 */
	public ComponentInfo startComponent(String componentId, String version) throws DeploymentException;
	
	/**
	 * Stops the given component instance.
	 * 
	 * @param component 	The component instance to stop.
	 * @throws Exception when no such bundle was installed.
	 */
	public void stopComponent(ComponentInfo component) throws DeploymentException;
	
	/**
	 * Lists all component instances running on this node.
	 * @return The {@link Collection} of components running on this instance. 
	 */
	public Collection<ComponentInfo> getComponents();
	
	/**
	 * Returns whether a component with componentId and version is available.
	 * If version is null then version is ignored.
	 * @param componentId	The queried component identifier.
	 * @param version		The queried version 
	 * @return The component instance found, or null.
	 */
	public ComponentInfo hasComponent(String componentId, String version);
}
