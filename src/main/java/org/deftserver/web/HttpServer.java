package org.deftserver.web;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.util.List;

import org.deftserver.io.IOLoop;
import org.deftserver.util.Closeables;
import org.deftserver.web.http.HttpProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class HttpServer {
	
	private final Logger logger = LoggerFactory.getLogger(HttpServer.class);
	
	private static final int MIN_PORT_NUMBER = 1;
	private static final int MAX_PORT_NUMBER = 65535;
	
	private ServerSocketChannel serverChannel;
	private final List<IOLoop> ioLoops = Lists.newLinkedList();
	
	private final Application application;
	
	public HttpServer(Application application) {
		this.application = application;
	}

	/**
	 * If you want to run Deft on multiple threads first invoke {@link #bind(int)} then {@link #start(int)} 
	 * instead of {@link #listen(int)} (listen starts Deft http server on a single thread with the default IOLoop 
	 * instance: {@code IOLoop.INSTANCE}).
	 * 
	 * @return this for chaining purposes
	 */
	public void listen(int port) {
		bind(port);
		ioLoops.add(IOLoop.INSTANCE);
		registerHandler(IOLoop.INSTANCE, new HttpProtocol(application));
	}
	
	public void bind(int port) {
		if (port <= MIN_PORT_NUMBER || port > MAX_PORT_NUMBER) {
			throw new IllegalArgumentException("Invalid port number. Valid range: [" + 
					MIN_PORT_NUMBER + ", " + MAX_PORT_NUMBER + ")");
		}
		
		try {
			serverChannel = ServerSocketChannel.open();
			serverChannel.configureBlocking(false);
		} catch (IOException e) {
			logger.error("Error creating ServerSocketChannel: {}", e);
		}
		
		InetSocketAddress endpoint = new InetSocketAddress(port);	// use "any" address
		try {
			serverChannel.socket().bind(endpoint);
		} catch (IOException e) {
			logger.error("Could not bind socket: {}", e);
		}	
	}
	
	public void start(int numThreads) {
		for (int i = 0; i < numThreads; i++) {
			final IOLoop ioLoop = new IOLoop();
			ioLoops.add(ioLoop);
			final HttpProtocol protocol = new HttpProtocol(ioLoop, application);
			new Thread(new Runnable() {
				
				@Override public void run() {
					registerHandler(ioLoop, protocol);
					ioLoop.start();
				}
			}).start();
		}
	}
	
	/**
	 * Unbinds the port and shutdown the HTTP server
	 */
	public void stop() {
		logger.debug("Stopping HTTP server");
		for (IOLoop ioLoop : ioLoops) {
			// TODO RS 110527 Should probably do this in each IOLoop through an AsyncCallback 
			// (hint: ioloop.addCallback(..))
			Closeables.closeQuietly(ioLoop, serverChannel);
		}
	}
	
	private void registerHandler(IOLoop ioLoop, HttpProtocol protocol) {
		ioLoop.addHandler(
				serverChannel,
				protocol, 
				SelectionKey.OP_ACCEPT,
				null /*attachment*/
		);
	}

}
