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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.Socket;

import org.osgi.service.log.LogService;

import be.iminds.aiolos.rsa.Activator;
import be.iminds.aiolos.rsa.Config;
import be.iminds.aiolos.rsa.Config.SerializationStrategy;
import be.iminds.aiolos.rsa.network.api.MessageReceiver;
import be.iminds.aiolos.rsa.network.api.NetworkChannel;
import be.iminds.aiolos.rsa.network.message.ROSGiMessage;
import be.iminds.aiolos.rsa.serialization.api.Deserializer;
import be.iminds.aiolos.rsa.serialization.api.SerializationException;
import be.iminds.aiolos.rsa.serialization.api.Serializer;
import be.iminds.aiolos.rsa.serialization.java.JavaDeserializer;
import be.iminds.aiolos.rsa.serialization.java.JavaSerializer;
import be.iminds.aiolos.rsa.serialization.kryo.KryoDeserializer;
import be.iminds.aiolos.rsa.serialization.kryo.KryoSerializer;

/**
 * TCP implementation of the protocol, sends and recieves ROSGiMessages
 */
public class TCPChannel implements NetworkChannel {

	private Socket socket;
	private Deserializer input;
	private Serializer output;

	private MessageReceiver receiver;
	private Thread receiverThread = null;
	
	private volatile boolean connected = true;

	
	public TCPChannel(final Socket socket, MessageReceiver receiver) throws IOException {
		this.receiver = receiver;
		open(socket);
		receiverThread = new ReceiverThread();
		receiverThread.start();
	}
	
	TCPChannel(String ip, int port, MessageReceiver receiver) throws IOException {
		this(new Socket(ip, port), receiver);
	}

	private void open(final Socket s) throws IOException {
		socket = s;
		try {
			socket.setKeepAlive(true);
		} catch (final Throwable t) {
			// for 1.2 VMs that do not support the setKeepAlive
		}
		socket.setTcpNoDelay(true);
		// Use ObjectOutputstream for object serialization
		// Maybe change to a more efficient serialization algorithm?
		if(Config.SERIALIZATION==SerializationStrategy.KRYO){ 
			try {
				output = new KryoSerializer(new BufferedOutputStream(
						socket.getOutputStream()));
				output.flush();
				input = new KryoDeserializer(new BufferedInputStream(socket
						.getInputStream()));
			}catch(NoClassDefFoundError e){
				Activator.logger.log(LogService.LOG_WARNING, "Kryo not available, falling back to Java Serialization", e);
				// fall back to Java serialization
				Config.SERIALIZATION = SerializationStrategy.JAVA;
			}
		} 
		if(Config.SERIALIZATION==SerializationStrategy.JAVA){
			output = new JavaSerializer(new BufferedOutputStream(
					socket.getOutputStream()));
			output.flush();
			input = new JavaDeserializer(new BufferedInputStream(socket
					.getInputStream()));
		} 
	}

	public void close(){
		try {
			socket.close();
		} catch(IOException ioe){

		}
		connected = false;
		receiverThread.interrupt();
	}

	public void sendMessage(final ROSGiMessage message)
			throws SerializationException, IOException {
		message.send(output);
	}

	class ReceiverThread extends Thread {
		ReceiverThread() {
			setDaemon(true);
		}

		public void run() {
			while (connected) {
				try {
					final ROSGiMessage msg = ROSGiMessage.parse(input);
					receiver.receivedMessage(msg, TCPChannel.this);
				} catch (SerializationException e) {
					Activator.logger.log(LogService.LOG_WARNING, "Exception deserializing message : "+e.getMessage(), e);
				} catch(Exception e){
					Activator.logger.log(LogService.LOG_WARNING, "Exception receiving message, closing network channel to "+getRemoteAddress()+" : "+e.getMessage(), e);
					// e.printStackTrace();
					// Handle socket error
					connected = false;
					try {
						socket.close();
					} catch (final IOException e1) {
					}
					receiver.receivedMessage(null, TCPChannel.this);
					return;
				} 
			}
		}
	}

	@Override
	public String getRemoteAddress() {
		InetAddress address = socket.getInetAddress();
		if(address instanceof Inet6Address){
			return "["+address.getHostAddress()+"]:"+socket.getPort();
		} else {
			return address.getHostAddress()+":"+socket.getPort();
		}
	}

	@Override
	public String getLocalAddress(){
		InetAddress address = socket.getLocalAddress();
		if(address instanceof Inet6Address){
			return "["+address.getHostAddress()+"]:"+socket.getLocalPort();
		} else {
			return address.getHostAddress()+":"+socket.getLocalPort();
		}
	}
}
