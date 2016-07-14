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
package be.iminds.aiolos.repository;

import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

import org.apache.felix.service.command.CommandProcessor;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;
import org.osgi.service.repository.Repository;

import be.iminds.aiolos.repository.command.RepoCommands;
import be.iminds.aiolos.util.log.Logger;

/**
 * The {@link BundleActivator} for the Repository bundle. 
 */
public class Activator implements BundleActivator {

	public static Logger logger;
	private static final String PID = "be.iminds.aiolos.repository.Repository";
	private BundleContext bundleContext;
	private ServiceRegistration<ManagedService> managedService;
	private ServiceRegistration<Repository> repoService;
	private ServiceRegistration<Object> gogoService;
	
	@Override
	public void start(final BundleContext bundleContext) throws Exception {
		this.bundleContext = bundleContext;
		logger = new Logger(bundleContext);
		logger.open();
		
		Dictionary<String,Object> properties = new Hashtable<String,Object>();
		properties.put(Constants.SERVICE_PID, Activator.PID);
		managedService = bundleContext.registerService(ManagedService.class, new RepositoryConfigurator(), properties);
	}
	
	@Override
	public void stop(final BundleContext context) throws Exception {
		if (managedService != null) {
			managedService.unregister();
			managedService = null;
		}
		unregisterPreviousRepository();
		logger.close();
	}
	
	private void unregisterPreviousRepository() {
		if (repoService != null) {
			repoService.unregister();
			repoService = null;
		}
		if (gogoService != null) {
			gogoService.unregister();
			gogoService = null;
		}
	}
	
	private class RepositoryConfigurator implements ManagedService {
		
		@Override
		public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
			if (properties == null) {
				if (repoService != null) {
					unregisterPreviousRepository();
					Activator.logger.log(LogService.LOG_DEBUG, "Configuration deleted (" + Activator.PID + ")");
				}
	            // else no configuration from configuration admin
	        } else {
	            // apply configuration from config admin
	        	updateRepository(properties);
	        	Activator.logger.log(LogService.LOG_DEBUG, "Configuration updated (" + Activator.PID + ")");
	        }
		}
		
		private void updateRepository(Dictionary<String, ?> props) {
			String urls = (String) props.get("repository.urls");
			if(urls!=null){
				unregisterPreviousRepository();
				StringTokenizer tokenizer = new StringTokenizer(urls, ",");
				while(tokenizer.hasMoreTokens()){
					
					// for each URL, parse the index and register a Repository service instance
					String url = tokenizer.nextToken();
					String publicURL = null;
					URL indexURL = null;
					try {
						try{
							indexURL = new URL(url);
						} catch(MalformedURLException e){
							// no valid url, maybe a relative path to filesystem
							File f = new File(url);
							if(!f.exists()){
								throw new Exception("Invalid URL/File "+url);
							}
							indexURL = f.toURI().toURL();
						}
						publicURL = indexURL.toString();
						
						// If the repo consists of local files, make them available as HTTP resources
						// and change indexURL to http url
						if(indexURL.getProtocol().equals("file")){
							String path = indexURL.getPath();
							path = path.substring(0, path.lastIndexOf("/"));
							String alias = path.substring(path.lastIndexOf("/"));
							
							ServiceReference<HttpService> ref = bundleContext.getServiceReference(HttpService.class);
							if(ref==null){
								// sleep and try again ... 
								// TODO this is a hack (at startup time HttpService may not yet be intialized)
								Thread.sleep(100);
								ref = bundleContext.getServiceReference(HttpService.class);
							}
							if(ref!=null){
								HttpService http = bundleContext.getService(ref);
								String name = indexURL.toString().substring(0, indexURL.toString().lastIndexOf("/"));
								http.registerResources(alias, name , new RepositoryHttpContext());
							}
							
							// TODO how to construct the right URL here (right IP and Port number?)
							// for now relying on same method as used in RSA and fixed 8080 port
							String publicip = bundleContext.getProperty("rsa.ip");
							String ip = getIP();
							indexURL = new URL("http://"+ip+":"+getPort()+alias+"/index.xml");
							
							if(publicip!=null && !ip.equals(publicip)){
								publicURL = "http://"+publicip+":"+getPort()+alias+"/index.xml";
							} else {
								publicURL = indexURL.toString();
							}
						}

						//Initialize new repo from url
						RepositoryImpl repo = IndexParser.parseIndex(indexURL, publicURL);


						Hashtable<String, Object> properties = new Hashtable<String, Object>();
						properties.put("service.pid", repo.getName());
						// make Repository service remote available 
						properties.put("service.exported.interfaces", new String[]{Repository.class.getName()});
						repoService = bundleContext.registerService(Repository.class, repo , properties);
						
						
						// GoGo Shell
						// add shell commands (try-catch in case no shell available)
						RepoCommands commands = new RepoCommands(repo);
						Dictionary<String, Object> commandProps = new Hashtable<String, Object>();
						try {
							commandProps.put(CommandProcessor.COMMAND_SCOPE, "repo");
							commandProps.put(CommandProcessor.COMMAND_FUNCTION, new String[] {"list"});
							gogoService = bundleContext.registerService(Object.class, commands, commandProps);
						} catch (Throwable t) {
							// ignore exception, in that case no GoGo shell available
						}

					} catch(Exception e){
						logger.log(LogService.LOG_ERROR, "Could not initialize repository "+indexURL, e);
					}
				}
			}
		}

		private String getIP(){
			String hostAddress = null;
			try {
				Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
				String preferredInterface = bundleContext.getProperty("rsa.interface");
				for (NetworkInterface netint : Collections.list(nets)){
					if(preferredInterface==null || 
							(preferredInterface!=null && netint.getName().equals(preferredInterface))){
						Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
						for (InetAddress inetAddress : Collections.list(inetAddresses)) {
							 if(inetAddress instanceof Inet4Address){
								 if(hostAddress!=null && (inetAddress.isLoopbackAddress() || inetAddress.isAnyLocalAddress()))
									 break;  //only set loopbackadres if no other possible
								 else {	 
									 hostAddress = inetAddress.getHostAddress();
						     		 break;
								 }
						     }
						}
					}
			    }
			}catch(Exception e){}
			return hostAddress;
		}
		
		private int getPort(){
			int port = 8080;
			
			String p = bundleContext.getProperty("org.osgi.service.http.port");
			if(p!=null){
				port = Integer.parseInt(p);
			}
			
			return port;
		}
	}
}
