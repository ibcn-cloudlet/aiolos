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
package be.iminds.aiolos.monitor.node.api;

public class NodeMonitorInfo {

	private final long timestamp;
	
	private final String nodeId;
	private final int noCpuCores;
	private final double cpuUsage;
	private final double memoryUsage;
	private final long bpsIn;
	private final long bpsOut;
	
	public NodeMonitorInfo(String nodeId,
			int noCpuCores, double cpuUsage, double memoryUsage,
			long bpsIn, long bpsOut){
		this.timestamp = System.currentTimeMillis();
		this.nodeId = nodeId;
		this.noCpuCores = noCpuCores;
		this.cpuUsage = cpuUsage;
		this.memoryUsage = memoryUsage;
		this.bpsIn = bpsIn;
		this.bpsOut = bpsOut;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public String getNodeId() {
		return nodeId;
	}

	public int getNoCpuCores() {
		return noCpuCores;
	}

	public double getCpuUsage() {
		return cpuUsage;
	}

	public double getMemoryUsage() {
		return memoryUsage;
	}
	
	public long getBpsIn(){
		return bpsIn;
	}
	
	public long getBpsOut(){
		return bpsOut;
	}
}
