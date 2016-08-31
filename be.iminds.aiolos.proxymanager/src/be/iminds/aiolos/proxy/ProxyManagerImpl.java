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
package be.iminds.aiolos.proxy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.framework.hooks.service.EventListenerHook;
import org.osgi.framework.hooks.service.FindHook;
import org.osgi.framework.hooks.service.ListenerHook.ListenerInfo;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

import be.iminds.aiolos.info.ComponentInfo;
import be.iminds.aiolos.info.ServiceInfo;
import be.iminds.aiolos.proxy.api.ProxyInfo;
import be.iminds.aiolos.proxy.api.ProxyManager;
import be.iminds.aiolos.proxy.api.ProxyPolicy;
import be.iminds.aiolos.proxy.api.ServiceProxyListener;

/**
 * Implementation of the {@link ProxyManager} interface, 
 * is responsible for creating and managing proxies of OSGi services.
 * 
 * Service registrations are captured by implementing the {@link EventListenerHook}, 
 * which allows to transparently register a proxy of all service interfaces.
 * 
 * Using the {@link FindHook} the original service instances are hidden for service lookup 
 * and only the proxies are visible.
 */
public class ProxyManagerImpl implements FindHook, EventListenerHook, ProxyManager {
	
	// Service property that is set when the service reference is from a service proxy
	public final static String IS_PROXY = "aiolos.isproxy";
	// Service property to know the component that hosts the service (also for imported services) - by default the symbolicName
	public final static String COMPONENT_ID = "aiolos.component.id";
	// Service property to know the version of the component that hosts the service (also for imported services) - by default the bundle version
	public final static String VERSION = "aiolos.component.version";
	// Service property identifying the service interface - by default the interface name, but can be differentiated further with the instance-id
	public final static String SERVICE_ID = "aiolos.service.id";
	// Service property that is set on exported services, identifying the framework uuid that exported the service
	public final static String FRAMEWORK_UUID = "aiolos.framework.uuid";
	// Extra service property to be set to be unique across multiple instances of the same service interface
	public final static String INSTANCE_ID = "aiolos.instance.id";
	// Extra service property to be set callback interfaces that should be uniquely proxied
	public final static String CALLBACK = "aiolos.callback";
	// Same as callback, but indicates interfaces that should be treated as unique service
	public final static String UNIQUE = "aiolos.unique";
	// Extra service property to select a number of interfaces that should be treated as one (e.g. interface hierarchy)
	public final static String COMBINE = "aiolos.combine";
	// Extra service property to select a subset of interfaces to export, or put to false for no exports
	public final static String EXPORT = "aiolos.export";
	// Extra service property to select a subset of interfaces to proxy, or put to false for no proxy
	public final static String PROXY = "aiolos.proxy";
	
	private final BundleContext context;

	// Keep all proxies here, mapped by ( ComponentId-Version --> ( ServiceId -->  ServiceProxy ) )
	private final Map<String, Map<String, ServiceProxy>> proxies = Collections.synchronizedMap(new HashMap<String, Map<String,ServiceProxy>>());
	
	// Keep all Service instances that are registered here
	private final Set<ServiceInfo> services = Collections.synchronizedSet(new HashSet<ServiceInfo>());
	
	// Separate map for matching serviceReferences to generated instanceIds for callback services
	private final Map<ServiceReference, String> uniqueInstanceIds = Collections.synchronizedMap(new HashMap<ServiceReference, String>());
	
	// Map with re-registered ignored interfaces that are 
	private final Map<ServiceReference, ServiceRegistration> ignoredRegistrations = Collections.synchronizedMap(new HashMap<ServiceReference, ServiceRegistration>());
	
	// Filters to decide by default which interfaces should be proxied and exported
	private String[] exportFilters = new String[]{"*"}; // export all
	private String[] ignoreExportFilters = new String[]{"org.osgi.service.event.EventHandler"}; // don't export EventHandler, use EventBroker to distribute Events
	private String[] proxyFilters = new String[]{"*","org.osgi.service.event.EventHandler"}; // proxy all and also proxy EventHandler
	private String[] ignoreProxyFilters = new String[]{  // except these
			"org.osgi.service.*",
			"org.osgi.framework.hooks.*",
			"org.apache.felix.*",
			"java.lang.Object",
			"org.apache.felix.shell.Command",
			"be.iminds.aiolos.*",
			"aQute.launcher.*",
			"java.lang.Runnable",
			"com.esotericsoftware.kryo.Serializer"};
	private String[] uniqueFilters = new String[]{"org.osgi.service.event.EventHandler"}; // service interfaces that should be treated as unique (= new proxy for each instance)
	
