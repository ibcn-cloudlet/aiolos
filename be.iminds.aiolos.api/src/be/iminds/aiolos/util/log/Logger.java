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
package be.iminds.aiolos.util.log;

import java.io.Closeable;
import java.io.IOException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

public class Logger implements Closeable {
	
	private ServiceReference<?> ref = null;
	private final ServiceTracker<LogService, LogService> logger;
	
	public Logger(BundleContext context){
		logger = new ServiceTracker<LogService,LogService>(context, LogService.class, null);
	}
	
	public void setServiceReference(ServiceReference<?> ref){
		this.ref = ref;
	}
	
	public synchronized void log(int level, String message, Throwable exception){
		LogService log  = logger.getService();
		if(log!=null)
			if (ref != null)
				log.log(ref, level, message, exception);
			else
				log.log(level, message, exception);
	}
	
	public void log(int level, String message){
		log(level, message, null);
	}

	public void open() {
		logger.open();
	}
	
	@Override
	public void close() throws IOException {
		logger.close();
	}
}
