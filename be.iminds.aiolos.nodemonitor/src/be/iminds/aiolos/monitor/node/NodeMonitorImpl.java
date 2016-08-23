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
package be.iminds.aiolos.monitor.node;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

import org.osgi.service.log.LogService;

import be.iminds.aiolos.monitor.node.api.NodeMonitor;
import be.iminds.aiolos.monitor.node.api.NodeMonitorInfo;

public class NodeMonitorImpl implements NodeMonitor {
	private int interval = 0;

	private final String nodeId;
	
	// cpu
	private volatile double cpu = 0;
	private long cpuWork = 0;
	private long cpuIdle = 0;
	private long cpuWorkLast = 0;
	private long cpuIdleLast = 0;

	private int noCores = -1;

	// memory
	private long memTotal = 0;
	private long memFree = 0;
	private long memBuffers = 0;
	private long memCached = 0;
	private volatile double memory = 0;
	
	// bandwidth
	private String netinterface = null; // network interface to monitor
	private volatile long bpsIn = 0;
	private long rcvLast = 0;
	private volatile long bpsOut = 0;
	private long sntLast = 0;

	private boolean running = true;

	public NodeMonitorImpl(String nodeId) {
		this.nodeId = nodeId;
	}
	
	public NodeMonitorImpl(String nodeId, int interval, String netinterface) {
		this.nodeId = nodeId;
		this.interval = interval;
		this.netinterface = netinterface;
	}

	@Override
	public double getCpuUsage() {
		return cpu;
	}

	@Override
	public double getMemoryUsage() {
		return memory;
	}

	@Override
	public int getNoCpuCores() {
		return noCores;
	}

	@Override
	public long getBpsIn(){
		return bpsIn;
	}
	
	@Override
	public long getBpsOut(){
		return bpsOut;
	}
	
	@Override
	public NodeMonitorInfo getNodeMonitorInfo() {
		// if interval <= 0, read on request
		if(interval <= 0)
			update();
		return new NodeMonitorInfo(nodeId, noCores, cpu, memory, bpsIn, bpsOut);
	}
	
	public void start() {
		Thread t = new Thread(new MonitorThread());
		t.start();
	}

	public void stop() {
		running = false;
	}
	
	private void update() {
		String line = null;

		// total cpu monitor
		File cpuFile = new File("/proc/stat");
		BufferedReader cpuReader = null;
		try {
			cpuReader= new BufferedReader(new FileReader(cpuFile));
			String cpuInfo = cpuReader.readLine();
	
			StringTokenizer t = new StringTokenizer(cpuInfo, " ");
			t.nextToken();
			long umode = Long.parseLong(t.nextToken());
			long nmode = Long.parseLong(t.nextToken());
			long smode = Long.parseLong(t.nextToken());
			long idle = Long.parseLong(t.nextToken());
			long work = umode + nmode + smode;
	
			cpuWork = work - cpuWorkLast;
			cpuIdle = idle - cpuIdleLast;
	
			cpu = 100 * ((double) (cpuWork)) / (cpuWork + cpuIdle);
	
			cpuWorkLast = work;
			cpuIdleLast = idle;
	
			// System.out.println("CPU usage " + cpu + " %");
	
			int i = 0;
			while (cpuReader.readLine().startsWith("cpu")) {
				i++;
			}
			// keep max no of seen active processors
			// due to hotplug this can change over time
			if (i > noCores)
				noCores = i;

		} catch(IOException e){
			Activator.logger.log(LogService.LOG_WARNING, "Error reading cpu monitor information: " + e.getLocalizedMessage());
		} finally {
			try {
				if (cpuReader != null)
					cpuReader.close();
			} catch (IOException e) {
			}
		}

		// process memory monitor
		File memFile = new File("/proc/meminfo");
		BufferedReader memReader = null;
		
		try {
			memReader = new BufferedReader(new FileReader(memFile));
	
			memory = 0;
			while ((line = memReader.readLine()) != null) {
				if (line.startsWith("MemTotal")) {
					StringTokenizer st = new StringTokenizer(line,
							" ");
					st.nextToken();
					memTotal = Long.parseLong(st.nextToken());
				} else if (line.startsWith("MemFree")) {
					StringTokenizer st = new StringTokenizer(line,
							" ");
					st.nextToken();
					memFree = Long.parseLong(st.nextToken());
				} else if (line.startsWith("Buffers")) {
					StringTokenizer st = new StringTokenizer(line,
							" ");
					st.nextToken();
					memBuffers = Long.parseLong(st.nextToken());
				} else if (line.startsWith("Cached")) {
					StringTokenizer st = new StringTokenizer(line,
							" ");
					st.nextToken();
					memCached = Long.parseLong(st.nextToken());
				}
			}
	
			memory = 100 * ((double) (memTotal - memFree - memBuffers - memCached))
					/ memTotal;
	
			// System.out.println("Memory usage is "+ memory +" %");
		} catch(IOException e){
			Activator.logger.log(LogService.LOG_WARNING, "Error reading memory monitor information: " + e.getLocalizedMessage());
		} finally {
			try {
				if (memReader != null)
					memReader.close();
			} catch (IOException e) {
			}
		}

		// Bandwidth monitor
		File networkFile = new File("/proc/net/dev");
		BufferedReader networkReader = null;
		
		try {
			networkReader = new BufferedReader( new FileReader(networkFile));
			line = null;
	
			long bytesRecieved = 0;
			long bytesSent = 0;
			while ((line = networkReader.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(line, " ");
				String token = st.nextToken();
				if (token.contains(":") && (netinterface==null || token.contains(netinterface))) {
					String b = token
							.substring(token.indexOf(':') + 1);
					if (b.length() != 0)
						bytesRecieved += Long.parseLong(b);
					else
						bytesRecieved += Long.parseLong(st
								.nextToken());
	
					for (int k = 0; k < 7; k++)
						st.nextToken();
	
					bytesSent += Long.parseLong(st.nextToken());
				}
	
			}
	
			bpsIn = (bytesRecieved - rcvLast)*8/interval;
			rcvLast = bytesRecieved;
	
			bpsOut = (bytesSent - sntLast)*8/interval;
			sntLast = bytesSent;
		
		} catch(IOException e){
			Activator.logger.log(LogService.LOG_WARNING, "Error reading network monitor information: " + e.getLocalizedMessage());
		} finally {
			try {
				if (networkReader!=null)
					networkReader.close();
			} catch (IOException e) {
			}
		}
	}
	

	private class MonitorThread implements Runnable {
		public void run() {
			if (interval > 0) {
				while (running) {
					try {
						update();

						Thread.sleep(interval * 1000);
					} catch (InterruptedException e) {
						Activator.logger.log(LogService.LOG_ERROR, "NodeMonitoring interupted", e);
					}
				}
			}

		}
	}

}
