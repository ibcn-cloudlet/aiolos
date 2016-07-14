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
package be.iminds.aiolos.cloud.jclouds;

import static org.jclouds.scriptbuilder.domain.Statements.exec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadataBuilder;
import org.jclouds.compute.domain.OperatingSystem;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.predicates.NodePredicates;
import org.jclouds.compute.predicates.OperatingSystemPredicates;
import org.jclouds.io.Payload;
import org.jclouds.io.payloads.ByteSourcePayload;
import org.jclouds.scriptbuilder.ScriptBuilder;
import org.jclouds.scriptbuilder.domain.Statement;
import org.jclouds.ssh.SshClient;
import org.jclouds.sshj.config.SshjSshClientModule;
import org.osgi.service.log.LogService;

import be.iminds.aiolos.cloud.AbstractCloudManager;
import be.iminds.aiolos.cloud.Activator;
import be.iminds.aiolos.cloud.api.CloudException;
import be.iminds.aiolos.cloud.api.CloudManager;
import be.iminds.aiolos.cloud.api.VMInstance;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.inject.Module;

/**
 * Implementation of the {@link CloudManager} interface 
 * using jClouds
 *
 */
public abstract class CloudManagerImplJClouds extends AbstractCloudManager implements Closeable {

	protected static final String PROPERTY_GROUP 			= "vmgroup";
	protected static final String PROPERTY_IMAGE_ID 		= "imageid";
	protected static final String PROPERTY_NETWORK_IDS 	= "network.ids";
	protected static final String PROPERTY_MIN_RAM 		= "image.ram.min";
	protected static final String PROPERTY_MIN_DISK 		= "vm.disk.min";
	protected static final String PROPERTY_MIN_VCPU 		= "vm.vcpu.min";
	protected static final String PROPERTY_PUBLIC_KEY 	= "publickey.file";
	protected static final String PROPERTY_ADMIN_ACCESS 	= "admin.access";
	protected static final String PROPERTY_SSH_TIMEOUT	= "vm.ssh.timeout";
	protected static final String PROPERTY_PROXY			= "vm.http.proxy";
	protected static final String PROPERTY_NOPROXY		= "vm.http.noproxy";
	protected static final String PROPERTY_INSTALL_PKGS	= "vm.install.packages";
	protected static final String PROPERTY_PORTS_INBOUND	= "ports.inbound";
	protected static final String PROPERTY_PORTS_INBOUND_EXTRA	= "ports.inbound.extra";

	
	protected static final String APT_GET_INSTALL			= "sudo -E apt-get update -qq && sudo -E apt-get install -f -y -qq --force-yes";
	protected static final String YUM_INSTALL				= "sudo -E yum --quiet --nogpgcheck -y install";
	protected static final String ZYPPER_INSTALL			= "sudo -E zypper install";
	
	protected ComputeService computeService;
	protected Map<String, VMInstance> instances = Collections.synchronizedMap(new HashMap<String, VMInstance>());
	
	protected String vmgroup = "aiolos";
	
	protected File publicKeyFile;
	protected boolean adminAccess = true;
	protected String proxy;
	protected String noproxy;
	protected String installPkgs;
	protected int sshTimeout = 300;
	protected int[] inboundPorts = new int[]{22, 80, 8080, 8081, 443, 9278};
	

    // provide the correct ComputeServiceContext builder
    protected abstract ContextBuilder getContextBuilder();
    
    // provide the correct VM template
    protected abstract Template getTemplate();
    
