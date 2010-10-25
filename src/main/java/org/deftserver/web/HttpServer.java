package org.deftserver.web;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;

import org.deftserver.web.protocol.HttpProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServer {
	
	private final Logger logger = LoggerFactory.getLogger(HttpServer.class);
	
	private static final int MIN_PORT_NUMBER = 1;
	private static final int MAX_PORT_NUMBER = 65535;
	
	private ServerSocketChannel serverChannel;
	
	private final IOHandler protocol;

	public HttpServer(Application application) {
		protocol = new HttpProtocol(application);
		try {
			serverChannel = ServerSocketChannel.open();
			serverChannel.configureBlocking(false);
		} catch (IOException e) {
			logger.error("Error creating ServerSocketChannel: {}", e);
		}
	}

	/**
	 * @return this for chaining purposes
	 */
	public void listen(int port) {
		if (port <= MIN_PORT_NUMBER || port > MAX_PORT_NUMBER) {
			throw new IllegalArgumentException("Invalid port number. Valid range: [" + 
					MIN_PORT_NUMBER + ", " + MAX_PORT_NUMBER + ")");
		}
		InetSocketAddress endpoint = new InetSocketAddress(port);	// use "any" address
		try {
			serverChannel.socket().bind(endpoint);
		} catch (IOException e) {
			logger.error("Could not bind socket: {}", e);
		}
		registerHandler();
	}
	
	private void registerHandler() {
		IOLoop.INSTANCE.addHandler(
				serverChannel,
				protocol, 
				SelectionKey.OP_ACCEPT,
				null /*attachment*/
		);
	}

}
