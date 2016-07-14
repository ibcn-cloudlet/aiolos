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
package be.iminds.aiolos.cloud.api;

import java.net.URI;
import java.util.Set;

/**
 * {@link VMInstance} describes a VM instance on the cloud.
 */
public class VMInstance {
	
	private String id;
	private URI uri;
	private String name;
	private String group;
	private String imageId;
	private String status;
	private String hostname;
	private Set<String> privateAddresses;
	private Set<String> publicAddresses;
	private String type;
	private int osgiPort;
	private int httpPort;
	
	public VMInstance(String id, URI uri, String name, String group,
			String image, String status, String hostname,
			Set<String> privateAddresses, Set<String> publicAddresses,
			String type, int osgiPort, int httpPort) {
		super();
		this.id = id;
		this.uri = uri;
		this.name = name;
		this.group = group;
		this.imageId = image;
		this.status = status;
		this.hostname = hostname;
		this.privateAddresses = privateAddresses;
		this.publicAddresses = publicAddresses;
		this.type = type;
		this.osgiPort = osgiPort;
		this.httpPort = httpPort;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the uri
	 */
	public URI getUri() {
		return uri;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the group
	 */
	public String getGroup() {
		return group;
	}

	/**
	 * @return the imageId
	 */
	public String getImage() {
		return imageId;
	}

	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @return the hostname
	 */
	public String getHostname() {
		return hostname;
	}

	/**
	 * @return the privateAddresses
	 */
	public Set<String> getPrivateAddresses() {
		return privateAddresses;
	}

	/**
	 * @return the publicAddresses
	 */
	public Set<String> getPublicAddresses() {
		return publicAddresses;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}
	
	public int getOsgiPort() {
		return osgiPort;
	}

	public int getHttpPort(){
		return httpPort;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("VMInstance [id=").append(id).append(", uri=")
				.append(uri).append(", name=").append(name).append(", group=")
				.append(group).append(", image=").append(imageId)
				.append(", status=").append(status).append(", hostname=")
				.append(hostname).append(", privateAddresses=")
				.append(privateAddresses).append(", publicAddresses=")
				.append(publicAddresses).append(", type=").append(type)
				.append(", osgiPort=").append(osgiPort)
				.append(", httpPort=").append(httpPort)
				.append("]");
		return builder.toString();
	}
}
