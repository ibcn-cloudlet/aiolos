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
package be.iminds.aiolos.ds.util;

import java.lang.reflect.Method;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import be.iminds.aiolos.ds.description.ComponentDescription;
import be.iminds.aiolos.ds.description.ReferenceDescription;

public class ComponentMethodLookup {

	public static Method getActivate(Object object, ComponentDescription description){
		String methodName = description.getActivate();
		return getMethod(object, methodName, 
				new Class[]{ComponentContext.class, BundleContext.class, Map.class});
	}

	public static Method getDeactivate(Object object, ComponentDescription description){
		String methodName = description.getDeactivate();
		return getMethod(object, methodName, 
				new Class[]{ComponentContext.class, BundleContext.class, Map.class, Integer.class});		
	}

	public static Method getModified(Object object, ComponentDescription description){
		String methodName = description.getActivate();
		return getMethod(object, methodName, 
				new Class[]{ComponentContext.class, BundleContext.class, Map.class});		
	}
	
	public static Method getBind(Object object, ReferenceDescription description){
		String methodName = description.getBind();
		Class interfaceClass;
		try {
			interfaceClass = object.getClass().getClassLoader().loadClass(description.getInterface());
		} catch(ClassNotFoundException e){
			System.err.println("Could not find interface class "+description.getInterface());
			return null;
		}
		return getMethod(object, methodName, 
				new Class[]{ServiceReference.class, Map.class, 
					interfaceClass});		
	}
	
	public static Method getUnbind(Object object, ReferenceDescription description){
		String methodName = description.getUnbind();
		Class interfaceClass;
		try {
			interfaceClass = object.getClass().getClassLoader().loadClass(description.getInterface());
		} catch(ClassNotFoundException e){
			System.err.println("Could not find interface class "+description.getInterface());
			return null;
		}
		return getMethod(object, methodName, 
				new Class[]{ServiceReference.class, Map.class, 
					interfaceClass});	
	}
	
	public static Method getUpdated(Object object, ReferenceDescription description){
		String methodName = description.getUpdated();
		Class interfaceClass;
		try {
			interfaceClass = object.getClass().getClassLoader().loadClass(description.getInterface());
		} catch(ClassNotFoundException e){
			System.err.println("Could not find interface class "+description.getInterface());
			return null;
		}
		return getMethod(object, methodName, 
				new Class[]{ServiceReference.class, Map.class, 
					interfaceClass});	
	}
	
	
	private static Method getMethod(Object object, String methodName, Class[] acceptableTypes){
		// TODO should be done according to 112.9.4
		Method result = null;
		for(Method method : object.getClass().getDeclaredMethods()){
			if(method.getName().equals(methodName)){
				if(checkMethod(method, acceptableTypes)){
					result = method;
					break;
				}
			}
		}
		// set accessible
		if(result!=null){
			result.setAccessible(true);
		}
		return result;
	}
	
	private static boolean checkMethod(Method method, Class[] acceptableTypes){
		boolean ok = true;
		for(Class p : method.getParameterTypes()){
			boolean acceptableType = false;
			for(Class a : acceptableTypes){
				if(p.equals(a)){
					acceptableType = true;
				}
			}
			if(!acceptableType){
				ok  = false;
			}
		}
		return ok;
	}
}
