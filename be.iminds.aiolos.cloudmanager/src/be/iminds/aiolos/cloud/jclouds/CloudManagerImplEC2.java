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

import static org.jclouds.compute.config.ComputeServiceProperties.TIMEOUT_PORT_OPEN;
import static org.jclouds.openstack.nova.v2_0.config.NovaProperties.AUTO_ALLOCATE_FLOATING_IPS;
import static org.jclouds.openstack.nova.v2_0.config.NovaProperties.AUTO_GENERATE_KEYPAIRS;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Dictionary;
import java.util.Properties;

import org.jclouds.Constants;
import org.jclouds.ContextBuilder;
import org.jclouds.aws.ec2.reference.AWSEC2Constants;
import org.jclouds.compute.domain.Template;
import org.jclouds.scriptbuilder.domain.Statement;
import org.jclouds.scriptbuilder.domain.StatementList;
import org.jclouds.scriptbuilder.statements.login.AdminAccess;
import org.osgi.service.log.LogService;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;

import be.iminds.aiolos.cloud.Activator;
import be.iminds.aiolos.cloud.api.CloudManager;

/**
 * Implementation of the {@link CloudManager} interface 
 * for the OpenStack cloud 
 *
 */
public class CloudManagerImplEC2 extends CloudManagerImplJClouds {

	private static final String ACCESSKEYIDKEY = "accesskeyid";
	private static final String SECRETKEY = "secret";
	
	private static final String PROPERTY_IMAGE_ID 		= "imageid";
	private static final String PROPERTY_LOCATION 		= "location";
	private static final String PROPERTY_INSTANCE_TYPE  = "instance.type";
	
	private String accesskeyid;
	private String secret;
	
   	private String imageId = "ami-e84d8480";
	private	String locationId = "us-east-1";
	private String instanceType = "t1.micro";
	
    @SuppressWarnings("unchecked")
	public void configure(Dictionary<String,?> properties) {

		accesskeyid = (String) properties.get(ACCESSKEYIDKEY);
		secret = (String) properties.get(SECRETKEY);
		
		imageId = (String) properties.get(PROPERTY_IMAGE_ID);
		locationId = (String) properties.get(PROPERTY_LOCATION);
		instanceType = (String) properties.get(PROPERTY_INSTANCE_TYPE);
		
		super.configure(properties);
    }

    protected ContextBuilder getContextBuilder(){
    	Properties overrides = new Properties();
		// set AMI queries to nothing
		overrides.setProperty(AWSEC2Constants.PROPERTY_EC2_AMI_QUERY, "");
		overrides.setProperty(AWSEC2Constants.PROPERTY_EC2_CC_AMI_QUERY, "");
		
		overrides.setProperty(TIMEOUT_PORT_OPEN, Integer.toString(sshTimeout * 1000));
		overrides.setProperty(Constants.PROPERTY_CONNECTION_TIMEOUT, Integer.toString(0)); 
		
    	ContextBuilder builder = ContextBuilder.newBuilder("aws-ec2")
                .credentials(accesskeyid, secret)
                .overrides(overrides);
    	
    	return builder;
    }
    
    protected Template getTemplate() {
		Template template = computeService.templateBuilder()
				.imageId(locationId+"/"+imageId)
				.locationId(locationId)
				.hardwareId(instanceType).build();
		
		template.getOptions().inboundPorts(inboundPorts);

		if (publicKeyFile != null) {
			try {
				template.getOptions().authorizePublicKey(Files.toString(publicKeyFile, StandardCharsets.UTF_8));
			} catch (IOException e) {
				Activator.logger.log(LogService.LOG_ERROR, String.format("Public ssh key file (%s) does not exist.", publicKeyFile), e);
			}
        }
		
        ImmutableList.Builder<Statement> bootstrapBuilder = ImmutableList.<Statement>builder();
		if (adminAccess)
	        bootstrapBuilder.add(AdminAccess.standard());
		
        StatementList bootstrap = new StatementList(bootstrapBuilder.build());
        if (!bootstrap.isEmpty())
        	template.getOptions().runScript(bootstrap);
		
		// specify your own keypair for use in creating nodes
		//template.getOptions().as(AWSEC2TemplateOptions.class).keyPair("ec2");
		
		return template;
	}
    
   
}
