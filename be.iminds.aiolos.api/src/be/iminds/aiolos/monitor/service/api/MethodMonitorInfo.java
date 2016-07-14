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
package be.iminds.aiolos.monitor.service.api;

public class MethodMonitorInfo {

	private final String methodName;
	
	private long lastArrival = 0;
	
	private Statistic arrival = new Statistic();
	private Statistic total = new Statistic();
	private Statistic self = new Statistic();
	private Statistic argSize = new Statistic();
	private Statistic retSize= new Statistic();
	
	public MethodMonitorInfo(String methodName){
		this.methodName = methodName;
	}
	
	public synchronized void  addCall(long start, long stop, 
			double self, double argSize, double retSize){
		if(lastArrival!=0)
			this.arrival.addValue(start-lastArrival);
		this.total.addValue(stop-start);
		this.self.addValue(self);
		this.argSize.addValue(argSize);
		this.retSize.addValue(retSize);
		this.lastArrival = start;
	}
	
	public String getName(){
		return methodName;
	}
	
	public double getInterarrivalTime(){
		return arrival.mean();
	}
	
	public double getInterarrivalTimeStd(){
		return arrival.stdev();
	}
	
	public double getTime(){
		return total.mean();
	}
	
	public double getTimeStd(){
		return total.stdev();
	}
	
	public double getSelfTime(){
		return self.mean();
	}
	
	public double getSelfTimeStd(){
		return self.stdev();
	}
	
	public double getArgSize(){
		return argSize.mean();
	}
	
	public double getArgSizeStd(){
		return argSize.stdev();
	}
	
	public double getRetSize(){
		return retSize.mean();
	}
	
	public double getRetSizeStd(){
		return retSize.stdev();
	}
	
}
