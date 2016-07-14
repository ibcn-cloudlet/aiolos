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

import java.util.HashMap;
import java.util.Map;

import be.iminds.aiolos.info.ServiceInfo;
import be.iminds.aiolos.monitor.service.api.MethodMonitorInfo;
import be.iminds.aiolos.monitor.service.api.ServiceMonitor;
import be.iminds.aiolos.monitor.service.api.ServiceMonitorInfo;
import be.iminds.aiolos.proxy.api.ServiceProxyListener;

import com.vladium.utils.ObjectProfiler;

/**
 * Implementation of the {@link ServiceMonitor} interface
 *
 */
public class ServiceMonitorImpl implements ServiceProxyListener, ServiceMonitor {

	private final boolean keepSize;
	
	private final Map<Long, MonitoredThread> threads = new HashMap<Long, MonitoredThread>();
	private final Map<ServiceInfo, ServiceMonitorInfo> monitorInfo = new HashMap<ServiceInfo, ServiceMonitorInfo>();
	
	public ServiceMonitorImpl(boolean keepSize){
		this.keepSize = keepSize;
	}
	
	@Override
	public void methodCalled(ServiceInfo service, String methodName,
			long threadId, Object[] args, long timestamp) {
		int argSize = 0;
		if(keepSize){
			if(args!=null){
				for(Object arg : args){
					argSize += ObjectProfiler.sizeof(arg);
				}
			}
		}
		
		MonitoredThread thread = threads.get(threadId);
		if(thread == null){
			thread = new MonitoredThread(threadId);
			threads.put(threadId, thread);
		}
		
		MonitoredMethodCall call = new MonitoredMethodCall(methodName, service);

		MonitoredMethodCall parent = thread.getCurrentMethod();
		if(parent!=null){
			parent.self+=(timestamp-thread.getTime());
			call.parent = parent;
		}
		
		// TODO do we need to know where the call comes from?
		// e.g. which bundle? by inspecting the stacktrace
		
		call.argSize = argSize;
		call.start = timestamp;

		thread.push(call);
		thread.setTime(timestamp);
	}


	@Override
	public void methodReturned(ServiceInfo service, String methodName,
			long threadId, Object ret, long timestamp) {
	
		int retSize = 0 ;
		if(keepSize){
			if(ret!=null){
				retSize += ObjectProfiler.sizeof(ret);
			}
		}
		
		MonitoredThread thread = threads.get(threadId);
		if(thread == null){
			// add new thread 
			thread = new MonitoredThread(threadId);
			threads.put(threadId, thread);
		}
		
		if(thread.getCurrentMethod()!=null){
			thread.getCurrentMethod().retSize = retSize;
			thread.getCurrentMethod().self += (timestamp-thread.getTime());
			thread.getCurrentMethod().end = timestamp;
			
			MonitoredMethodCall call = thread.pop();
			
			if(thread.getCurrentMethod()!=null){
				thread.getCurrentMethod().children.add(call);
			} else {
				// remove MonitoredThread object if all calls done?
				threads.remove(thread.getThreadId());
			}
			
			// add to summary monitorInfo
			ServiceMonitorInfo serviceMonitorInfo = monitorInfo.get(service);
			if(serviceMonitorInfo == null){
				serviceMonitorInfo = new ServiceMonitorInfo(service);
				monitorInfo.put(service, serviceMonitorInfo);
			}
			
			MethodMonitorInfo methodInfo = serviceMonitorInfo.getMethod(call.methodName);
			if(methodInfo == null){
				methodInfo = new MethodMonitorInfo(methodName);
				serviceMonitorInfo.addMethodMonitorInfo(methodInfo);
			}
			
			methodInfo.addCall(call.start, call.end, call.self, call.argSize, call.retSize);
		}
		thread.setTime(timestamp);
		
		//Activator.logger.log(LogService.LOG_DEBUG, "Method returned "+methodName+" "+service.frameworkId+" "+service.componentId+" "+service.serviceId+" "+service.bundleId+" "+threadId+" "+retSize+" "+timestamp);
	}


	@Override
	public ServiceMonitorInfo getServiceMonitorInfo(ServiceInfo service) {
		return monitorInfo.get(service);
	}

}
