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

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.service.command.CommandProcessor;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import be.iminds.aiolos.deployment.api.DeploymentManager;
import be.iminds.aiolos.deployment.command.DeploymentCommands;
import be.iminds.aiolos.util.log.Logger;

/**
 * The {@link BundleActivator} of the DeploymentManager bundle. 
 */
public class Activator implements BundleActivator {

	public static Logger logger;
	
	@Override
	public void start(final BundleContext context) throws Exception {
		logger = new Logger(context);
		logger.open();
		final DeploymentManagerImpl deploymentManager = new DeploymentManagerImpl(context);

		Hashtable<String, Object> properties = new Hashtable<String, Object>();
		properties.put("service.exported.interfaces", new String[] { DeploymentManager.class.getName() });

		context.addBundleListener(deploymentManager);
		ServiceRegistration<DeploymentManager> reg = context.registerService(DeploymentManager.class, deploymentManager, properties);
		logger.setServiceReference(reg.getReference());

		// GoGo Shell
		// add shell commands (try-catch in case no shell available)
		DeploymentCommands commands = new DeploymentCommands(deploymentManager);
		Dictionary<String, Object> commandProps = new Hashtable<String, Object>();
		try {
			commandProps.put(CommandProcessor.COMMAND_SCOPE, "component");
			commandProps.put(CommandProcessor.COMMAND_FUNCTION, new String[] {"start","stop","list"});
			context.registerService(Object.class, commands, commandProps);
		} catch (Throwable t) {
			// ignore exception, in that case no GoGo shell available
		}
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		logger.close();
	}
}
