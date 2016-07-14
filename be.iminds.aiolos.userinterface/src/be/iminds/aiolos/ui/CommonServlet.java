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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;
import org.osgi.service.repository.Repository;
import org.osgi.util.tracker.ServiceTracker;

import be.iminds.aiolos.cloud.api.CloudManager;
import be.iminds.aiolos.platform.api.PlatformManager;

@SuppressWarnings("serial")
public abstract class CommonServlet extends HttpServlet {
	
	public final String LABEL;
	public final String CATEGORY;
	public final String TITLE;
	public final String CSS[];
	private final String TEMPLATE;
	protected final ServiceTracker<PlatformManager, PlatformManager> platformTracker;
	protected final ServiceTracker<Repository, Repository> repositoryTracker;
	protected final ServiceTracker<CloudManager, CloudManager> cloudTracker;

	protected final ExecutorService executor = Executors.newCachedThreadPool();
	protected boolean drawDetails = false;
	
	protected abstract void writeJSON(final Writer w, final Locale locale) throws IOException;
	
	public CommonServlet(String label, String title, String category, String[] css, String template, BundleContext bundleContext) {
		this.LABEL = label;
		this.TITLE = title;
		this.CATEGORY = category;
		this.CSS = css;
		this.TEMPLATE = readTemplateFile(template);
		this.platformTracker = new ServiceTracker<PlatformManager,PlatformManager>(bundleContext, PlatformManager.class, null);
		this.repositoryTracker = new ServiceTracker<Repository,Repository>(bundleContext, Repository.class, null);
		this.cloudTracker = new ServiceTracker<CloudManager,CloudManager>(bundleContext, CloudManager.class, null);
		
	}
	@Override
	public void destroy() {
		executor.shutdownNow();
		platformTracker.close();
		repositoryTracker.close();
		cloudTracker.close();
		super.destroy();
	}

	@Override
	public void init() throws ServletException {
		platformTracker.open();
		repositoryTracker.open();
		cloudTracker.open();
		super.init();
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// Not necessary for plugin only as standalone app.
		// doGetPlugin allways needed.
		// check whether we are not at .../{webManagerRoot}
        final String pathInfo = request.getPathInfo();
        if ( pathInfo == null || pathInfo.equals( "/" ) )
        {
            String path = request.getRequestURI();
            if ( !path.endsWith( "/" ) )
            {
                path = path.concat( "/" );
            }
            path = path.concat( LABEL );
            response.sendRedirect( path );
            return;
        }
        
        int slash = pathInfo.indexOf( "/", 1 );
        if ( slash < 2 )
        {
            slash = pathInfo.length();
        }
        
        final String label = pathInfo.substring( 1, slash );
        if ( label != null && label.startsWith(LABEL) )
        {
        	final RequestInfo reqInfo = new RequestInfo(request, LABEL);
    		if (reqInfo.extension.equals("html")) {
    			doGetPlugin(request, response);
    		} else if (reqInfo.extension.equals("json")) {
    			renderJSON(response, request.getLocale());
    		} 
        }
        else
        {
            response.sendError( HttpServletResponse.SC_NOT_FOUND );
        }
	}
	
	private void doGetPlugin(HttpServletRequest request, HttpServletResponse response) throws IOException {
		StringWriter sw = new StringWriter();
		writeJSON(sw, request.getLocale());
		
		Map<Object,Object> vars = getProperties(request);
		vars.put("__data__", sw.toString());
		vars.put("drawDetails", drawDetails);
		
		response.getWriter().print(TEMPLATE);
	}
	
	protected void renderJSON(HttpServletResponse response, Locale locale) throws IOException {
		response.setContentType( "application/json" );
        response.setCharacterEncoding( "UTF-8" );

        final PrintWriter pw = response.getWriter();
        writeJSON(pw, locale);
	}
	
	@SuppressWarnings("unchecked")
	public static Map<Object,Object> getProperties( final ServletRequest request )
    {
        final Object resolverObj = request.getAttribute( "felix.webconsole.variable.resolver" );
        if ( resolverObj instanceof Map<?,?> )
        {
            return ( Map<Object,Object> ) resolverObj;
        }

        final Map<Object,Object> resolver = new HashMap<Object,Object>();
        request.setAttribute( "felix.webconsole.variable.resolver" , resolver);
        return resolver;
    }
	
	/**
     * Called internally by {@link AbstractWebConsolePlugin} to load resources.
     *
     * This particular implementation depends on the label. As example, if the
     * plugin is accessed as <code>/system/console/abc</code>, and the plugin
     * resources are accessed like <code>/system/console/abc/res/logo.gif</code>,
     * the code here will try load resource <code>/res/logo.gif</code> from the
     * bundle, providing the plugin.
     *
     *
     * @param path the path to read.
     * @return the URL of the resource or <code>null</code> if not found.
     */
    protected URL getResource( String path )
    {
    	String labelRes = '/' + LABEL + '/';
        int labelResLen = labelRes.length() - 1;
        return ( path != null && path.startsWith( labelRes ) ) ? //
        		getClass().getResource( path.substring( labelResLen ) )
        		: null;
    }
    
    /**
     * Reads the <code>templateFile</code> as a resource through the class
     * loader of this class converting the binary data into a string using
     * UTF-8 encoding.
     * <p>
     * If the template file cannot read into a string and an exception is
     * caused, the exception is logged and an empty string returned.
     *
     * @param templateFile The absolute path to the template file to read.
     * @return The contents of the template file as a string or and empty
     *      string if the template file fails to be read.
     *
     * @throws NullPointerException if <code>templateFile</code> is
     *      <code>null</code>
     * @throws RuntimeException if an <code>IOException</code> is thrown reading
     *      the template file into a string. The exception provides the
     *      exception thrown as its cause.
     */
    protected final String readTemplateFile(final String templateFile)
    {
    	Class<? extends HttpServlet> clazz = this.getClass();
        InputStream templateStream = clazz.getResourceAsStream( templateFile );
        if ( templateStream != null )
        {
            try
            {
                String str = IOUtils.toString( templateStream, "UTF-8" ); //$NON-NLS-1$
                switch ( str.charAt(0) )
                { // skip BOM
                    case 0xFEFF: // UTF-16/UTF-32, big-endian
                    case 0xFFFE: // UTF-16, little-endian
                    case 0xEFBB: // UTF-8
                        return str.substring(1);
                }
                return str;
            }
            catch ( Exception e )
            {
                // don't use new Exception(message, cause) because cause is 1.4+
                throw new RuntimeException( "readTemplateFile: Error loading " + templateFile + ": " + e ); //$NON-NLS-1$ //$NON-NLS-2$
            }
            finally
            {
                IOUtils.closeQuietly( templateStream );
            }
        }

        // template file does not exist, return an empty string
        Activator.logger.log(LogService.LOG_ERROR, "readTemplateFile: File '" + templateFile + "' not found through class " + clazz ); //$NON-NLS-1$ //$NON-NLS-2$
        return ""; //$NON-NLS-1$
    }
}
