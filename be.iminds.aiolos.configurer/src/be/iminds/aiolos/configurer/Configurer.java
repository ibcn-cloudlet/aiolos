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
package be.iminds.aiolos.configurer;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.BundleTrackerCustomizer;

public class Configurer implements  BundleTrackerCustomizer<Bundle> {

	private final String location = "/configuration"; //TODO use a header?

	
	private final ConfigurationAdmin cm;
	
	private final Map<String, String> pids = new HashMap<String, String>();
	
	public Configurer(ConfigurationAdmin cm){
		this.cm = cm;
	}

	@Override
	public Bundle addingBundle(Bundle bundle, BundleEvent event) {
		for(URL url : getConfigurationURLs(bundle, location)){
			configure(bundle, url);
		}
		return bundle;
	}

	@Override
	public void modifiedBundle(Bundle bundle, BundleEvent event, Bundle object) {}

	@Override
	public void removedBundle(Bundle bundle, BundleEvent event, Bundle object) {
		for(URL url : getConfigurationURLs(bundle, location)){
			deconfigure(url);
		}
	}
	
	public void configure(Bundle b, URL url){
		Properties props = new Properties();
		try {
			props.load(url.openStream());
			props.put("be.iminds.aiolos.configurer.bundle.id", b.getBundleId());
			
			String[] pid = parsePid(url);
			if(pid[1]==null){
				// regular pid
				
				Configuration conf = cm.getConfiguration(pid[0], null);
				conf.update(toDictionary(props));
				
				pids.put(url.toString(), pid[0]);
			} else {
				// factory pid
				Configuration conf = cm.createFactoryConfiguration(pid[1], null);
				conf.update(toDictionary(props));
				String generatedPid = conf.getPid();
				pids.put(url.toString(), generatedPid);
			}
			
		} catch (IOException e) {
			System.err.println("Error configuring "+url);
			e.printStackTrace();
		}
	}
	
	public void deconfigure(URL url){
		String pid = pids.remove(url.toString());
		if(pid!=null){
			try {
				Configuration conf = cm.getConfiguration(pid, null);
				conf.delete();
			} catch(IOException e){
				System.err.println("Error remove configuration "+url);
				e.printStackTrace();
			}
		}
	}
	
	public String[] parsePid(URL url){
		String pid = url.getFile();
		int i = pid.lastIndexOf("/"); 
		if(i > 0){
			pid = pid.substring(i+1, pid.lastIndexOf("."));
		}

		int j = pid.lastIndexOf("-");
		if(j > 0){
			// factory
			String factoryPid = pid.substring(0, j);
			pid = pid.substring(j+1);
			return new String[]{pid, factoryPid};
		} else {
			return new String[]{pid, null};
		}
		
	}
	
	List<URL> getConfigurationURLs(Bundle bundle, String location){
		List<URL> urls = new ArrayList<URL>();
		Enumeration<String> paths = bundle.getEntryPaths(location);
		if(paths!=null){
			while(paths.hasMoreElements()){
				String path = paths.nextElement();
				URL url = bundle.getEntry(path);
				urls.add(url);
			}
		}
		return urls;
	}

	public Dictionary<String, ?> toDictionary(Properties p){
		Dictionary<String, Object> d = new Hashtable<String, Object>();
		for(Object k : p.keySet()){
			d.put((String)k, p.get((String)k));
		}
		return d;
	}
}
