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
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.Version;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.log.LogService;
import org.osgi.service.repository.RepositoryContent;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.ResolveContext;
import org.osgi.service.resolver.Resolver;

import be.iminds.aiolos.deployment.api.DeploymentException;
import be.iminds.aiolos.deployment.api.DeploymentManager;
import be.iminds.aiolos.info.ComponentInfo;

/**
 * The {@link DeploymentManagerImpl} is responsible for starting, stopping and migrating components.
 * Components (=bundles) are resolved using the OSGi Resolver specification
 * Bundles can be fetched from a repository, or from other {@link DeploymentManagerImpl}s
 */
public class DeploymentManagerImpl implements DeploymentManager, SynchronousBundleListener {

	private final BundleContext context;
	
	private final Map<ComponentInfo, Bundle> components = new HashMap<ComponentInfo ,Bundle>();
	
	public DeploymentManagerImpl(BundleContext context){
		this.context = context;
	}
	
	@Override
	public synchronized void bundleChanged(BundleEvent event) {
		Bundle bundle = event.getBundle();
		ComponentInfo component = getComponentInfo(bundle);
		
		// only keep application bundles, so ignore aiolos bundles
		// TODO this is rather dirty ...
		if(component==null || component.getComponentId().startsWith("be.iminds.aiolos"))
			return;
		
		switch(event.getType()){	
			case BundleEvent.STARTED:{
				Activator.logger.log(LogService.LOG_INFO, "Component "+component+" started.");
				components.put(component, bundle);
				break;
			}
			case BundleEvent.STOPPING:{
				Activator.logger.log(LogService.LOG_INFO, "Component "+component+" stopped.");
				components.remove(component);
				break;
			}
		}	
	}

	public synchronized ComponentInfo installPackage(String packageName) throws DeploymentException {
		return installPackage(packageName, null);
	}
	
	public synchronized ComponentInfo installPackage(String packageName, String version) throws DeploymentException {
		String componentId = null;
		String componentVersion = null;
		
		ServiceReference<Resolver> resolveRef = context.getServiceReference(Resolver.class);
		if(resolveRef==null){
			DeploymentException e = new DeploymentException("Cannot resolve package"+packageName+", no Resolver service available");
			Activator.logger.log(LogService.LOG_ERROR, e.getMessage(), e);
			throw e;
		}
			
		Resolver resolver = context.getService(resolveRef);
		try {
			Requirement req;
			if(version!=null){
				req = RequirementBuilder.buildPackageNameRequirement(packageName, version);
			} else {
				req = RequirementBuilder.buildPackageNameRequirement(packageName);
			}
			
			ResolveContext resolveContext = new CurrentResolveContext(context, req);
			Map<Resource, List<Wire>> resources = resolver.resolve(resolveContext);
			for(Resource r : resources.keySet()){
				componentId = (String) r.getCapabilities("osgi.identity").get(0).getAttributes().get("osgi.identity");
				componentVersion = r.getCapabilities("osgi.identity").get(0).getAttributes().get("version").toString();
				break;
			}
		} catch(ResolutionException e){
			DeploymentException ex = new DeploymentException("Error resolving package "+packageName);
			Activator.logger.log(LogService.LOG_ERROR, ex.getMessage(), e);
			for(Requirement r : e.getUnresolvedRequirements()){
				Activator.logger.log(LogService.LOG_ERROR, "Unresolved requirement "+r);
			}
			throw ex;
		} finally {
			context.ungetService(resolveRef);
		}
		
		if(componentId == null){
			// TODO this cannot happen?
			DeploymentException ex = new DeploymentException("Cannot resolve package "+packageName);
			Activator.logger.log(LogService.LOG_ERROR, ex.getMessage(), ex);
			throw ex;
		}

		return startComponent(componentId, componentVersion);
	}
	
	@Override
	public synchronized ComponentInfo startComponent(String componentId) throws DeploymentException {
		return startComponent(componentId, null);
	}
	
