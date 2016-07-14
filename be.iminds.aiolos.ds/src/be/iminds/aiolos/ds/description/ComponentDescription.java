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
package be.iminds.aiolos.ds.description;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

public class ComponentDescription {
	
	public enum ConfigurationPolicy {
		OPTIONAL("optional"),
		REQUIRE("require"),
		IGNORE("ignore");

		private final String	value;

		ConfigurationPolicy(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}
		
		public static ConfigurationPolicy toConfigurationPolicy(String v){
			for(int i=0;i<ConfigurationPolicy.values().length;i++){
				if(ConfigurationPolicy.values()[i].toString().equals(v)){
					return ConfigurationPolicy.values()[i];
				}
			}
			return null;
		}
	}
	
	private String name;
	private String clazz;
	
	private boolean enabled = true;
	private boolean factory = false;
	private boolean immediate = false;
	
	private String pid = null;
	private ConfigurationPolicy policy = ConfigurationPolicy.OPTIONAL;
	
	private String activate;
	private String deactivate;
	private String modified;
	
	private List<ServiceDescription> services;
	private List<ReferenceDescription> references;
	
	private Dictionary<String, Object> properties;
	
	public ComponentDescription(){
		this.services = new ArrayList<ServiceDescription>();
		this.references = new ArrayList<ReferenceDescription>();
		this.properties = new Hashtable<String, Object>();
	}
	
	public String getName(){
		return name;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public String getImplementationClass(){
		return clazz;
	}
	
	public void setImplementationClass(String clazz){
		this.clazz = clazz;
	}
	
	public boolean isEnabled(){
		return enabled;
	}
	
	public void setEnabled(boolean b){
		this.enabled = b;
	}
	
	public boolean isFactory(){
		return factory;
	}
	
	public void setFactory(boolean b){
		this.factory = b;
	}
	
	public boolean isImmediate(){
		return immediate;
	}
	
	public void setImmediate(boolean b){
		this.immediate = b;
	}
	
	public String getConfigurationPID(){
		if(pid!=null){
			return pid;
		} else if(name!=null){
			return name;
		} else {
			return clazz;
		}
	}
	
	public void setConfigurationPID(String pid){
		this.pid = pid;
	}
	
	public ConfigurationPolicy getConfigurationPolicy(){
		return policy;
	}
	
	public void setConfigurationPolicy(ConfigurationPolicy p){
		this.policy = p;
	}
	
	public String getActivate(){
		return activate;
	}
	
	public void setActivate(String activate){
		this.activate = activate;
	}

	public String getDeactivate(){
		return deactivate;
	}
	
	public void setDeactivate(String deactivate){
		this.deactivate = deactivate;
	}
	
	public String getModified(){
		return modified;
	}
	
	public void setModified(String modified){
		this.modified = modified;
	}
	
	public void addService(ServiceDescription s){
		this.services.add(s);
	}
	
	public List<ServiceDescription> getServices(){
		return Collections.unmodifiableList(services);
	}
	
	public void addReference(ReferenceDescription r){
		this.references.add(r);
	}
	
	public List<ReferenceDescription> getReferences(){
		return Collections.unmodifiableList(references);
	}
	
	public void setProperty(String key, Object value){
		this.properties.put(key, value);
	}
	
	public Dictionary<String, Object> getProperties(){
		return properties;
	}
}