	// configure and instantiate the computeService
    @SuppressWarnings("unchecked")
	public void configure(Dictionary<String,?> properties){
		proxy				= (String) properties.get(PROPERTY_PROXY);
		noproxy				= (String) properties.get(PROPERTY_NOPROXY);
		installPkgs			= (String) properties.get(PROPERTY_INSTALL_PKGS);
		adminAccess			= (properties.get(PROPERTY_ADMIN_ACCESS) == null ? adminAccess : Boolean.parseBoolean((String) properties.get(PROPERTY_ADMIN_ACCESS)));
		vmgroup 			= (properties.get(PROPERTY_GROUP) == null ? vmgroup : (String) properties.get(PROPERTY_GROUP));
		sshTimeout			= (properties.get(PROPERTY_SSH_TIMEOUT) == null ? sshTimeout : Integer.parseInt((String) properties.get(PROPERTY_SSH_TIMEOUT)));
		
		String inports = (String) properties.get(PROPERTY_PORTS_INBOUND);
		if(inports!=null){
			String[] split = inports.split(",");
			inboundPorts = new int[split.length];
			for(int i=0;i<split.length;i++){
				inboundPorts[i] = Integer.parseInt(split[i]);
			}
		}
		String inportsExtra = (String) properties.get(PROPERTY_PORTS_INBOUND_EXTRA);
		if(inportsExtra!=null){
			String[] split = inportsExtra.split(",");
			int o = inboundPorts.length;
			inboundPorts = Arrays.copyOf(inboundPorts, o+split.length);
			for(int i=0;i<split.length;i++){
				inboundPorts[o+i] = Integer.parseInt(split[i]);
			}
		}

		
		String fileName  	= (String) properties.get(PROPERTY_PUBLIC_KEY);
		if (fileName != null) {
			publicKeyFile = new File(fileName);
			if (!publicKeyFile.exists())
				publicKeyFile = new File(System.getProperty("user.home") + "/.ssh/" + fileName);
			if (!publicKeyFile.exists())
				publicKeyFile = new File("/tmp/AIOLOS/tools/resources/shared.pub"); // extra for vms created by cloudmanager (hack)
		}
		
    	this.computeService = initComputeService();	
    }
    
    private ComputeService initComputeService(){
    	// injecting a ssh implementation
        Iterable<Module> modules = null;
        try {
        	Module loggingModule = (Module) Class.forName("org.jclouds.logging.slf4j.config.SLF4JLoggingModule").newInstance();
        	modules = ImmutableSet.<Module> of(
        			new SshjSshClientModule()
        			,loggingModule);
        } catch(Exception e){
        	Activator.logger.log(LogService.LOG_DEBUG, "No slf4j logging module available");
        }
        
        if(modules==null)
        	modules = ImmutableSet.<Module> of(new SshjSshClientModule());
    	
    	ContextBuilder builder = getContextBuilder();
    	builder.modules(modules);
    	
    	Activator.logger.log(LogService.LOG_DEBUG, String.format(">> initializing %s%n", builder.getApiMetadata()));
    	
    	return builder.buildView(ComputeServiceContext.class).getComputeService();
    }
		
    @Override
	public void close() throws IOException {
    	this.computeService.getContext().close();
	}
    
    private String getInstallCommand(OperatingSystem os) {
    	if (os == null || OperatingSystemPredicates.supportsApt().apply(os))
    		return APT_GET_INSTALL;
    	else if (OperatingSystemPredicates.supportsYum().apply(os))
    		return YUM_INSTALL;
    	else if (OperatingSystemPredicates.supportsZypper().apply(os))
    		return ZYPPER_INSTALL;
    	else
    		throw new IllegalArgumentException("don't know how to handle" + os.toString());
    }
    
    private Statement installJavaIfNotPresent(OperatingSystem os) {
    	if (os == null || OperatingSystemPredicates.supportsApt().apply(os))
    		return installIfNotPresent("java", "openjdk-7-jre", os);
    	else if (OperatingSystemPredicates.supportsYum().apply(os) || OperatingSystemPredicates.supportsZypper().apply(os))
    		return installIfNotPresent("java", "java-1_7_0-openjdk", os);
    	else
    		throw new IllegalArgumentException("don't know how to handle" + os.toString());
    }
    
    private Statement installIfNotPresent(String cmd, String pkgs, OperatingSystem os) {
    	if (cmd != null)
    		return exec(String.format("hash %s 2> /dev/null || (%s %s)", cmd, getInstallCommand(os), pkgs));
    	else
    		return exec(String.format("%s %s", getInstallCommand(os), pkgs));
    }
    
