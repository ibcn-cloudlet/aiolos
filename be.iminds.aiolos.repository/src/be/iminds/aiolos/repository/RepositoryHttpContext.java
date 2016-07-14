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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.http.HttpContext;

/**
 * {@link HttpContext} that provides HTTP access to repository bundles
 * hosted on the local filesystem.
 */
public class RepositoryHttpContext implements HttpContext {

	@Override
	public boolean handleSecurity(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		// for now no security ... could be of use later on
		return true;
	}

	@Override
	public URL getResource(String name) {
		try {
			if(name.endsWith(".xml")||name.endsWith(".jar")){
				return new URL(name);
			} else {
				return null;
			}
		} catch(MalformedURLException e){
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public String getMimeType(String name) {
		if(name.endsWith(".xml"))
			return "text/xml";
		else 
			return "application/octed-stream";
	}

}
