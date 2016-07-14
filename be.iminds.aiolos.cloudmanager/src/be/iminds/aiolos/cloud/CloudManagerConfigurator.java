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
/**
 * 
 */
package be.iminds.aiolos.cloud;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.log.LogService;

import be.iminds.aiolos.cloud.api.CloudManager;

/**
 * @author elias
 *
 */
public class CloudManagerConfigurator implements ManagedServiceFactory {
	
	private String PID;
	private String provider;
	private Map<String,AbstractCloudManager> managers = new HashMap<String,AbstractCloudManager>();
	private Map<String, ServiceRegistration<CloudManager>> services = new HashMap<String, ServiceRegistration<CloudManager>>();
	private BundleContext bundleContext;
	
	public CloudManagerConfigurator() {}
	
	public CloudManagerConfigurator(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
		String name = bundleContext.getBundle().getSymbolicName();
		provider = name.substring(name.lastIndexOf(".")+1);
		PID = "be.iminds.aiolos.cloud."+provider +".CloudManager";
		Activator.logger.log(LogService.LOG_DEBUG, "Configurator started (" + PID + ")");
	}
	
	@Override
	public String getName() {
		return PID;
	}

	@Override
	public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
		try {
			AbstractCloudManager manager = managers.get(pid);
			if (properties != null) {
				if (manager == null) {
					manager = createNewCloudManager(properties);
					
					Dictionary<String,Object> p = new Hashtable<String,Object>();
					p.put("aiolos.cloud.provider", provider);
					p.put("service.exported.interfaces", new String[] { CloudManager.class.getName() });

					ServiceRegistration<CloudManager> service = bundleContext.registerService(CloudManager.class, manager, p);
					managers.put(pid, manager);
					services.put(pid, service);
				} else {
					manager.configure(properties);
				}
		    	Activator.logger.log(LogService.LOG_DEBUG, "Configuration updated (" + pid + ")");
			}
		} catch (InstantiationException e) {
			Activator.logger.log(LogService.LOG_ERROR, "Unable to instantiate " + properties.get("class").toString(), e);
		} catch (IllegalAccessException e) {
			Activator.logger.log(LogService.LOG_ERROR, e.getLocalizedMessage(), e);
		} catch (ClassNotFoundException e) {
			Activator.logger.log(LogService.LOG_ERROR, "Class not found: " + properties.get("class").toString(), e);
		}
	}

	@Override
	public void deleted(String pid) {
		ServiceRegistration<CloudManager> service = services.get(pid);
		if (service != null) {
			service.unregister();
			services.remove(pid);
			managers.remove(pid);
			Activator.logger.log(LogService.LOG_DEBUG, "Configuration deleted (" + PID + ")");
		}
	}
	
	public AbstractCloudManager createNewCloudManager(Dictionary<String, ?> properties) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		String className = null;
		String provider = properties.get("provider").toString();
		if(provider.equals("local")){
			className = "be.iminds.aiolos.cloud.local.CloudManagerLocal";
		} else if (provider.equals("jclouds")){
			String cloud = properties.get("cloud").toString();
			if(cloud.equals("ec2")){
				className = "be.iminds.aiolos.cloud.jclouds.CloudManagerImplEC2";
			} else if(cloud.equals("openstack")){
				className = "be.iminds.aiolos.cloud.jclouds.CloudManagerImplOpenStack";
			} else {
				Activator.logger.log(LogService.LOG_DEBUG, "Cloud "+cloud+" not supported.");
			}
		} else {
			Activator.logger.log(LogService.LOG_DEBUG, "Cloud provider "+provider+" unknown.");
		}
		
		Class<?> clazz = this.getClass().getClassLoader().loadClass(className);
		AbstractCloudManager ocm = (AbstractCloudManager) clazz.newInstance();
		ocm.configure(properties);
		return ocm;
	}
}