	public ProxyManagerImpl(BundleContext context){
		this.context = context;
		
		// configure default export/proxy behavior
		Object property = context.getProperty("aiolos.proxy");
		if(property!=null){
			proxyFilters = ((String)property).split(",");
		}
		property = context.getProperty("aiolos.proxy.extra");
		if(property!=null){
			String[] proxyExtra = ((String)property).split(",");
			proxyFilters = merge(proxyFilters, proxyExtra);
 		}

		property = context.getProperty("aiolos.proxy.ignore");
		if(property!=null){
			ignoreProxyFilters = ((String)property).split(",");
		}
		property = context.getProperty("aiolos.proxy.ignore.extra");
		if(property!=null){
			String[] ignoreProxyExtra = ((String)property).split(",");
			ignoreProxyFilters = merge(ignoreProxyFilters, ignoreProxyExtra);
 		}
		
		property = context.getProperty("aiolos.export");
		if(property!=null){
			exportFilters = ((String)property).split(",");
		}
		property = context.getProperty("aiolos.export.extra");
		if(property!=null){
			String[] exportExtra = ((String)property).split(",");
			exportFilters = merge(exportFilters, exportExtra);
 		}
		
		property = context.getProperty("aiolos.export.ignore");
		if(property!=null){
			ignoreExportFilters = ((String)property).split(",");
		}
		property = context.getProperty("aiolos.export.ignore.extra");
		if(property!=null){
			String[] ignoreExportExtra = ((String)property).split(",");
			ignoreExportFilters = merge(ignoreExportFilters, ignoreExportExtra);
 		}
		
		property = context.getProperty("aiolos.unique");
		if(property!=null){
			uniqueFilters = ((String)property).split(",");
		}
		property = context.getProperty("aiolos.unique.extra");
		if(property!=null){
			String[] uniqueFiltersExtra = ((String)property).split(",");
			uniqueFilters = merge(uniqueFilters, uniqueFiltersExtra);
 		}
	}

