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
package be.iminds.aiolos.rsa.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.kxml2.io.KXmlParser;
import org.osgi.framework.Bundle;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.xmlpull.v1.XmlPullParser;

import be.iminds.aiolos.rsa.Activator;

/**
 * Utility class that parses a EndpointDescription XML
 */
public class EndpointDescriptionParser {

	public static List<EndpointDescription> parseEndpointDescriptions(Bundle b) {
		List<EndpointDescription> endpointDescriptions = new ArrayList<EndpointDescription>();

		String xmlLocations = (String) b.getHeaders().get("Remote-Service");

		if(xmlLocations!=null){
			StringTokenizer st = new StringTokenizer(xmlLocations, ",");
			while (st.hasMoreElements()) {
				String xmlLocation = st.nextToken();
				if (xmlLocation.endsWith("/")) {
					// scan the directory for .xml files
					Enumeration<String> paths = b.getEntryPaths(xmlLocation);
					while(paths.hasMoreElements()){
						String path = paths.nextElement();
						if(path.endsWith(".xml")){
							URL entry = b.getEntry(path);
							if (entry != null) {
								try {
									InputStream stream = entry.openStream();
									endpointDescriptions.addAll(parseEndpointDescriptions(stream));
								} catch(Exception e){
									Activator.logger.log(LogService.LOG_ERROR, "Invalid EndpointDescription in "+b.getSymbolicName()+": "+e.getMessage(),e);
								}
							}
						}
					}
	
				} else if (xmlLocation.contains("*")) {
					// path with wildcards, use findentries
					int i = xmlLocation.lastIndexOf("/");
					String path = xmlLocation.substring(0, i);
					String filePattern = xmlLocation.substring(i+1);
					Enumeration<URL> paths = b.findEntries(path, filePattern, true);
					while(paths.hasMoreElements()){
						URL entry = b.getEntry(path);
						if (entry != null) {
							try {
								InputStream stream = entry.openStream();
								endpointDescriptions.addAll(parseEndpointDescriptions(stream));
							} catch(Exception e){
								Activator.logger.log(LogService.LOG_ERROR, "Invalid EndpointDescription in "+b.getSymbolicName()+": "+e.getMessage(),e);
							}
						}
					}
				} else {
					// complete path
					String path = xmlLocation;
					URL entry = b.getEntry(path);
					if (entry != null) {
						try {
							InputStream stream = entry.openStream();
							endpointDescriptions.addAll(parseEndpointDescriptions(stream));
						} catch(Exception e){
							Activator.logger.log(LogService.LOG_ERROR, "Invalid EndpointDescription in "+b.getSymbolicName()+": "+e.getMessage(),e);
						}
					}
				}			
			}

		}
		return endpointDescriptions;
	}

	private static List<EndpointDescription> parseEndpointDescriptions(InputStream stream) throws Exception {
		List<EndpointDescription> endpointDescriptions = new ArrayList<EndpointDescription>();
		
		BufferedReader in = new BufferedReader(new InputStreamReader(stream));
		KXmlParser parser = new KXmlParser();
		parser.setInput(in);

		parser.nextTag();
		parser.require(XmlPullParser.START_TAG, null, "endpoint-descriptions");
		
		while (parser.getEventType() != XmlPullParser.END_DOCUMENT) { // parser.nextTag()
			if (parser.getEventType() == XmlPullParser.START_TAG) {
				if (parser.getName().equals("endpoint-description")) {
					EndpointDescription ed = parseEndpointDescription(parser);
					endpointDescriptions.add(ed);
				}
			} 
			parser.next();
		}
		
		return endpointDescriptions;
	}
	
	private static EndpointDescription parseEndpointDescription(KXmlParser parser) throws Exception{
		
		Map<String, Object> props = new HashMap<String, Object>();
		
		String propertyName = null;
		String propertyValueType = null;
		Object propertyValue = null;
		
		parser.next();
		while(parser.getEventType()!=XmlPullParser.END_DOCUMENT){
			if (parser.getEventType() == XmlPullParser.START_TAG) {
				if(parser.getName().equals("property")){
					propertyName = parser.getAttributeValue(null, "name");
					propertyValueType = parser.getAttributeValue(null, "value-type");
					propertyValue = parseValue(parser.getAttributeValue(null, "value"), propertyValueType);
				} else if(parser.getName().equals("array")){
					propertyValue = parseArray(parser, propertyValueType);
				} else if(parser.getName().equals("list")){
					propertyValue = parseList(parser, propertyValueType);
				} else if(parser.getName().equals("set")){
					propertyValue = parseSet(parser, propertyValueType);
				} else if(parser.getName().equals("xml")){
					propertyValue = parseXml(parser, propertyValueType);
				} 
			} else if(parser.getEventType() == XmlPullParser.END_TAG){
				if(parser.getName().equals("property")){
					props.put(propertyName, propertyValue);
				} else if(parser.getName().equals("endpoint-description")){
					break;
				}
			}
			parser.next();
		}
		return new EndpointDescription(props);
	}
	
