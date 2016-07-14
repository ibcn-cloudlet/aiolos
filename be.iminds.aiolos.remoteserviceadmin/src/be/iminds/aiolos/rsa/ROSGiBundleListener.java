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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ImportRegistration;

import be.iminds.aiolos.rsa.util.EndpointDescriptionParser;

/**
 * Listens to {@link BundleEvent}s, parses XML endpoint descriptions 
 * and imports endpoint descriptions provided by started bundles
 */
public class ROSGiBundleListener implements BundleListener {

	private final ROSGiServiceAdmin rsa;

	private final Map<Bundle, List<ImportRegistration>> importRegistrations;

	public ROSGiBundleListener(ROSGiServiceAdmin rsa) {
		this.rsa = rsa;
		this.importRegistrations = new HashMap<Bundle, List<ImportRegistration>>();
	}
	
	@Override
	public void bundleChanged(BundleEvent event) {
		final Bundle bundle = event.getBundle();
		switch (event.getType()) {
		case BundleEvent.STARTED: {
			List<EndpointDescription> endpointDescriptions = EndpointDescriptionParser
					.parseEndpointDescriptions(bundle);
			if (endpointDescriptions.size() > 0) {
				List<ImportRegistration> importRegistrationsList = new ArrayList<ImportRegistration>();
				for (EndpointDescription endpointDescription : endpointDescriptions) {
					ImportRegistration ir = rsa.importService(endpointDescription);
					importRegistrationsList.add(ir);
				}
				importRegistrations.put(bundle, importRegistrationsList);
			}
			break;
		}
		case BundleEvent.UNINSTALLED: {
			List<ImportRegistration> importRegistrationsList = importRegistrations.get(bundle);
			if (importRegistrationsList != null) {
				for (ImportRegistration ir : importRegistrationsList) {
					ir.close();
				}
			}
			break;
		}
		}

	}

}