	private VMInstance provisionOSGiRuntime(String bndrun, List<String> resources, NodeMetadata node) throws Exception {
		
		int osgiPort = 9278;
		int httpPort = 8080;
		
		Activator.logger.log(LogService.LOG_INFO, "Waiting for node " + node.getHostname() + " to come online...");		
		SshClient sshClient = computeService.getContext().utils().sshForNode().apply(NodeMetadataBuilder.fromNodeMetadata(node).privateAddresses(Collections.<String> emptyList()).build());
		Activator.logger.log(LogService.LOG_INFO, "Node " + node.getHostname() + " online, provision OSGi");
		
		synchronized(this){
			try {
				sshClient.connect();
				
				Activator.logger.log(LogService.LOG_INFO, "Connected to node "+node.getHostname()+" at ip "+sshClient.getHostAddress()+", uploading necessary files...");
								
				// set build.bnd and copy ext folder
				sshClient.exec("mkdir -p /tmp/AIOLOS/cnf/ext");
				sshClient.exec("touch /tmp/AIOLOS/cnf/build.bnd");
				
				File ext = new File("../cnf/ext");
				for(String name : ext.list()){
					uploadFile(sshClient, "../cnf/ext/"+name, "/tmp/AIOLOS/cnf/ext/"+name);
				}
				
				// upload bnd
				String bnd = null;
				File bndDir = new File("../cnf/plugins/biz.aQute.bnd");
				for(String name : bndDir.list()){
					bnd = name;
					break;
				}
				if(bnd==null)
					throw new Exception("No bnd present...");
				
				uploadFile(sshClient, "../cnf/plugins/biz.aQute.bnd/"+bnd ,"/tmp/AIOLOS/cnf/plugins/biz.aQute.bnd/" + bnd);
				
				// upload bnd repository plugin TODO avoid hard coded url?
				uploadFile(sshClient, "../cnf/plugins/biz.aQute.repository/biz.aQute.repository-2.1.0.jar", "/tmp/AIOLOS/cnf/plugins/biz.aQute.repository/biz.aQute.repository-2.1.0.jar");
				
				
				// copy all bndrun files present (can be used for cloudmanagers on the new machine)
				sshClient.exec("mkdir -p /tmp/AIOLOS/tools");
				for(File file : getFilteredBndRunFiles()) {
					uploadFile(sshClient, file.getAbsolutePath(), "/tmp/AIOLOS/tools/"+file.getName());
				}
				
				// Set the rsa.ip property to the public ip of the VM
				try {
					Properties run = new Properties();
					run.load(new FileInputStream(new File(bndrun)));
					String runproperties=run.getProperty("-runproperties");
			
					// check for port properties
					try {
						Properties props = new Properties();
						props.load(new ByteArrayInputStream(runproperties.replaceAll(",", "\n").getBytes("UTF-8")));
						String osgiPortString = props.getProperty("rsa.port");
						if(osgiPortString!=null){
							osgiPort = Integer.parseInt(osgiPortString);
						}
						String httpPortString = props.getProperty("org.osgi.service.http.port");
						if(httpPortString!=null){
							httpPort = Integer.parseInt(httpPortString);
						}
					} catch(Exception e){}
					
					String publicIP = node.getPublicAddresses().iterator().next();
					String privateIP = node.getPrivateAddresses().iterator().next();
					if(runproperties==null)
						runproperties="rsa.ip="+publicIP+",private.ip="+privateIP+",public.ip="+publicIP;
					else
						runproperties+=",rsa.ip="+publicIP+",private.ip="+privateIP+",public.ip="+publicIP;
					run.put("-runproperties", runproperties);
					ByteArrayOutputStream bao = new ByteArrayOutputStream();
					run.store(bao, "bnd run configuration");
					bao.flush();
					ByteSource byteSource = ByteSource.wrap(bao.toByteArray());
					Payload payload = new ByteSourcePayload(byteSource);
					payload.getContentMetadata().setContentLength(byteSource.size());
					bao.close();
					sshClient.put("/tmp/AIOLOS/tools/" + bndrun, payload);
				} catch(Exception e){
					throw new Exception("Invalid bndrun configuration provided "+bndrun);
				}				
				
				// copy resources
				if(resources!=null){
					sshClient.exec("mkdir -p /tmp/AIOLOS/tools/resources");
					for(String r : resources ){
						File f = new File(r);
						ByteSource byteSource = Files.asByteSource(f);
						Payload payload = new ByteSourcePayload(byteSource);
						payload.getContentMetadata().setContentLength(byteSource.size());
						sshClient.put("/tmp/AIOLOS/tools/resources/"+f.getName(), payload);
					}
				}
				if (publicKeyFile != null && publicKeyFile.exists()) {
					try {
						uploadFile(sshClient, publicKeyFile.getAbsolutePath(), "/tmp/AIOLOS/tools/resources/shared.pub");
					} catch (IOException e) {}
		        }
				
				// upload repositories
				// TODO don't hard code this?
				uploadDir(sshClient, "../cnf/localrepo", "/tmp/AIOLOS/cnf/localrepo");
				uploadDir(sshClient, "../cnf/releaserepo", "/tmp/AIOLOS/cnf/releaserepo");
				uploadDir(sshClient, "../tools/generated/workspacerepo", "/tmp/AIOLOS/tools/generated/workspacerepo");
				
				// start 
				ByteSource byteSource = ByteSource.wrap(buildBndRunScript(bnd, bndrun, node.getOperatingSystem()).getBytes());
				Payload payload = new ByteSourcePayload(byteSource);
				payload.getContentMetadata().setContentLength(byteSource.size());
				sshClient.put("/tmp/AIOLOS/init.sh", payload);
				sshClient.exec("chmod a+x /tmp/AIOLOS/init.sh");
				
				Activator.logger.log(LogService.LOG_INFO, "Start OSGi on node "+node.getHostname()+ "  ...");
				ExecResponse response = sshClient.exec("nohup /tmp/AIOLOS/init.sh >> /tmp/AIOLOS/init.log 2>&1 < /dev/null");
				if (response.getExitStatus() != 0) {
					Activator.logger.log(LogService.LOG_ERROR, "Execution of script failed: " + response.getError());
				}
			} finally {
				if (sshClient != null)
				    sshClient.disconnect();
		    }
		}
		return new VMInstance(node.getId(), node.getUri(), node.getName(), node.getGroup(), node.getImageId()
    			, node.getStatus().name(), node.getHostname(), node.getPrivateAddresses(), node.getPublicAddresses()
    			, node.getHardware().getName(), osgiPort, httpPort);
	}
	
