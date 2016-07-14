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
package be.iminds.aiolos.cloud.local;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.TimeoutException;

import org.osgi.service.log.LogService;

import be.iminds.aiolos.cloud.AbstractCloudManager;
import be.iminds.aiolos.cloud.Activator;
import be.iminds.aiolos.cloud.api.CloudException;
import be.iminds.aiolos.cloud.api.VMInstance;

public class CloudManagerLocal extends AbstractCloudManager {

	private static final Set<String> privateaddress = new HashSet<String>(Arrays.asList("localhost")); // ip does not work -> virtualhost of jetty is localhost

	private static final Map<String, OSGiRuntime> processes = new HashMap<String, OSGiRuntime>();
	private static final Map<String, VMInstance> instances = new HashMap<String, VMInstance>();

	@Override
	public synchronized VMInstance startVM(String bndrun, List<String> resources) throws CloudException, TimeoutException {
		return startVMs(bndrun, resources, 1).iterator().next();
	}

	@Override
	public synchronized List<VMInstance> startVMs(String bndrun, List<String> resources, int count) throws CloudException, TimeoutException {
		List<VMInstance> result = new ArrayList<VMInstance>();
		try {
			for (int i=0; i<count; i++) {
				OSGiRuntime runtime = new OSGiRuntime(bndrun);
				runtime.start();
				Thread.yield();
				String endpoint = runtime.getEndpointListener();
				long start = System.currentTimeMillis();
				while(endpoint==null && System.currentTimeMillis()-start < 15000){
					Thread.sleep(100);
					Thread.yield();
					endpoint = runtime.getEndpointListener();
				}
				if(endpoint==null){
					throw new TimeoutException();
				}
				List<String> ips = Arrays.asList(runtime.getIp());
				int osgiPort = runtime.osgiPort();
				int httpPort = runtime.httpPort();
				VMInstance instance = new VMInstance(runtime.process.toString(), null, null, null, null, null, 
						"localhost", privateaddress, new HashSet<String>(ips), null, osgiPort, httpPort);
				result.add(instance);
				instances.put(runtime.process.toString(), instance);
				processes.put(runtime.process.toString(), runtime);
			}
		} catch (Exception e) {
			if(e instanceof TimeoutException){
				throw (TimeoutException)e;
			} else {
				throw new CloudException("Error starting new instance(s)", e);
			}
		}
		return result;
	}

	@Override
	public List<VMInstance> listVMs() {
		List<VMInstance> vms = new ArrayList<VMInstance>(instances.values());
		return vms;
	}

	@Override
	public VMInstance stopVM(String id) {
		OSGiRuntime runtime = processes.get(id);
		if(runtime!=null){
			runtime.stop();
			processes.remove(id);
		}
		return instances.remove(id);
	}

	@Override
	public List<VMInstance> stopVMs(String[] ids) {
		List<VMInstance> stopped = new ArrayList<VMInstance>();
		for(String id : ids){
			stopped.add(stopVM(id));
		}
		return stopped;
	}

	@Override
	public List<VMInstance> stopVMs() {
		List<VMInstance> stopped = new ArrayList<VMInstance>();
		for(String id : processes.keySet()){
			stopped.add(stopVM(id));
		}
		return stopped;
	}

	/**
	 * Prints the log messages from nodes to standard output. If we would log this to aiolos Logger,
	 * messages would be saved multiple times in LogService.
	 */
	private synchronized static void println(String log) {
		System.out.println(log);
	}

	private static class OSGiRuntime {
		private static int counter = 0;
		
		private final String bndrun;
		private final String tag;
		private final int id;
		
		int httpPort;
		
		private Process process;
		private boolean running;
		//private Thread loggerThread;

		private BufferedReader reader;
		private PrintWriter writer;
		
		private Runnable destroy = new Runnable() {
			public void run() {
				running = false;
				process.destroy();
			}
		};

		// gather endpoint ...
		private String endpoint;
		
