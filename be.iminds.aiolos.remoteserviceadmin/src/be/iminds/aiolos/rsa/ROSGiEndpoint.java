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
package be.iminds.aiolos.rsa;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Capability;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

import be.iminds.aiolos.rsa.network.api.NetworkChannelFactory;
import be.iminds.aiolos.rsa.util.MethodSignature;

/**
 * Implementation of the R-OSGi endpoint
 * 
 * Keeps a map of Method objects hashed by signature to which method calls are dispatched
 * 
 * Also keeps a list of aqcuired references to this endpoint
 */
@SuppressWarnings({"unchecked"})
public class ROSGiEndpoint implements ExportReference {

	private String serviceId;
	private Object serviceObject;
	private ServiceReference<?> serviceReference;
	private Map<String, Method> methodList = new HashMap<String, Method>();
	
	private Map<String, Object> endpointDescriptionProperties;
	
	private NetworkChannelFactory factory;
	
	private int refCount = 0;

	public ROSGiEndpoint(BundleContext context, 
			ServiceReference<?> serviceReference, 
			Map<String, ?> properties,  
			String frameworkId, 
			NetworkChannelFactory factory){
		if(properties==null){
			properties = Collections.EMPTY_MAP;
		}
		
		// keep factory to fetch address for endpoint id
		this.factory = factory;
		
		// First get exported interfaces
		String[] exportedInterfaces = getExportedInterfaces(serviceReference, properties);
		if (exportedInterfaces == null)
			throw new IllegalArgumentException(
					RemoteConstants.SERVICE_EXPORTED_INTERFACES
							+ " not set"); 
		
		// Check if all exported interfaces are actually service interfaces
		if (!ckeckExportedInterfaces(serviceReference, exportedInterfaces))
			throw new IllegalArgumentException(
					RemoteConstants.SERVICE_EXPORTED_INTERFACES
					+ " invalid"); 

		// Get exported configs
		String[] exportedConfigs = toStringArray(getProperty(RemoteConstants.SERVICE_EXPORTED_CONFIGS,
				serviceReference, properties));
		
		boolean configSupported = false;
		if(exportedConfigs !=null){
			for(String config : exportedConfigs){
				if(config.equals(Config.CONFIG_ROSGI)){
					configSupported = true;
				}
			}
		} else {
			configSupported = true;
		}
		if(!configSupported){
			throw new IllegalArgumentException("Configurations not supported!");
		}
		
		// Get all intents (service.intents, service.exported.intents,
		// service.exported.intents.extra)
		String[] serviceIntents = getServiceIntents(serviceReference, properties);
		
		// We don't support any intents at this moment
		if(serviceIntents!=null){
			throw new IllegalArgumentException("Intent "+serviceIntents[0]+" not supported!");
		}
		
		// Keep service id and service object
		long id = (Long)serviceReference.getProperty("service.id");
		this.serviceId = ""+id;
		this.serviceObject = context.getService(serviceReference);
		this.serviceReference = serviceReference;
		
		// Create EndpointDescriptionProperties
		createExportEndpointDescriptionProperties(serviceReference, properties, exportedInterfaces, serviceIntents, frameworkId);
		
		// Cache list of methods in a Map, faster lookup then reflection?
		createMethodList(serviceObject, exportedInterfaces);

	}
	
	public int acquire(){
		return ++refCount;
	}
	
	public int release(){
		return --refCount;
	}
	
	public String getServiceId(){
		return serviceId;
	}
	
	public Method getMethod(String methodSignature){
		return methodList.get(methodSignature);
	}
	
	public Object getServiceObject(){
		return serviceObject;
	}

	@Override
	public ServiceReference<?> getExportedService() {
		return serviceReference;
	}

	@Override
	public EndpointDescription getExportedEndpoint() {
		// always re-fetch the address in order to mitigate runtime ip change
		String endpointId = "r-osgi://"+factory.getAddress()+"#"+serviceId;
		
		endpointDescriptionProperties.put(RemoteConstants.ENDPOINT_ID,
				endpointId);
		
		return new EndpointDescription(endpointDescriptionProperties);
	}
	
	
	private void createExportEndpointDescriptionProperties(
			ServiceReference<?> serviceReference,
			Map<String, ?> properties,
			String[] exportedInterfaces, 
			String[] serviceIntents, 
			String frameworkId) {
	
		endpointDescriptionProperties = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);

		endpointDescriptionProperties.put(Constants.OBJECTCLASS, exportedInterfaces);

		// endpointId is refreshed each time endpoint description is collected
		// this is to mitigate possible changing ip address

		Long serviceId = (Long) serviceReference
				.getProperty(Constants.SERVICE_ID);
		endpointDescriptionProperties.put(RemoteConstants.ENDPOINT_SERVICE_ID, serviceId);

		endpointDescriptionProperties.put(RemoteConstants.ENDPOINT_FRAMEWORK_UUID, frameworkId);

