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
package be.iminds.aiolos.rsa.command;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.service.remoteserviceadmin.ImportRegistration;

import be.iminds.aiolos.rsa.Activator;
import be.iminds.aiolos.rsa.Config;
import be.iminds.aiolos.rsa.ROSGiServiceAdmin;
import be.iminds.aiolos.rsa.network.api.NetworkChannel;

/**
 * 	CLI Commands to import/export services 
 *  and list all exported endpoints 
 */
public class RSACommands {
	
	private final ROSGiServiceAdmin rsa;
	private final BundleContext context;
	
	public RSACommands(BundleContext context,
			ROSGiServiceAdmin rsa){
		this.context = context;
		this.rsa = rsa;
	}
	
	/*
	 * OSGi Shell commands implementations
	 */
	
	public void endpoints(){
		StringBuilder sb = new StringBuilder();
		for(ExportReference export : rsa.getExportedServices()){
			sb.append(export.getExportedEndpoint().getId());
			sb.append(" ");
			for(String iface : export.getExportedEndpoint().getInterfaces()){
				sb.append(iface+" ");
			}
			sb.append("\n");
		}
		System.out.println(sb.toString());
	}

	public void importEndpoint(String uri, String clazz){
		try {
			Map<String, Object> properties = new HashMap<String, Object>();
			properties.put("endpoint.id", uri);
			properties.put("service.imported.configs", Config.CONFIG_ROSGI);
			properties.put("objectClass", new String[]{clazz});
			EndpointDescription endpoint = new EndpointDescription(properties);
			ImportRegistration ir = rsa.importService(endpoint);
			if(ir.getException()!=null){
				throw new Exception(ir.getException());
			} else {
				System.out.println("Imported endpoint "+ir.getImportReference().getImportedEndpoint().getId()
						+" "+ir.getImportReference().getImportedEndpoint().getFrameworkUUID());
			}
		} catch (Exception e) {
			System.err.println("Failed to import endpoint "+uri);
			e.printStackTrace();
		}
	}
	
	public void exportEndpoint(String clazz){
		try {
			ServiceReference<?> toExport = context.getServiceReference(clazz);
			Map<String, Object> properties = new HashMap<String, Object>();
			properties.put("service.exported.interfaces", new String[]{clazz});
			Collection<ExportRegistration> exports = rsa.exportService(toExport, properties);
			for(ExportRegistration export : exports){
				if(export.getException()!=null){
					throw new Exception(export.getException());
				} 
			}
			System.out.println("Exported service "+toExport.getProperty("service.id"));
		} catch (Exception e) {
			System.err.println("Error exporting service.");
			e.printStackTrace();
		}
	}
	
	public void channels(){
		StringBuilder sb = new StringBuilder();
		sb.append("Channels:\n");
		for(NetworkChannel c : rsa.getChannels()){
			try {
				sb.append("* "+c.getLocalAddress()+"->"+c.getRemoteAddress() + "\n");
			} catch(Exception e){
				e.printStackTrace();
			}
		}
		System.out.println(sb.toString());
	}
}
