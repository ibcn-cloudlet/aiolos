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
package be.iminds.aiolos.deployment;

import org.osgi.framework.Version;
import org.osgi.resource.Requirement;

import be.iminds.aiolos.resource.CapabilityRequirementImpl;

/**
 * Helper class to build simple requirements to resolve bundles.
 */
public class RequirementBuilder {

	public static Requirement buildComponentNameRequirement(final String componentName){
		String namespace = "osgi.identity";
		CapabilityRequirementImpl r = new CapabilityRequirementImpl(namespace, null);
		r.addDirective("filter", String.format("(%s=%s)", namespace, componentName));
		return r;
	}
	
	public static Requirement buildPackageNameRequirement(final String packageName){
		String namespace = "osgi.wiring.package";
		CapabilityRequirementImpl r = new CapabilityRequirementImpl(namespace, null);
		r.addDirective("filter", String.format("(%s=%s)", namespace, packageName));
		return r;
	}
	
	public static Requirement buildComponentNameRequirement(final String componentName, final String version){
		String namespace = "osgi.identity";
		CapabilityRequirementImpl r = new CapabilityRequirementImpl(namespace, null);
		r.addDirective("filter", String.format("(&(%s=%s)%s)", namespace, componentName, buildVersionFilter(version)));

		return r;
	}
	
	public static Requirement buildPackageNameRequirement(final String packageName, final String version){
		String namespace = "osgi.wiring.package";
		CapabilityRequirementImpl r = new CapabilityRequirementImpl(namespace, null);
		r.addDirective("filter", String.format("(&(%s=%s)%s)", namespace, packageName, buildVersionFilter(version)));
		return r;
	}
	
	private static String buildVersionFilter(String version){
		String s ="";
		if(version.startsWith("[")){
			s+="(&";
			Version v = new Version(version.substring(1, version.indexOf(",")));
			s+=String.format("(version>=%s)", v.toString());
		} else if(version.startsWith("(")){
			s+="(&";
			Version v = new Version(version.substring(1, version.indexOf(",")));
			s+=String.format("(&(version>=%s)(!(version=%s)))", v.toString(), v.toString());
		} else {
			Version v = new Version(version);
			s=String.format("(version=%s)", v.toString());
		}
		if(version.endsWith(")")){
			Version v = new Version(version.substring(version.indexOf(",")+1, version.length()-1));
			s+=String.format("(!(version>=%s))", v.toString());
			s+=")";
		} else if (version.endsWith("]")){
			Version v = new Version(version.substring(version.indexOf(",")+1, version.length()-1));
			s+=String.format("(|(!(version>=%s))(version=%s))", v.toString(), v.toString());
			s+=")";
		}
		return s;
	}
}