		String[] remoteConfigsSupported = new String[]{Config.CONFIG_ROSGI}; 
		endpointDescriptionProperties.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, remoteConfigsSupported);

		if (serviceIntents != null)
			endpointDescriptionProperties.put(RemoteConstants.SERVICE_INTENTS, serviceIntents);

		for(String key : properties.keySet()){
			if(!isIgnored(key)){
				endpointDescriptionProperties.put(key, properties.get(key));
			}
		}
		for(String key : serviceReference.getPropertyKeys()){
			if(!isIgnored(key)
				&&
				!endpointDescriptionProperties.containsKey(key)){
				endpointDescriptionProperties.put(key, serviceReference.getProperty(key));
			}
		}
		
		BundleWiring wiring = serviceReference.getBundle().adapt(BundleWiring.class);
		for(BundleWire wire : wiring.getRequiredWires(null)){
			Capability cap = wire.getCapability();
			
			if(cap.getNamespace().equals("osgi.wiring.package")){
				String packageName = (String) cap.getAttributes().get("osgi.wiring.package");
				Version version = (Version)cap.getAttributes().get("version");
				if(version!=null){
					endpointDescriptionProperties.put(RemoteConstants.ENDPOINT_PACKAGE_VERSION_+packageName, version.toString());
				} 
			}
		}
	}

	private void createMethodList(Object serviceObject, String[] exportedInterfaces){
		List<String> exportedInterfaceList = Arrays.asList(exportedInterfaces);
		for(Class<?> iface : getInterfaces(serviceObject.getClass())){
			if(exportedInterfaceList.contains(iface.getName())){
				for(Method m : iface.getMethods()){
					methodList.put(MethodSignature.getMethodSignature(m), m);
				}
			}
		}
	}
	
	private List<Class> getInterfaces(Class clazz){
		List<Class> ifaces = new ArrayList<Class>();
		for(Class<?> iface : clazz.getInterfaces()){
			ifaces.add(iface);
		}
		if(clazz.getSuperclass()!=null){
			ifaces.addAll(getInterfaces(clazz.getSuperclass()));
		}
		return ifaces;
	}

	private boolean ckeckExportedInterfaces(ServiceReference<?> serviceReference,
			String[] exportedInterfaces) {
		if (exportedInterfaces == null || exportedInterfaces.length == 0){
			return false;
		}
		List<String> objectClassList = Arrays.asList((String[]) serviceReference
											.getProperty(Constants.OBJECTCLASS));
		for (int i = 0; i < exportedInterfaces.length; i++){
			if (!objectClassList.contains(exportedInterfaces[i])){
				return false;
			}
		}
		return true;
	}
	
	private static String[] getExportedInterfaces(ServiceReference r, Map<String, ?> properties){
		String[] exportedInterfaces = toStringArray(
				getProperty(RemoteConstants.SERVICE_EXPORTED_INTERFACES, r, properties));
		if(exportedInterfaces==null){
			return null;
		}
		// if exportedInterfaces == {"*"}, use objectClass property
		if(exportedInterfaces.length==1 && exportedInterfaces[0].equals("*")){
			exportedInterfaces = (String[]) r.getProperty(org.osgi.framework.Constants.OBJECTCLASS);
		}
		return exportedInterfaces;
	}
	
	private static String[] getServiceIntents(ServiceReference r, Map<String, ?> properties){
		// merge service.intents, service.exported.intents and service.exported.intents.extra
		List<String> results = new ArrayList<String>();

		for(String key : new String[]{RemoteConstants.SERVICE_INTENTS,
			RemoteConstants.SERVICE_EXPORTED_INTENTS, 
			RemoteConstants.SERVICE_EXPORTED_INTENTS_EXTRA}){
			String[] intents = toStringArray(getProperty(
					key, r, properties));
			if(intents!=null){
				results.addAll(Arrays.asList(intents));
			}
		}

		if (results.size() == 0)
			return null;
		
		return (String[]) results.toArray(new String[results.size()]);
		
		
	}
	
	private static Object getProperty(String key, ServiceReference r, Map<String, ?> p){
		Object result = p.get(key);
		if(result==null){
			result = r.getProperty(key);
		}
		return result;
	}
	
	private static String[] toStringArray(Object o){
		if(o==null){
			return null;
		} else if(o instanceof String){
			return new String[]{(String)o};
		} else if(o instanceof String[]){
			return (String[]) o;
		} else if(o instanceof Collection){
			Collection c = (Collection) o;
			String[] r = new String[c.size()];
			int i = 0;
			for(Object s : c){
				r[i++] = (String)s;
			}
			return r;
		}
		return null;
	}
	
	private static boolean isIgnored(String key){
		// ignore private keys
		if(key.startsWith("."))
			return true;
		
		// ignore predefined keys
		for(String i : ignoredKeys){
			if(i.equalsIgnoreCase(key)){
				return true;
			}
		}
		
		return false;
	}
	
	private static final List<String> ignoredKeys = Arrays
			.asList(new String[] {
					Constants.OBJECTCLASS, 
					Constants.SERVICE_ID,
					RemoteConstants.ENDPOINT_FRAMEWORK_UUID,
					RemoteConstants.ENDPOINT_ID,
					RemoteConstants.ENDPOINT_SERVICE_ID,
					RemoteConstants.REMOTE_CONFIGS_SUPPORTED,
					RemoteConstants.REMOTE_INTENTS_SUPPORTED,
					RemoteConstants.SERVICE_EXPORTED_CONFIGS,
					RemoteConstants.SERVICE_EXPORTED_INTENTS,
					RemoteConstants.SERVICE_EXPORTED_INTENTS_EXTRA,
					RemoteConstants.SERVICE_EXPORTED_INTERFACES,
					RemoteConstants.SERVICE_IMPORTED,
					RemoteConstants.SERVICE_IMPORTED_CONFIGS,
					RemoteConstants.SERVICE_INTENTS });
}
