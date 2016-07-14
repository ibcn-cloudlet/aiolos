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
package be.iminds.aiolos.rsa.network;

import java.io.IOException;
import java.net.BindException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.log.LogService;

import be.iminds.aiolos.rsa.Activator;
import be.iminds.aiolos.rsa.network.api.MessageReceiver;
import be.iminds.aiolos.rsa.network.api.NetworkChannel;
import be.iminds.aiolos.rsa.network.api.NetworkChannelFactory;
import be.iminds.aiolos.rsa.util.URI;

/**
 * Factory for creating TCP Channels
 */
public class TCPChannelFactory implements NetworkChannelFactory{

	private String hostAddress = null;
	private String networkInterface = null;
	private boolean ipv6 = false;
	private int listeningPort = 9278;
	private TCPAcceptorThread thread;
	
	private Map<String, NetworkChannel> channels = new HashMap<String, NetworkChannel>();
	
	private MessageReceiver receiver;
	
	public TCPChannelFactory(MessageReceiver receiver, String ip, String networkInterface, int port, boolean ipv6){
		this.receiver = receiver;
		if(ip!=null){
			if(ip.contains(":")){
				this.hostAddress = "["+ip+"]";
			} else {
				this.hostAddress = ip;
			}
		}
		this.networkInterface = networkInterface;
		if(port!=-1)
			this.listeningPort = port;
		this.ipv6 = ipv6;
	}
	
	public void activate() throws IOException {
		thread = new TCPAcceptorThread();
		thread.start();
	}

	public void deactivate(){
		thread.interrupt();
		
		synchronized(channels){
			for(NetworkChannel channel : channels.values()){
				channel.close();
			}
			channels.clear();
		}
	}
	
	@Override
	public List<NetworkChannel> getChannels(){
		List<NetworkChannel> c;
		synchronized(channels){
			c = new ArrayList<NetworkChannel>(channels.values());
		}
		return c;
	}
	
	@Override
	public NetworkChannel getChannel(URI uri) throws Exception {
		synchronized(channels){
			NetworkChannel channel = channels.get(uri.getAddress());
			if(channel == null) {
				try {
					channel = new TCPChannel(uri.getIP(), uri.getPort(), receiver);
					channels.put(channel.getRemoteAddress(), channel);
				} catch(IOException ioe){
					throw new Exception("Error creating TCP channel to "+uri, ioe);
				}
			}
			return channel;
		}
	}

	@Override
	public void deleteChannel(NetworkChannel channel){
		synchronized(channels){
			channels.remove(channel.getRemoteAddress());
			channel.close();
		}
	}
	
	
	// handles incoming tcp messages.
	protected final class TCPAcceptorThread extends Thread {

		private ServerSocket socket;

		TCPAcceptorThread() throws IOException {
			setDaemon(true);

			int e = 0;
			while (true) {
				try {
					listeningPort += e;
					socket = new ServerSocket(listeningPort);
					return;
				} catch (final BindException b) {
					e++;
				}
			}
		}


		public void run() {
			while (!isInterrupted()) {
				try {
					// accept incoming connections
					TCPChannel channel = new TCPChannel(socket.accept(), receiver);
					synchronized(channels){
						channels.put(channel.getRemoteAddress(), channel);
					}
				} catch (IOException ioe) {
					Activator.logger.log(LogService.LOG_ERROR, "Error creating new channel: "+ioe.getMessage(), ioe);
				}
			}
		}
		
		// method to try to get a currently valid ip of the host
		public String getListeningAddress(){
			// method one : already set (e.g. using property rsa.ip)
			if(hostAddress==null){
				// if not set , try to get it from a (hopefully the preferred) network interface
				try {
					Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
					for (NetworkInterface netint : Collections.list(nets)) {
						Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
						for (InetAddress inetAddress : Collections.list(inetAddresses)) {
							if (hostAddress != null
									&& (inetAddress.isLoopbackAddress() 
										|| inetAddress.isAnyLocalAddress())) {
								break; // only set loopbackadres if no other possible
							} else if (!ipv6 && inetAddress instanceof Inet4Address) {
								hostAddress = inetAddress.getHostAddress();
								break;
							} else if (ipv6 && inetAddress instanceof Inet6Address) {
								if (!(inetAddress.isLinkLocalAddress() 
										|| inetAddress.isSiteLocalAddress())) { // restrict to global addresses?
									String address = inetAddress.getHostAddress();
									// remove scope from hostAddress
									int e = address.indexOf('%');
									if (e == -1) {
										e = address.length();
									}
									hostAddress = "[" + address.substring(0, e)+ "]";
									break;
								}
							}
						}
						if (netint.getName().equals(networkInterface)
								&& hostAddress != null) { // prefer configured networkInterface
							break;
						}
					}
				} catch (Exception e) {
				}
			}

			// if still not set just get the default one...
			if(hostAddress==null){
				hostAddress = socket.getInetAddress().getHostAddress();
				if(hostAddress.contains(":")){
					hostAddress = "["+hostAddress+"]";
				}
			}
			
			return hostAddress+":"+socket.getLocalPort();
		}
	}

	@Override
	public String getAddress() {
		if(thread!=null)
			return thread.getListeningAddress();
		return null;
	}
}
