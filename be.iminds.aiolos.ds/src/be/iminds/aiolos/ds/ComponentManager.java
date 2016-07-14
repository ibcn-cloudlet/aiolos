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
package be.iminds.aiolos.ds;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentConstants;

import be.iminds.aiolos.ds.description.ComponentDescription;


public class ComponentManager {

	private List<Component> components = new ArrayList<Component>();
	
	public void registerComponent(Bundle bundle, ComponentDescription description){
		Component component;
		try {
			synchronized(components){
				long id = components.size();
				component = new Component(id, description, bundle);
				components.add(component);
			}
		} catch(Exception e){
			System.err.println("Error initializing component "+description.getName());
			e.printStackTrace();
		}
		
		
	}

	public void unregisterComponents(Bundle bundle){
		// TODO should we keep a map of components per bundle?
		synchronized(components){
			for(Component c : components){
				if(c.getBundle()==bundle){
					c.deactivate(ComponentConstants.DEACTIVATION_REASON_BUNDLE_STOPPED);
				}
			}
			
		}
	}
	
	
}