	@Override
	public void event(ServiceEvent event,
			Map<BundleContext, Collection<ListenerInfo>> listeners) {

		ServiceReference<?> serviceReference = event.getServiceReference();
		List<String> allInterfaces = Arrays.asList((String[])serviceReference.getProperty(Constants.OBJECTCLASS));
		
		// check which interfaces to proxy
		// aiolos.proxy property on service overrides system-wide properties
		Set<String> interfaces = new HashSet<String>();
		Object interfacesToProxy = serviceReference.getProperty(PROXY);
		if(interfacesToProxy instanceof String[]){
			for(String s : (String[])interfacesToProxy){
				interfaces.add(s);
			}
		} else if(interfacesToProxy instanceof String){
			if(interfacesToProxy.equals("false")){
				// don't proxy at all
				return;
			} else {
				interfaces.add((String)interfacesToProxy);
			}
		} else {
			// use defaults
			interfaces.addAll(allInterfaces);
			filterInterfaces(interfaces);
			
			if(interfaces.size()==0)
				return;
		}
		
		// special treatment required for ignored interfaces, if not they will 
		// also be hidden in the find() method
		List<String> ignoredInterfaces = new ArrayList<String>();
		for(String i : allInterfaces){
			if(!interfaces.contains(i)){
				ignoredInterfaces.add(i);
			}
		}
		
		String componentId =  (String)serviceReference.getProperty(COMPONENT_ID);
		// use component symbolic name by default
		if(componentId==null)	
			componentId = serviceReference.getBundle().getSymbolicName();
		
		String version =  (String)serviceReference.getProperty(VERSION);
		// use component symbolic name by default
		if(version==null){
			version = parseVersion(serviceReference.getBundle().getVersion());
		}
		
		// possibly identify unique instances of service interfaces
		String instanceId = (String)serviceReference.getProperty(INSTANCE_ID);
		
		// ID of the framework this services is instantiated
		String nodeId = (String) serviceReference.getProperty(RemoteConstants.ENDPOINT_FRAMEWORK_UUID);
		if(nodeId==null)
			nodeId = context.getProperty(Constants.FRAMEWORK_UUID);
		
		// Create ComponentInfo
		ComponentInfo component = new ComponentInfo(componentId, version, nodeId);
			
		Object combine = serviceReference.getProperty(COMBINE);
		if(combine!=null)
			combineInterfaces(interfaces, combine);
	
		
		// service is not yet proxied
		// or service is an imported service (and proxy flag is thus set by remote instance)
		if((serviceReference.getProperty(IS_PROXY) == null)
				|| (serviceReference.getProperty("service.imported")!=null)){
			Map<String, ServiceProxy> p = getProxiesOfComponent(componentId, version);
			
			// create a serviceproxy for each interface
			for(String i : interfaces){

				String iid = instanceId;
				// check if interface is set as unique if no instanceId set
				if(iid==null){
					boolean unique = false;
					// check runtime-wide unique property
					if(longestPrefixMatch(i, uniqueFilters) > 0){
						unique = true;
					} else {
						// check service properties
						Object uniques = serviceReference.getProperty(CALLBACK);
						if(uniques==null){
							uniques = serviceReference.getProperty(UNIQUE);
						}
						if(uniques instanceof String[]){
							for(String c : (String[])uniques){
								if(c.equals(i))
									unique = true;
							}
						} else if(uniques instanceof String){
							if(uniques.equals(i) || uniques.equals("*")|| uniques.equals("true"))
								unique = true;
						} else if(uniques instanceof Boolean){
							unique = ((Boolean) uniques).booleanValue();
						}
					}
					
					if(unique) {
						iid = uniqueInstanceIds.get(serviceReference);
						if(iid==null){
							iid = UUID.randomUUID().toString();
							uniqueInstanceIds.put(serviceReference, iid);
						}
					}
					
				}
				

				// Check if ID was customly set
				String serviceId = (String)serviceReference.getProperty(SERVICE_ID);
				if(serviceId==null){
					// use interface name by default
					serviceId = i;
					if(iid!=null)
						serviceId+="-"+iid;
				}

				ServiceProxy proxy = p.get(serviceId);
				switch(event.getType()){
				case ServiceEvent.REGISTERED:
					// create proxy or add a reference to extra instance
					if(proxy==null){
						// add new proxy
						boolean export = false;
						Object exports = serviceReference.getProperty(ProxyManagerImpl.EXPORT);
						if(exports instanceof String[]){
							for(String e : (String[]) exports){
								if(e.equals(i)){
									export = true;
								}
							}
						} else if(exports instanceof String){
							String e = (String) exports;
							if(e.equals(i) || e.equals("*")){
								export = true;
							} else {
								export = Boolean.parseBoolean(e);
							}
						} else if(exports instanceof Boolean){
							export = ((Boolean) exports).booleanValue();
						} else {
							// check default
							int ok = longestPrefixMatch(i, exportFilters);
							int ignore = longestPrefixMatch(i, ignoreExportFilters);
							if(ok>=ignore){
								export = true;
							}
						}
						proxy = new ServiceProxy(context, i, serviceId, componentId, version, serviceReference, export);
						p.put(serviceId, proxy);
					}
					try {
						// proxy the actual object implementing the service
						Object serviceObject = context.getService(serviceReference);
						
						ServiceInfo service = new ServiceInfo(serviceId, component);
						proxy.addInstance(service, serviceObject);
						
						// keep list of services
						if(component.getNodeId().equals(context.getProperty(Constants.FRAMEWORK_UUID))){
							services.add(service);
						}
						
						// succesfully proxied ... hide event for listeners
						listeners.clear();
					} catch(Exception e){
						Activator.logger.log(LogService.LOG_WARNING, "Failed to create proxy for "+componentId+" "+serviceId, e);
						// proxying failed ...
						p.remove(serviceId);
					}
					break;
				case ServiceEvent.UNREGISTERING:
					// remove reference to instance and remove proxy if no more instances left
					if(proxy!=null){
						ServiceInfo service = new ServiceInfo(serviceId, component);
						
						if(proxy.removeInstance(service)){
							p.remove(serviceId);
						}
						
						if(component.getNodeId().equals(context.getProperty(Constants.FRAMEWORK_UUID))){
							services.remove(service);
						}
						
						context.ungetService(serviceReference);
						
						// is a proxied service ... hide event for listeners
						listeners.clear();
					}
					break;
				case ServiceEvent.MODIFIED:
				case ServiceEvent.MODIFIED_ENDMATCH:
					// TODO how to handle modified services?
					if(proxy!=null){
						// is a proxied service ... hide event for listeners
						listeners.clear();
					}
					break;
				}
			}
			// reregister the service object in case of ignoredInterfaces, so that these are not ignored in find
			if(!ignoredInterfaces.isEmpty()){
				switch(event.getType()){
				case ServiceEvent.REGISTERED:
					// reregister this object with only the ignoredInterfaces
					Object serviceObject = context.getService(serviceReference);
					Dictionary<String, Object> properties = new Hashtable<String, Object>();
					for(String key : serviceReference.getPropertyKeys()){
						properties.put(key, serviceReference.getProperty(key));
					}
					properties.put(IS_PROXY, "ignored");
					String[] objectClass = new String[ignoredInterfaces.size()];
					ServiceRegistration r = context.registerService(ignoredInterfaces.toArray(objectClass), serviceObject, properties);
					ignoredRegistrations.put(serviceReference, r);
					break;
				case ServiceEvent.UNREGISTERING:
					// unregister our reregistration
					ServiceRegistration remove = ignoredRegistrations.remove(serviceReference);
					if(remove!=null){
						remove.unregister();
					}
					break;
				case ServiceEvent.MODIFIED:
				case ServiceEvent.MODIFIED_ENDMATCH:
					// modify our reregistration
					ServiceRegistration update = ignoredRegistrations.remove(serviceReference);
					if(update!=null){
						Dictionary<String, Object> newProperties = new Hashtable<String, Object>();
						for(String key : serviceReference.getPropertyKeys()){
							newProperties.put(key, serviceReference.getProperty(key));
						}
						newProperties.put(IS_PROXY, "ignored");
						update.setProperties(newProperties);
					}
					break;
				}
			}
			
			if(event.getType()==ServiceEvent.UNREGISTERING){
				uniqueInstanceIds.remove(serviceReference);
			}
		}
	}