	private void uploadDir(SshClient ssh, String src, String dest) throws IOException {
		File dir = new File(src);
		if(dir.isDirectory()){
			ssh.exec("mkdir -p "+dest);
			for(File f : dir.listFiles()){
				if(f.isDirectory()){
					uploadDir(ssh, f.getAbsolutePath(), dest+"/"+f.getName());
				} else {
					uploadFile(ssh, f.getAbsolutePath(), dest+"/"+f.getName());
				}
			}
		}
	}
	
	private void uploadFile(SshClient ssh, String src, String dest) throws IOException {
		String dir = dest.substring(0, dest.lastIndexOf("/"));
		ssh.exec("mkdir -p "+dir);
		File srcFile = new File(src);
		ByteSource byteSource = Files.asByteSource(srcFile);
		Payload payload = new ByteSourcePayload(byteSource);
		payload.getContentMetadata().setContentLength(byteSource.size());
		ssh.put(dest, payload);
	}
	
	private String buildBndRunScript(String bnd, String bndrun, OperatingSystem os){
		ScriptBuilder scriptBuilder = new ScriptBuilder();
		
		if (proxy != null)
			scriptBuilder
	        	.addStatement(exec(String.format("export http_proxy=%s", proxy)))
	        	.addStatement(exec(String.format("export https_proxy=%s", proxy)))
	        	.addStatement(exec(String.format("export ftp_proxy=%s", proxy)));
        if (noproxy != null)
        	scriptBuilder
	        	.addStatement(exec(String.format("export no_proxy='%s'", noproxy)));
        
        if (installPkgs != null)
        	scriptBuilder
        		.addStatement(installIfNotPresent(null, installPkgs, os));
        
        // construct bnd run statement
        String run = "java -jar";
        if(proxy != null){
        	try {
				URI proxyURL = new URI(proxy);
				run+=" -Dhttp.proxyHost="+proxyURL.getHost();
				run+=" -Dhttp.proxyPort="+proxyURL.getPort();
			} catch (URISyntaxException e) {}
        }
        if(noproxy !=null){
        	run+=" -Dhttp.nonProxyHosts=\""+noproxy+"\"";
        }
        // TODO also handle http.proxyUser and http.proxyPassword
        run += " /tmp/AIOLOS/cnf/plugins/biz.aQute.bnd/" + bnd + " " + bndrun + " >> /tmp/AIOLOS/aiolos.log 2>&1 < /dev/null &";
        
        // Check curl and java. Install packages if if missing.
        scriptBuilder
        	.addStatement(installIfNotPresent("curl", "curl", os))
        	.addStatement(installJavaIfNotPresent(os))
        	
		// run AIOLOS
			.addStatement(exec("cd /tmp/AIOLOS/tools")) //change work directory
			.addStatement(exec(run));
					
		return scriptBuilder.render(org.jclouds.scriptbuilder.domain.OsFamily.UNIX);
	}

