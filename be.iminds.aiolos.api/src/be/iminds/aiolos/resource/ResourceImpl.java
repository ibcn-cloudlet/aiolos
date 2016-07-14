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

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Version;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;
import org.osgi.service.repository.RepositoryContent;

/**
 * Implementation of the {@link Resource} interface.
 * 
 * This package is exported in order to allow remote method calls to the {@link Repository} which take
 * {@link Resource}s as return values.
 */
public class ResourceImpl implements Resource, RepositoryContent {

	private final String repoIndex;
	
	// capabilities and requirements of this resource (mapped by namespace)
	private final Map<String, List<Capability>> capabilities = new HashMap<String, List<Capability>>();
	private final Map<String, List<Requirement>> requirements = new HashMap<String, List<Requirement>>();
	
	public ResourceImpl(String repoIndex){
		this.repoIndex = repoIndex;
	}

	public void addCapability(Capability c){
		List<Capability> caps = capabilities.get(c.getNamespace());
		if(caps==null){
			caps = new ArrayList<Capability>();
			capabilities.put(c.getNamespace(), caps);
		}
		caps.add(c);
	}
	
	public void addRequirement(Requirement r){
		List<Requirement> reqs = requirements.get(r.getNamespace());
		if(reqs==null){
			reqs = new ArrayList<Requirement>();
			requirements.put(r.getNamespace(), reqs);
		}
		reqs.add(r);
	}
	
	@Override
	public InputStream getContent() {
		InputStream stream = null;
		
		List<Capability> caps = capabilities.get("osgi.content");
		if(caps==null || caps.isEmpty()){
			return stream;
		}
		
		Capability c = caps.get(0);
		String relativeLocation = (String) c.getAttributes().get("url");
		String absoluteLocation = repoIndex.substring(0, repoIndex.lastIndexOf("/")+1)+relativeLocation;
		
		try {
			URL url = new URL(absoluteLocation);
			stream =  url.openStream();
		} catch(Exception e){
			// error opening url
		}
		return stream;
	}

	@Override
	public List<Capability> getCapabilities(String namespace) {
		List<Capability> result = new ArrayList<Capability>();
		if(namespace==null){
			for(List<Capability> caps : capabilities.values()){
				result.addAll(caps);
			}	
		} else if(capabilities.containsKey(namespace)){
			result.addAll(capabilities.get(namespace));
		} 
		return Collections.unmodifiableList(result);
	}

	@Override
	public List<Requirement> getRequirements(String namespace) {
		List<Requirement> result = new ArrayList<Requirement>();
		if(namespace==null){
			// include all
			for(List<Requirement> reqs : requirements.values()){
				result.addAll(reqs);
			}
		} else if(requirements.containsKey(namespace)){
			result.addAll(requirements.get(namespace));
		} 
		return Collections.unmodifiableList(result);
	}
	
	@Override
	public String toString(){
		if(capabilities==null) {
			return super.toString(); // could happen when logging serialization lib 
		}
		
		List<Capability> caps = capabilities.get("osgi.content");
		if(caps==null || caps.isEmpty()){
			return super.toString();
		}
		
		Capability c = caps.get(0);
		return (String) c.getAttributes().get("url");
	}

	@Override
	public boolean equals(Object other){
		if(other instanceof Resource){
			// TODO this makes assumption that resource always has one osgi.identity capability ...
			// is this incorrect?
			List<Capability> caps = this.getCapabilities("osgi.identity");
			if(caps==null || caps.isEmpty()){
				return false;
			}
			Capability c1 = caps.get(0);
			String name1 = (String) c1.getAttributes().get("osgi.identity");
			Version v1 = (Version) c1.getAttributes().get("version");
			
			List<Capability> otherCaps = ((Resource) other).getCapabilities("osgi.identity");
			if(otherCaps==null || otherCaps.isEmpty()){
				return false;
			}
			Capability c2 = otherCaps.get(0);
			String name2 = (String) c2.getAttributes().get("osgi.identity");
			Version v2 = (Version) c2.getAttributes().get("version");
		
			if(name1.equals(name2)){
				// same name, now check version
				if(v1.equals(v2)){
					return true;
				}
			}

		}
		return false;
	}

	@Override
	public int hashCode() {
		if(capabilities==null) {
			return super.hashCode(); // could happen when logging serialization lib 
		}
		
		List<Capability> caps = this.getCapabilities("osgi.identity");
		if(caps==null || caps.isEmpty()){
			return super.hashCode();
		}
		Capability cap = caps.get(0);
		String name = (String) cap.getAttributes().get("osgi.identity");
		Version v = (Version) cap.getAttributes().get("version");
		
		String hashString = name+"-"+v.toString();
		return hashString.hashCode();
	}
	
	
}