	@Override
	public synchronized ComponentInfo startComponent(String componentId, String version) throws DeploymentException {
		Activator.logger.log(LogService.LOG_INFO, "Starting component "+componentId);
		// check if already installed
		ComponentInfo c = new ComponentInfo(componentId, version, context.getProperty(Constants.FRAMEWORK_UUID));
		
		if(components.containsKey(componentId)){
			DeploymentException ex = new DeploymentException("Component "+componentId+" already installed...");
			Activator.logger.log(LogService.LOG_INFO, "Component "+componentId+" already installed...");
			throw ex;
		}

		// first resolve the component and its dependencies
		List<Bundle> toStart = new ArrayList<Bundle>();
		
		ServiceReference<Resolver> resolveRef = context.getServiceReference(Resolver.class);
		if(resolveRef==null){
			DeploymentException e = new DeploymentException("Cannot resolve "+componentId+", no Resolver service available");
			Activator.logger.log(LogService.LOG_ERROR, e.getMessage(), e);
			throw e;
		}

		Map<Resource, List<Wire>> resources = null;
		
		Resolver resolver = context.getService(resolveRef);
		try {
			Requirement req;
			if(version!=null){
				req = RequirementBuilder.buildComponentNameRequirement(componentId, version);
			} else {
				req = RequirementBuilder.buildComponentNameRequirement(componentId);
			}
			ResolveContext resolveContext = new CurrentResolveContext(context, req);
			if(resolveContext.getMandatoryResources().isEmpty()){
				DeploymentException e = new DeploymentException("Component "+componentId+" not found ...");
				Activator.logger.log(LogService.LOG_ERROR, e.getMessage(), e);
				throw e;
			}
			resources = resolver.resolve(resolveContext);
		} catch(ResolutionException e ){
			DeploymentException ex = new DeploymentException("Cannot resolve "+componentId+"; "+e.getMessage());
			Activator.logger.log(LogService.LOG_ERROR, ex.getMessage(), e);
			for(Requirement r : e.getUnresolvedRequirements()){
				Activator.logger.log(LogService.LOG_ERROR, "Unresolved requirement "+r);
			}
			throw ex;
		} finally {
			context.ungetService(resolveRef);
		}

		for(Resource r : resources.keySet()){
			if(r instanceof RepositoryContent){ // Could also be something else (i.e. Concierge Resource impl when bundle is running)
				RepositoryContent content = (RepositoryContent) r;	
				String location = (String) r.getCapabilities("osgi.content").get(0).getAttributes().get("url");
				try {
					Bundle b = context.installBundle(location, content.getContent());
					toStart.add(b);
				} catch(BundleException e){
					DeploymentException ex = new DeploymentException("Error installing component "+componentId);
					Activator.logger.log(LogService.LOG_ERROR, ex.getMessage(), e);
					throw ex;
				}
			} 
		}
		context.ungetService(resolveRef);
		
		// start bundle(s)
		for(Bundle b : toStart){
			try {
				if(!(b.getState()==Bundle.ACTIVE || b.getState()==Bundle.STARTING))
					b.start();
			}catch(BundleException e){
				DeploymentException ex = new DeploymentException("Error starting component "+componentId);
				Activator.logger.log(LogService.LOG_ERROR, ex.getMessage(), e);
				throw ex;
			}
			
			// set componentinfo here - version could be null but should be filled in here
			if(b.getSymbolicName().equals(componentId)){
				c = getComponentInfo(b);
			}
		}
		
		return c;
	}

	
	@Override
	public synchronized void stopComponent(ComponentInfo component) throws DeploymentException {		
		Activator.logger.log(LogService.LOG_INFO, "Stopping component "+component);

		if(component==null){
			// to cleanly stop the complete node... TODO should this function be moved to e.g. TopologyManager?
			Runnable shutdown = new Runnable(){
				public void run(){
					// stop topologymanager to assure clean shutdown synchronization
					// this makes sure that all endpoints are removed when this call returns
					try {
						ServiceReference<?> ref = context.getServiceReference("be.iminds.aiolos.topology.api.TopologyManager");
						if(ref!=null){
							Activator.logger.log(LogService.LOG_INFO, "Stopping bundle "+ref.getBundle().getSymbolicName());
							ref.getBundle().stop();
						}
					} catch(BundleException e){}

					// next stop framework?
					Bundle systemBundle = context.getBundle(0);
					try {
						systemBundle.stop();
					} catch (BundleException e) {
					}
				}
			};
			Thread t = new Thread(shutdown);
			t.start();
		} else if(!components.containsKey(component)){
			DeploymentException ex = new DeploymentException("Component "+component+" not present!");
			Activator.logger.log(LogService.LOG_ERROR, ex.getMessage(), ex);
			throw ex;
		} else {	
			// Stop (and uninstall) bundle
			Bundle b = components.get(component);

			try {
				b.stop();
				b.uninstall();
			} catch (BundleException e) {
				DeploymentException ex = new DeploymentException("Error stopping component "+component);
				Activator.logger.log(LogService.LOG_ERROR, ex.getMessage(), e);
				throw ex;
			}
		}
		
	}


	@Override
	public synchronized Collection<ComponentInfo> getComponents() {
		Collection<ComponentInfo> result = new ArrayList<ComponentInfo>();
		result.addAll(components.keySet());
		return Collections.unmodifiableCollection(result);
	}
	
	@Override
	public synchronized ComponentInfo hasComponent(String componentId, String version){
		for(ComponentInfo component : components.keySet()){
			if(component.getComponentId().equals(componentId)){
				if(version==null)
					return component;
				else if( component.getVersion().equals(version)){
					return component;
				}
			}
		}
		return null;
	}

	private ComponentInfo getComponentInfo(Bundle bundle){
		if(bundle.getBundleId() < context.getBundle().getBundleId())
			return null;
		
		final String componentId = bundle.getSymbolicName();
		Version v = bundle.getVersion();
		final String version = v.getMajor()+"."+v.getMinor()+"."+v.getMicro();
		final String nodeId = context.getProperty(Constants.FRAMEWORK_UUID);
		final String name = bundle.getHeaders().get("Bundle-Name");
		ComponentInfo c = new ComponentInfo(componentId, version, nodeId, name);
		return c;
	}
}

