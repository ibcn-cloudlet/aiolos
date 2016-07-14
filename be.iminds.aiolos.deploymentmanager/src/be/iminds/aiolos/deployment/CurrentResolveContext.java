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
package be.iminds.aiolos.deployment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wiring;
import org.osgi.service.log.LogService;
import org.osgi.service.repository.Repository;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolveContext;

import be.iminds.aiolos.resource.CapabilityRequirementImpl;

/**
 * {@link ResolveContext} used to resolve bundles.
 */
public class CurrentResolveContext extends ResolveContext {

	private BundleContext context;
	private Collection<Resource> mandatoryResources; 
	
	public CurrentResolveContext(BundleContext context, Requirement r) {
		this.context = context;
		
		List<Capability> found = findProviders(r);
		if(!found.isEmpty()){
			Resource resource = found.iterator().next().getResource();
			this.mandatoryResources = Collections.singleton(resource);
		} else {
			// TODO exception??
			mandatoryResources = Collections.emptyList();
		}
		
	}
	
	public Collection<Resource> getMandatoryResources(){
		return Collections.unmodifiableCollection(mandatoryResources);
	}

	@Override
	public List<Capability> findProviders(Requirement requirement) {
		// TODO find all providers (= Repositories)
		List<Capability> capabilities = new ArrayList<Capability>();
		try {
			// First add the current framework's matching capabilities
			for(Bundle b : context.getBundles()){
				BundleRevision rev = b.adapt(BundleRevision.class);
				if(rev==null)
					continue; // handle this, this can happen in Concierge (should be fixed there?)
				try {
					String filterStr = requirement.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
					Filter filter = filterStr != null ? FrameworkUtil.createFilter(filterStr) : null;

					for (Capability cap : rev.getCapabilities(null)) {
						boolean match;
						if (filter == null){
							match = true;
						} else {
							match = filter.matches(cap.getAttributes());
						}
						if (match){
							capabilities.add(cap);
						}
					}
				}catch (InvalidSyntaxException e) {
				}
			}
			
			// Then search capabilities in the available Repositories
			// TODO give priority to some of the providers???
			Collection<ServiceReference<Repository>> refs = context.getServiceReferences(
					Repository.class, null);
		
			for (ServiceReference<?> ref : refs) {
				Repository repo = (Repository) context.getService(ref);
				Collection<Capability> found = repo.findProviders(
						Collections.singleton(requirement)).values().iterator().next();
				for (Capability c : found) {
					// TODO additional matching needed? 
					capabilities.add(c);
				}

				context.ungetService(ref);
			}
			

		} catch (Exception e) {
			Activator.logger.log(LogService.LOG_ERROR, "Error in ResolveContext", e);
		}
		
		// TODO
		// THIS IGNORES osgi.native REQUIREMENTS ... 
		// THESE SHOULD BE SET BY THE OSGI RUNTIME, BUT IS ONLY REQUIRED FROM R6 ON
		// IGNORE FOR NOW?!
		if(requirement.getNamespace().equals("osgi.native")){
			capabilities.add(new CapabilityRequirementImpl("osgi.native", new Resource() {
				@Override
				public List<Requirement> getRequirements(String namespace) {
					return Collections.EMPTY_LIST;
				}
				@Override
				public List<Capability> getCapabilities(String namespace) {
					ArrayList<Capability> caps = new ArrayList<Capability>();
					caps.add(new CapabilityRequirementImpl("osgi.native", this));
					return caps;
				}
			}));
		}
		
		return capabilities;
	}

	@Override
	public int insertHostedCapability(List<Capability> capabilities,
			HostedCapability hostedCapability) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isEffective(Requirement requirement) {
		String e = requirement.getDirectives().get( "effective" );
		return e==null || "resolve".equals( e );
	}

	@Override
	public Map<Resource, Wiring> getWirings() {
		Map<Resource, Wiring> currentWiring = new HashMap<Resource, Wiring>();
		for(Bundle b : context.getBundles()){
			currentWiring.put(b.adapt(BundleRevision.class), b.adapt(BundleWiring.class));
		}
		return currentWiring;
	}

}
