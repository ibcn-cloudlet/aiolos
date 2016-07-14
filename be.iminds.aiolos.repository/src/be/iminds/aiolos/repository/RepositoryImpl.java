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
package be.iminds.aiolos.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;

/**
 * Implementation of the {@link Repository} interface.
 * 
 * Maintains an index of the resources available in the repository.
 */
public class RepositoryImpl implements Repository {

	private final String name;
	private final String indexURL;
	// keep capabilities mapped by namespace
	private final Map<String, List<Capability> > capabilities;
	
	// TODO should one be able to add/remove capabilities at runtime?
	public RepositoryImpl(String name, String indexURL,
			List<Resource> resources){
		this.name = name;
		this.indexURL = indexURL;
		this.capabilities = new HashMap<String, List<Capability>>();
		for(Resource r : resources){
			for(Capability c : r.getCapabilities(null)){
				List<Capability> caps = capabilities.get(c.getNamespace());
				if(caps==null){
					caps = new ArrayList<Capability>();
					capabilities.put(c.getNamespace(), caps);
				}
				caps.add(c);
			}
		}
	}
	
	public String getName(){
		return name;
	}
	
	public String getIndexURL(){
		return indexURL;
	}
	
	@Override
	public Map<Requirement, Collection<Capability>> findProviders(
			Collection<? extends Requirement> requirements) {
		
		Map<Requirement,Collection<Capability>> result = new HashMap<Requirement,Collection<Capability>>();
		for(Requirement requirement : requirements){
			Collection<Capability> matches = calculateMatches(requirement);
			result.put(requirement, matches);
		}
		
		return result;
	}

	private Collection<Capability> calculateMatches(Requirement requirement){
		List<Capability> matches = new ArrayList<Capability>();
		
		List<Capability> caps = capabilities.get(requirement.getNamespace());
		if (caps != null && !caps.isEmpty()){
		
			try {
				String filterStr = requirement.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
				Filter filter = filterStr != null ? FrameworkUtil.createFilter(filterStr) : null;

				for (Capability cap : caps) {
					boolean match;
					if (filter == null){
						match = true;
					} else {
						match = filter.matches(cap.getAttributes());
					}

				if (match)
					matches.add(cap);
				}
			}catch (InvalidSyntaxException e) {
			}
			
		}
		return matches;
	}
	
	// for the list() command
	public List<Capability> listCapabilities(String namespace){
		List<Capability> result = new ArrayList<Capability>();
		if(namespace!=null){
			result.addAll(capabilities.get(namespace));
		} else {
			for(String n : capabilities.keySet()){
				result.addAll(capabilities.get(n));
			}
		}
		return result;
	}
}
