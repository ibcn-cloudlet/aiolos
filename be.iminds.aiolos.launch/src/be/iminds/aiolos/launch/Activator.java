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
package be.iminds.aiolos.launch;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

import be.iminds.aiolos.cloud.api.CloudManager;
import be.iminds.aiolos.util.log.Logger;

public class Activator implements BundleActivator {

	public static Logger logger;
	private static final String BNDRUN_DEFAULT = "run-mgmt.bndrun";
	public static String bndrun = null;

	@Override
	public void start(BundleContext context) throws Exception {
		logger = new Logger(context);
		logger.open();
		String config = context.getProperty("aiolos.launch.config");
		String mode = context.getProperty("aiolos.launch.mode");
		Activator.logger.log(LogService.LOG_INFO, "Preparing AIOLOS CloudManager ...");
		bndrun = context.getProperty("aiolos.launch.bndrun");
		if (bndrun == null)
			bndrun = BNDRUN_DEFAULT;
		
		Filter f = null;
		if(config.equals("local")){
			f = context.createFilter("(&(objectClass="+CloudManager.class.getName()+")(aiolos.cloud.provider=local))");
		} else {
			f = context.createFilter("(&(objectClass="+CloudManager.class.getName()+")(aiolos.cloud.provider=jclouds))");
		}
		ServiceTracker<CloudManager, CloudManager> tracker = new ServiceTracker<CloudManager, CloudManager>(context, f, null);
		tracker.open();
		CloudManager cloudManager = tracker.waitForService(10000);
		if(cloudManager!=null){
			CloudLauncher launcher = new CloudLauncher(cloudManager);
		
			if(mode.equals("launch")){
				launcher.launch();
			} else if(mode.equals("kill")){
				launcher.kill();
			}
			
			tracker.close();
			
			if(mode.equals("kill") || !config.equals("local")){
				System.exit(0);
			}
		} else {
			Activator.logger.log(LogService.LOG_ERROR, "Launch failed - No CloudManager available");
			System.exit(-1);
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		logger.close();
	}
}
