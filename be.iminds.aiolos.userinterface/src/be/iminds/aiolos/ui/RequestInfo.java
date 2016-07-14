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
package be.iminds.aiolos.ui;

import javax.servlet.http.HttpServletRequest;

public class RequestInfo
{
    public final String extension;

    protected RequestInfo( final HttpServletRequest request, final String LABEL )
    {
        String info = request.getPathInfo();
        // remove label and starting slash
    	info = info.substring(LABEL.length() + 1);

        // get extension
        if ( info.endsWith(".json") )
        {
            extension = "json";
            info = info.substring(0, info.length() - 5);
        } else if ( info.endsWith(".nfo") ) 
        {
        	extension = "nfo";
        	info = info.substring(0, info.length() - 4);
        }
        else
        {
            extension = "html";
        }

        request.setAttribute(NodeServlet.class.getName(), this);
    }

}
