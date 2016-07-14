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
package be.iminds.aiolos.resource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

/**
 * Implementation of the {@link Capability} and {@link Requirement} interfaces.
 * 
 * This package is exported in order to allow remote method calls to the Repository which take
 * {@link Capability}s and {@link Requirement}s as arguments.
 */
public class CapabilityRequirementImpl implements Capability, Requirement {

	private final String namespace;
	private final Map<String, String> directives = new HashMap<String, String>();
	private final Map<String, Object> attributes = new HashMap<String, Object>();
	private final Resource resource;
	
	public CapabilityRequirementImpl(String namespace, Resource resource){
		this.namespace = namespace;
		this.resource = resource;
	}
	
	public void addDirective(String key, String directive){
		this.directives.put(key, directive);
	}
	
	public void addAttribute(String key, Object attribute){
		this.attributes.put(key, attribute);
	}
	
	@Override
	public String getNamespace() {
		return namespace;
	}

	@Override
	public Map<String, String> getDirectives() {
		return Collections.unmodifiableMap(directives);
	}

	@Override
	public Map<String, Object> getAttributes() {
		return Collections.unmodifiableMap(attributes);
	}

	@Override
	public Resource getResource() {
		return resource;
	}

	@Override 
	public String toString(){
		String result = namespace+"\n";
		for(String key : attributes.keySet()){
			result+= key+" : "+attributes.get(key)+"\n";
		}
		for(String key : directives.keySet()){
			result+= key+" : "+directives.get(key)+"\n";
		}
		return result;
	}

	@Override
	public int hashCode() {
		int hashCode = 0;
		hashCode+=namespace.hashCode();
		for(String key : directives.keySet()){
			hashCode+=key.hashCode();
			hashCode+=directives.get(key).hashCode();
		}
		for(String key : attributes.keySet()){
			hashCode+=key.hashCode();
			hashCode+=attributes.get(key).hashCode();
		}
		if(resource!=null)  // can be null in case of a stated requirement
			hashCode+=resource.hashCode();
		return hashCode;
	}

	@Override
	public boolean equals(Object other) {
		if(other instanceof Capability){
			Capability cap = (Capability) other;
			if(!cap.getNamespace().equals(namespace)){
				return false;
			}
			if(!cap.getResource().equals(resource)){
				return false;
			}
			for(String key : cap.getAttributes().keySet()){
				if(!attributes.containsKey(key)){
					return false;
				}
				Object attr = cap.getAttributes().get(key);
				if(!attributes.get(key).equals(attr)){
					return false;
				}
			}
			for(String key : cap.getDirectives().keySet()){
				if(!directives.containsKey(key)){
					return false;
				}
				String dir = cap.getDirectives().get(key);
				if(!directives.get(key).equals(dir)){
					return false;
				}
			}
			return true;
		}
		if(other instanceof Requirement){
			Requirement req = (Requirement) other;
			if(!req.getNamespace().equals(namespace)){
				return false;
			}
			if(!req.getResource().equals(resource)){
				return false;
			}
			for(String key : req.getAttributes().keySet()){
				if(!attributes.containsKey(key)){
					return false;
				}
				Object attr = req.getAttributes().get(key);
				if(!attributes.get(key).equals(attr)){
					return false;
				}
			}
			for(String key : req.getDirectives().keySet()){
				if(!directives.containsKey(key)){
					return false;
				}
				String dir = req.getDirectives().get(key);
				if(!directives.get(key).equals(dir)){
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
	
}