	private static Object parseValue(String value, String valueType){
		if(value==null){
			return null;
		} else if(valueType==null || valueType.equals("String")){
			return value;
		} else if(valueType.equals("long") || valueType.equals("Long")){
			return Long.parseLong(value);
		} else if(valueType.equals("double") || valueType.equals("Double")){
			return Double.parseDouble(value);
		} else if(valueType.equals("float") || valueType.equals("Float")){
			return Float.parseFloat(value);
		} else if(valueType.equals("int") || valueType.equals("Integer")){
			return Integer.parseInt(value);
		} else if(valueType.equals("byte") || valueType.equals("Byte")){
			return Byte.parseByte(value);
		} else if(valueType.equals("char") || valueType.equals("Character")){
			return value.charAt(0);
		} else if(valueType.equals("boolean") || valueType.equals("Boolean")){
			return Boolean.parseBoolean(value);
		} else if(valueType.equals("short") || valueType.equals("Short")){
			return Short.parseShort(value);
		} 
		// else error?
		return null;
	}
	
	private static Object parseArray(KXmlParser parser, String valueType) throws Exception {
		
		ArrayList<String> values = new ArrayList<String>();
		
		parser.next();
		while(parser.getEventType()!=XmlPullParser.END_DOCUMENT){
			if (parser.getEventType() == XmlPullParser.START_TAG) {
				if(parser.getName().equals("value")){
					values.add(parser.nextText());
				} 
			} else if(parser.getEventType() == XmlPullParser.END_TAG){
				if(parser.getName().equals("array")){
					break;
				} 
			}
			parser.next();
		}
		int count = values.size();
		if(valueType==null || valueType.equals("String")){
			String[] array = new String[count];
			for(int i=0;i<count;i++){
				array[i] = values.get(i);
			}
			return array;
		} else if(valueType.equals("long")){
			long[] array = new long[count];
			for(int i=0;i<count;i++){
				array[i] = Long.parseLong(values.get(i));
			}
			return array;		
		} else if(valueType.equals("Long")){
			Long[] array = new Long[count];
			for(int i=0;i<count;i++){
				array[i] = Long.parseLong(values.get(i));
			}
			return array;	
		} else if(valueType.equals("double")){
			double[] array = new double[count];
			for(int i=0;i<count;i++){
				array[i] = Double.parseDouble(values.get(i));
			}
			return array;
		} else if(valueType.equals("Double")){
			Double[] array = new Double[count];
			for(int i=0;i<count;i++){
				array[i] = Double.parseDouble(values.get(i));
			}
			return array;
		} else if(valueType.equals("float")){
			float[] array = new float[count];
			for(int i=0;i<count;i++){
				array[i] = Float.parseFloat(values.get(i));
			}
			return array;
		} else if(valueType.equals("Float")){
			Float[] array = new Float[count];
			for(int i=0;i<count;i++){
				array[i] = Float.parseFloat(values.get(i));
			}
			return array;
		} else if(valueType.equals("int")){
			int[] array = new int[count];
			for(int i=0;i<count;i++){
				array[i] = Integer.parseInt(values.get(i));
			}
			return array;
		} else if(valueType.equals("Integer")){
			Integer[] array = new Integer[count];
			for(int i=0;i<count;i++){
				array[i] = Integer.parseInt(values.get(i));
			}
			return array;
		} else if(valueType.equals("byte")){
			byte[] array = new byte[count];
			for(int i=0;i<count;i++){
				array[i] = Byte.parseByte(values.get(i));
			}
			return array;
		} else if(valueType.equals("Byte")){
			Byte[] array = new Byte[count];
			for(int i=0;i<count;i++){
				array[i] = Byte.parseByte(values.get(i));
			}
			return array;
		} else if(valueType.equals("char")){
			char[] array = new char[count];
			for(int i=0;i<count;i++){
				array[i] = values.get(i).charAt(0);
			}
			return array;
		} else if(valueType.equals("Character")){
			Character[] array = new Character[count];
			for(int i=0;i<count;i++){
				array[i] = values.get(i).charAt(0);
			}
			return array;
		} else if(valueType.equals("boolean")){
			boolean[] array = new boolean[count];
			for(int i=0;i<count;i++){
				array[i] = Boolean.parseBoolean(values.get(i));
			}
			return array;
		} else if(valueType.equals("Boolean")){
			Boolean[] array = new Boolean[count];
			for(int i=0;i<count;i++){
				array[i] = Boolean.parseBoolean(values.get(i));
			}
			return array;	
		} else if(valueType.equals("short")){
			short[] array = new short[count];
			for(int i=0;i<count;i++){
				array[i] = Short.parseShort(values.get(i));
			}
			return array;	
		} else if(valueType.equals("Short")){
			Short[] array = new Short[count];
			for(int i=0;i<count;i++){
				array[i] = Short.parseShort(values.get(i));
			}
			return array;
		} 
		return null;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Object parseList(KXmlParser parser, String valueType) throws Exception{
		ArrayList list = null;
		if(valueType==null || valueType.equals("String")){
			list = new ArrayList<String>();
		} else if(valueType.equals("long") || valueType.equals("Long")){
			list = new ArrayList<Long>();
		} else if(valueType.equals("double") || valueType.equals("Double")){
			list = new ArrayList<Double>();
		} else if(valueType.equals("float") || valueType.equals("Float")){
			list = new ArrayList<Float>();
		} else if(valueType.equals("int") || valueType.equals("Integer")){
			list = new ArrayList<Integer>();
		} else if(valueType.equals("byte") || valueType.equals("Byte")){
			list = new ArrayList<Byte>();
		} else if(valueType.equals("char") || valueType.equals("Character")){
			list = new ArrayList<Character>();
		} else if(valueType.equals("boolean") || valueType.equals("Boolean")){
			list = new ArrayList<Boolean>();
		} else if(valueType.equals("short") || valueType.equals("Short")){
			list = new ArrayList<Short>();
		} 
		
		parser.next();
		while(parser.getEventType()!=XmlPullParser.END_DOCUMENT){
			if (parser.getEventType() == XmlPullParser.START_TAG) {
				if(parser.getName().equals("value")){
					list.add(parseValue(parser.nextText(), valueType));
				}
			} else if(parser.getEventType() == XmlPullParser.END_TAG){
				if(parser.getName().equals("list")){
					break;
				} 
			}
			parser.next();
		}
		
		return list;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Object parseSet(KXmlParser parser, String valueType) throws Exception{
		HashSet set = null;
		if(valueType==null || valueType.equals("String")){
			set = new HashSet<String>();
		} else if(valueType.equals("long") || valueType.equals("Long")){
			set = new HashSet<Long>();
		} else if(valueType.equals("double") || valueType.equals("Double")){
			set = new HashSet<Double>();
		} else if(valueType.equals("float") || valueType.equals("Float")){
			set = new HashSet<Float>();
		} else if(valueType.equals("int") || valueType.equals("Integer")){
			set = new HashSet<Integer>();
		} else if(valueType.equals("byte") || valueType.equals("Byte")){
			set = new HashSet<Byte>();
		} else if(valueType.equals("char") || valueType.equals("Character")){
			set = new HashSet<Character>();
		} else if(valueType.equals("boolean") || valueType.equals("Boolean")){
			set = new HashSet<Boolean>();
		} else if(valueType.equals("short") || valueType.equals("Short")){
			set = new HashSet<Short>();
		} 
		
		parser.next();
		while(parser.getEventType()!=XmlPullParser.END_DOCUMENT){
			if (parser.getEventType() == XmlPullParser.START_TAG) {
				if(parser.getName().equals("value")){
					set.add(parseValue(parser.nextText(), valueType));
				} 
			} else if(parser.getEventType() == XmlPullParser.END_TAG){
				if(parser.getName().equals("set")){
					break;
				} 
			}
			parser.next();
		}
		
		return set;
	}
	
	private static Object parseXml(KXmlParser parser, String valueType) throws Exception{
		// TODO not implemented 
		return null;
	}
	
	
	// test the parser...
	public static void main(String[] args){
		try {
			InputStream input = new FileInputStream(new File("remote-services.xml"));
			parseEndpointDescriptions(input);
		} catch(Exception e){
			e.printStackTrace();
		}
	}
}