	@Override
	public VMInstance startVM(String bndrun, List<String> resources) throws CloudException, TimeoutException {
		List<VMInstance> vms = startVMs(bndrun, resources, 1);
		if(vms.size()>0)
			return vms.iterator().next();
		else 
			throw new CloudException("Failed to start instance");
	}
	
	@Override
	public List<VMInstance> startVMs(String bndrun, List<String> resources, int count) throws CloudException, TimeoutException {
		List<VMInstance> vms = new ArrayList<VMInstance>();
		int retry = 0;
		int noVMs = 0;
		Activator.logger.log(LogService.LOG_INFO, String.format("Start VM(s), this could take a while (Admin Install = %s).", adminAccess));
		long t1 = System.currentTimeMillis();
		while(noVMs < count && retry < 3){
			Set<? extends NodeMetadata> nodes = null;
			try {
				nodes = this.computeService.createNodesInGroup(vmgroup, count-noVMs, getTemplate());
			} catch(RunNodesException e){
				Activator.logger.log(LogService.LOG_ERROR, "createNodesInGroup failed", e);
				nodes = e.getSuccessfulNodes();
				Activator.logger.log(LogService.LOG_ERROR, String.format("%d of %d nodes successfully started.", nodes.size() + noVMs, count));
			}
			for (NodeMetadata node : nodes) {
				try {
					Activator.logger.log(LogService.LOG_INFO, String.format("Node %s with credentials %s. Provision OSGi runtime on node %s.", node.getHostname(), node.getCredentials(), node.getHostname()));
					VMInstance vm = provisionOSGiRuntime(bndrun, resources, node);
					noVMs++;
					vms.add(vm);
					instances.put(vm.getId(), vm);
				} catch(Exception e){
					Activator.logger.log(LogService.LOG_INFO, e.getLocalizedMessage() + "\nProvisioning OSGi on node "+node.getHostname()+" failed, retry.");
					this.computeService.destroyNode(node.getId());
				}
			}
			retry++;
		}
		long t2 = System.currentTimeMillis();
		Activator.logger.log(LogService.LOG_INFO, "Started "+noVMs+" VM(s), which took "+(t2-t1)+" ms. ("+(retry-1)+" retries)");
		
		return vms;
	}

	@Override
    public VMInstance stopVM(String id) {
		VMInstance vm = instances.remove(id);
		if(vm!=null){
			this.computeService.destroyNodesMatching(NodePredicates.<NodeMetadata>withIds(id));
		}
        return vm;
    }
    
    @Override
	public List<VMInstance> stopVMs(String[] ids) {
		List<VMInstance> vms = new ArrayList<VMInstance>();
		for(String id : ids){
			VMInstance vm = stopVM(id);
			if(vm!=null)
				vms.add(vm);
		}
		return vms;
	}
    
    @Override
    public List<VMInstance> stopVMs() {
    	// this will stop all VMs running ... is used to clean up cloud vms ...
    	// TODO is probably not the correct behavior?
    	this.computeService.destroyNodesMatching(NodePredicates.RUNNING);
		List<VMInstance> vms = new ArrayList<VMInstance>(instances.values());
		instances.clear();
		return vms;
    }

	@Override
	public List<VMInstance> listVMs() {
		List<VMInstance> vms = new ArrayList<VMInstance>(instances.values());
		return vms;
	}
}
