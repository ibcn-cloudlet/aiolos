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
package be.iminds.aiolos.monitor.component;

import java.util.Stack;

public class MonitoredThread {

	private final long threadId;
	
	private long time = 0;
	
	private Stack<MonitoredMethodCall> methodStack = new Stack<MonitoredMethodCall>();
	
	public MonitoredThread(long id){
		this.threadId = id;
	}
	
	public long getThreadId(){
		return threadId;
	}
	
	public long getTime() {
		return time;
	}
	
	public void setTime(long time){
		this.time = time;
	}
	
	public MonitoredMethodCall getCurrentMethod() {
		MonitoredMethodCall currentMethod = null;
		if(methodStack.size()>0){
			currentMethod = methodStack.get(methodStack.size()-1);
		}
		return currentMethod;
	}
	
	public MonitoredMethodCall pop(){
		MonitoredMethodCall ret = null;
		if(methodStack.size()>0){
			ret = methodStack.pop();
		}
		return ret;
	}
	
	public void push(MonitoredMethodCall call){
		methodStack.push(call);
	}
}
