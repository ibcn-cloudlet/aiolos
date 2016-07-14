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
package be.iminds.aiolos.repository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.kxml2.io.KXmlParser;
import org.osgi.framework.Version;
import org.osgi.resource.Resource;
import org.xmlpull.v1.XmlPullParser;

import be.iminds.aiolos.resource.CapabilityRequirementImpl;
import be.iminds.aiolos.resource.ResourceImpl;

/**
 * Parses the index.xml of the repository according to the OSGi Repository spec.
 */
public class IndexParser {

	private static final String TAG_REPOSITORY = "repository";
	private static final String TAG_RESOURCE = "resource";
	private static final String TAG_CAPABILITY = "capability";
	private static final String TAG_REQUIREMENT = "requirement";
	private static final String TAG_ATTRIBUTE = "attribute";
	private static final String TAG_DIRECTIVE = "directive";

	private static final String ATTR_NAMESPACE = "namespace";

	private static final String ATTR_NAME = "name";
	private static final String ATTR_VALUE = "value";
	private static final String ATTR_TYPE = "type";

	// publicURL can be different from the URL that needs to be parsed 
	// e.g. in cloud environment a locally hosted repo can only be accessed using the private ip address
	// but the vm cannot access its own public ip 
	public static RepositoryImpl parseIndex(URL indexUrl, String publicURL) throws Exception {
		RepositoryImpl repository = null;
		
		BufferedReader in = new BufferedReader(new InputStreamReader(indexUrl.openStream()));
		KXmlParser parser = new KXmlParser();
		parser.setInput(in);
	
		String repoName = null;
		List<Resource> repoResources = new ArrayList<Resource>();
		
		ResourceImpl resource = null;
		CapabilityRequirementImpl capreq = null;
		
		int state;
		while((state = parser.next()) != XmlPullParser.END_DOCUMENT){
			String element = parser.getName();
			switch(state){
			case XmlPullParser.START_TAG:
				if(element.equals(TAG_REPOSITORY)){
					repoName = parser.getAttributeValue(null, ATTR_NAME);
				} else if(element.equals(TAG_RESOURCE)){
					resource = new ResourceImpl(publicURL);
				} else if(element.equals(TAG_CAPABILITY) || element.equals(TAG_REQUIREMENT)){
					String namespace = parser.getAttributeValue(null, ATTR_NAMESPACE);
					capreq = new CapabilityRequirementImpl(namespace, resource);
				} else if(element.equals(TAG_DIRECTIVE)){
					String key = parser.getAttributeValue(null, ATTR_NAME);
					String directive = parser.getAttributeValue(null, ATTR_VALUE);
					capreq.addDirective(key, directive);
				} else if(element.equals(TAG_ATTRIBUTE)){
					String key = parser.getAttributeValue(null, ATTR_NAME);
					String attribute = parser.getAttributeValue(null, ATTR_VALUE);
					String type = parser.getAttributeValue(null, ATTR_TYPE);
					Object attr = null;
					if(type==null){
						attr = attribute;
					} else if(type.equals("Long")){
						attr = Long.parseLong(attribute);
					} else if(type.equals("Version")){
						attr = new Version(attribute);
					}
					capreq.addAttribute(key, attr);
				}
				break;
			case XmlPullParser.END_TAG:
				if(element.equals(TAG_REPOSITORY)){
					repository = new RepositoryImpl(repoName, publicURL, repoResources);
				} else if(element.equals(TAG_RESOURCE)){
					repoResources.add(resource);
				} else if(element.equals(TAG_CAPABILITY)){ 
					resource.addCapability(capreq);
				} else if(element.equals(TAG_REQUIREMENT)){
					resource.addRequirement(capreq);
				}
				break;
			}
		}
		return repository;
	}
	
}