	@Override
	public void find(BundleContext context, String name, String filter,
			boolean allServices, Collection<ServiceReference<?>> references) {
		Iterator<ServiceReference<?>> iterator = references.iterator();
		while (iterator.hasNext()) {
			ServiceReference<?> serviceReference = iterator.next();
	
			String componentId =  (String)serviceReference.getProperty(COMPONENT_ID);
			if(componentId==null)	
				componentId = serviceReference.getBundle().getSymbolicName();
			
			String version =  (String)serviceReference.getProperty(VERSION);
			if(version==null)	
				version = parseVersion(serviceReference.getBundle().getVersion());
			Map<String, ServiceProxy> p = getProxiesOfComponent(componentId, version);
			if(p!=null){	
				// In case multiple service interface present, just ignore if you find the first one proxied
				List<String> serviceInterfaces = new ArrayList<String>(Arrays.asList(((String[])serviceReference.getProperty(Constants.OBJECTCLASS))));
				
				Object combine = serviceReference.getProperty(COMBINE);
				if(combine!=null){
					combineInterfaces(serviceInterfaces, combine);
				}
				
				for(String serviceInterface : serviceInterfaces){
					String instanceId = (String)serviceReference.getProperty(INSTANCE_ID);
					if(serviceReference.getProperty(CALLBACK)!=null
							|| serviceReference.getProperty(UNIQUE)!=null){
						// TODO check here if it actually matches the serviceInterface
						instanceId = uniqueInstanceIds.get(serviceReference);
					}
					
					String serviceId = serviceInterface;
					if(instanceId!=null)
						serviceId+="-"+instanceId;
	
					if(p.containsKey(serviceId)){
						// This service is proxied, remove when it is not the service reference of the proxy
						if(serviceReference.getBundle().getBundleContext() != this.context){
							iterator.remove();
							break;
						}
					}
				}
			}
		}
	}
	
	void addServiceProxyListener(ServiceProxyListener l){
		synchronized(ServiceProxy.listeners){
			ServiceProxy.listeners.add(l);
		}
	}
	
	void removeServiceProxyListener(ServiceProxyListener l){
		synchronized(ServiceProxy.listeners){
			ServiceProxy.listeners.remove(l);
		}
	}
	