		public OSGiRuntime(String bndrun) {
			this.bndrun = bndrun;
			String id = "OSGi";
			if(bndrun.contains("mgmt"))
				id = "MGMT";
			else if(bndrun.contains("vm"))
				id = "VM";
			
			this.id = counter;
			this.tag = "["+id+"-" +counter+ "]";
			counter++;
		}

		public void start() throws Exception {
			Properties props = new Properties();
			props.load(new FileInputStream(new File(bndrun)));
			// make sure they have different cache
			props.put("-runstorage", "generated/"+tag);
			// set the increase the port number for local vms
			String runproperties = props.getProperty("-runproperties");
			String newproperties = "";
			StringTokenizer st = new StringTokenizer(runproperties,",");
			while(st.hasMoreElements()){
				String property = st.nextToken().trim();
				if(!property.startsWith("org.osgi.service.http.port")){
					newproperties+=property+",";
				} else {
					String portString = property.substring(property.indexOf('=')+1);
					httpPort = Integer.parseInt(portString)+id;
					newproperties+="org.osgi.service.http.port="+httpPort+",";
				}
			}
			props.put("-runproperties", newproperties.substring(0, newproperties.length()-1));
			File tempFile = new File("." + tag+ ".bndrun"); // hidden file
			tempFile.deleteOnExit();
			props.store(new FileOutputStream(tempFile), tag + " bndrun");
			
			File bndDir = new File("../cnf/plugins/biz.aQute.bnd");
			String[] bnd = bndDir.list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					if(name.endsWith(".jar"))
						return true;
					return false;
				}
			});
			if(bnd==null || bnd.length<1){
				Activator.logger.log(LogService.LOG_ERROR, "No valid bnd.jar found...");
				return;
			}
			String jarPath = bndDir.getAbsolutePath()+File.separator+bnd[0];
			String[] cmd = new String[] { "java", "-jar", jarPath,
					tempFile.getAbsolutePath() };
			ProcessBuilder pb = new ProcessBuilder(cmd);
			pb.redirectErrorStream(true);
			process = pb.start();
			running = true;

			reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			writer = new PrintWriter(process.getOutputStream());


			Runtime.getRuntime().addShutdownHook(new Thread(destroy));

			Thread loggerThread = new Thread(new Runnable() {
				@Override
				public void run() {
					while (running) {
						try {
							String log = reader.readLine();

							// This only works with GoGo shell!
							if (endpoint == null) {
								if (log.contains("r-osgi://")) {
									do {
										if (log.contains("EndpointListener")) {
											endpoint = log;
										}
										log = reader.readLine();
									} while(log.contains("r-osgi://"));
								} else if (log.equals("")) {
									Thread.sleep(1000);
									writer.println("rsa:endpoints");
									writer.flush();
								} else {
									println(tag + log);
								}
							} else {
								if(log==null)
									running = false;
								else
									println(tag + log);
							}
						} catch (Exception e) {
							//e.printStackTrace();
						}
					}
				}
			});
			loggerThread.start();
		}
		
		public String getEndpointListener(){
			 return endpoint;
		}
		
		public int httpPort(){
			return httpPort;
		}
		
		public int osgiPort(){
			String osgiPort = null;
			if (endpoint != null) {
				String pattern = "^.*:([0-9]+)#.*$";
				osgiPort = endpoint.replaceAll(pattern, "$1");
				pattern = "^.*//(.*):.*$";
			}
			return Integer.parseInt(osgiPort);
		}
		
		public String getIp(){
			String ip = null;
			if (endpoint != null) {
				String pattern = "^.*//(.*):.*$";
				ip = endpoint.replaceAll(pattern, "$1");
			}
			return ip;
		}
		
		public void stop(){
			Thread stop = new Thread(destroy);
			stop.start();
			try {
				stop.join();
			} catch (InterruptedException e) {
			}
		}
	}

	@Override
	public void configure(Dictionary<String, ?> properties) {
		// no real configuration options here
	}
}
