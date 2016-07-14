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
package be.iminds.aiolos.discovery;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import be.iminds.aiolos.topology.api.TopologyManager;

public abstract class Discovery implements Runnable {

	protected final static int INTERVAL = 60; // in seconds
	
	private TopologyManager topologyManager = null;
	
	private Set<String> registered = new HashSet<String>();
	private Set<String> discovered = new HashSet<String>();
	
	private volatile boolean discovering = false;
	
	public void setTopologyManager(TopologyManager topologyManager){
		this.topologyManager = topologyManager;
		
	}
	
	public void start(){
		discovering = true;
		Thread discoveryThread = new Thread(this);
		discoveryThread.start();
	}
	
	public void stop(){
		discovering = false;
	}
	
	public void run(){
		while(discovering){
			Set<String> uris = discoverURIs();
			Iterator<String> it = uris.iterator();
			Set<String> toConnect = new HashSet<String>();
			synchronized(registered){
				while(it.hasNext()){
					String uri = it.next();
					if(registered.contains(uri)){
						it.remove();
					} else if(!discovered.contains(uri)){
						// newly discovered
						toConnect.add(uri);
					}
				}
			}
			discovered = new HashSet<String>(uris);
			
			// connect
			if(topologyManager!=null){
				for(String uri : toConnect){
					String ip = uri.substring(uri.indexOf("://")+3, uri.lastIndexOf(":"));
					int port = Integer.parseInt(uri.substring(uri.lastIndexOf(":")+1));
					topologyManager.connect(ip, port);
				}
			}
			
			try {
				Thread.sleep(INTERVAL*1000);
			} catch (InterruptedException e) {
			}
		}
	}
	
	public void register(String uri){
		synchronized(registered){
			registered.add(uri);
		}
		registerURI(uri);
	}
	
	public void deregister(String uri){
		synchronized(registered){
			registered.remove(uri);
		}
		deregisterURI(uri);
	}
	
	protected abstract void registerURI(String uri);
	
	protected abstract void deregisterURI(String uri);
	
	protected abstract Set<String> discoverURIs();
	
	
}