	protected void filterInterfaces(Collection<String> interfaces){
		// remove service interfaces that should not be proxied
		Iterator<String> it = interfaces.iterator();
		while(it.hasNext()){
			String i = it.next();
			
			// search longest prefix match
			int ok = longestPrefixMatch(i, proxyFilters);
			int ignore = longestPrefixMatch(i, ignoreProxyFilters);
			
			if(ignore>ok){
				it.remove();
			}
		}
	}
	
	protected int longestPrefixMatch(String i, String[] filters){
		int longestPrefix = -1;
		for(String filter : filters){
			int prefix = -1;
			if(filter.endsWith("*")){
				if(i.startsWith(filter.substring(0, filter.length()-1))){
					prefix = filter.split("\\.").length-1;
				}
			} else {
				if(i.equals(filter)){
					prefix = filter.split("\\.").length;
				}
			}
		
			if(prefix > longestPrefix){
				longestPrefix = prefix;
			}
		}
		return longestPrefix;
	}
	
	protected void combineInterfaces(Collection<String> interfaces, Object combine){
		if(combine instanceof String[]){
			// only combine those defined
			String combined = "";
			for(String i : ((String[])combine)){
				if(interfaces.remove(i)){
					combined+=i+",";
				}
			}
			combined = combined.substring(0, combined.length()-1);
			interfaces.add(combined);
		} else if(combine instanceof String && ((String)combine).equals("*")) {
			// combine all
			String combined = "";
			for(String i : interfaces){
				combined+=i+",";
			}
			combined = combined.substring(0, combined.length()-1);
			interfaces.clear();
			interfaces.add(combined);
		}
	}
	
	
	private Map<String, ServiceProxy> getProxiesOfComponent(String componentId, String version) {
		Map<String, ServiceProxy> p = proxies.get(componentId+"-"+version);
		if(p==null){
			p = new HashMap<String, ServiceProxy>();
			proxies.put(componentId+"-"+version, p);
		}
		return p;
	}

	@Override
	public Collection<ProxyInfo> getProxies() {
		List<ProxyInfo> proxyInfos = new ArrayList<ProxyInfo>();
		for(String componentId : proxies.keySet()){
			Map<String, ServiceProxy> proxyMap = proxies.get(componentId);
			for(String serviceId : proxyMap.keySet()){
				ServiceProxy proxy = proxyMap.get(serviceId);
				proxyInfos.add(proxy.getProxyInfo());
			}
		}
		return Collections.unmodifiableList(proxyInfos);
	}

	@Override
	public Collection<ProxyInfo> getProxies(ComponentInfo component) {
		List<ProxyInfo> proxyInfos = new ArrayList<ProxyInfo>();
		Map<String, ServiceProxy> proxyMap = getProxiesOfComponent(component.getComponentId(), component.getVersion());
		if(proxyMap!=null) {
			for(String serviceId : proxyMap.keySet()){
				ServiceProxy proxy = proxyMap.get(serviceId);
				proxyInfos.add(proxy.getProxyInfo());
			}
		}
		return Collections.unmodifiableCollection(proxyInfos);
	}

	@Override
	public void setProxyPolicy(ProxyInfo proxyInfo, ProxyPolicy policy) {
		Map<String, ServiceProxy> proxyMap = getProxiesOfComponent(proxyInfo.getComponentId(), proxyInfo.getVersion());
		if(proxyMap==null)
			return;
		
		ServiceProxy proxy = proxyMap.get(proxyInfo.getServiceId());
		if(proxy==null)
			return;
		
		proxy.setPolicy(policy);
	}

	@Override
	public Collection<ServiceInfo> getServices() {
		List<ServiceInfo> result = new ArrayList<ServiceInfo>(services.size());
		synchronized(services){
			result.addAll(services);
		}
		return Collections.unmodifiableList(result);
	}

	@Override
	public Collection<ServiceInfo> getServices(ComponentInfo component) {
		List<ServiceInfo> result = new ArrayList<ServiceInfo>();
		synchronized(services){
			for(ServiceInfo s : services){
				if(s.getComponent().equals(component))
					result.add(s);
			}
		}
		return Collections.unmodifiableList(result);
	}
	
	private String parseVersion(Version v){
		return v.getMajor()+"."+v.getMinor()+"."+v.getMicro();  // ignore qualifiers
	}
	
	private String[] merge(String[] a1, String[] a2){
		int start = a1.length;
		String[] r = Arrays.copyOf(a1, a1.length+a2.length);
		for(int i=start;i<r.length;i++){
			r[i] = a2[i-start];
		}
		return r;
	}
	
}
