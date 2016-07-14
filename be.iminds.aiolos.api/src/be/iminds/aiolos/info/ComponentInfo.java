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
 * Uniquely represents one component instance running on the AIOLOS platform
 * 
 * A component instance is defined by
 * - the identifier of the component
 * - the version of the component
 * - the identifier of the node where the component is instantiated
 */
public class ComponentInfo {

	private final String componentId;
	private final String version;
	private final String nodeId;
	private final String name;

	public ComponentInfo(String componentId, String version, 
			String nodeId, String name){
		this.componentId = componentId;
		this.version = version;
		this.nodeId = nodeId;
		this.name = name;
	}

	public ComponentInfo(String componentId, String version, 
			String nodeId){
		this.componentId = componentId;
		this.version = version;
		this.nodeId = nodeId;
		this.name = componentId; // use componentId as name if not provided
	}
	
	/**
	 * @return the unique identifier of the component
	 */
	public String getComponentId() {
		return componentId;
	}

	/**
	 * @return the version string of the component instance
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * @return the ID of the node where the component is instantiated
	 */
	public String getNodeId() {
		return nodeId;
	}
	
	/**
	 * @return a human readable name of the component
	 */
	public String getName(){
		return name;
	}
	
	public boolean equals(Object other){
		if(!(other instanceof ComponentInfo))
			return false;
		
		ComponentInfo c = (ComponentInfo) other;
		return( c.componentId.equals(componentId)
				&& c.version.equals(version)
				&& c.nodeId.equals(nodeId));
	}
	
	public int hashCode(){
		return componentId.hashCode()+version.hashCode()+nodeId.hashCode();
	}
	
	public String toString(){
		return componentId+"-"+version+"@"+nodeId;
	}

}
