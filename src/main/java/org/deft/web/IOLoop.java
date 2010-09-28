package org.deft.web;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;

import org.deft.web.protocol.HttpProtocol;
import org.deft.web.protocol.HttpProtocolImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IOLoop {
	
	private final static Logger logger = LoggerFactory.getLogger(IOLoop.class);

	private static final long TIMEOUT = 3000;	//in ms

	private final Application application;
	private ServerSocketChannel channel;
	private Selector selector;

	protected IOLoop(Application application) {
		this.application = application;
		try {
			channel = ServerSocketChannel.open();
			channel.configureBlocking(false);
			selector = Selector.open();
		} catch (IOException e) {
			logger.error("Error creating ServerSocketChannel: {}", e);
		}
	}

	public void start() {
		Thread.currentThread().setName("I/O-LOOP");
		//Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

		registerSelector();
		HttpProtocol protocol = new HttpProtocolImpl(application);
		while (true) {
			try {
				if (selector.select(TIMEOUT) == 0) {
					continue;
				}

				Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
				while (keys.hasNext()) {
					SelectionKey key = keys.next();
					if (key.isAcceptable()) {
						protocol.handleAccept(key);
					}
					if (key.isReadable()) {
						protocol.handleRead(key);
					}
					keys.remove();
				}

			} catch (IOException e) {
				logger.error("Exception received in IOLoop: {}", e);			}
		}
	}

	protected void listen(int port) {
		InetSocketAddress endpoint = new InetSocketAddress(port);	// use "any" address
		try {
			channel.socket().bind(endpoint);
		} catch (IOException e) {
			logger.error("Could not bind socket: {}", e);
		}
	}

	private void registerSelector() {
		try {
			channel.register(selector, SelectionKey.OP_ACCEPT);
		} catch (ClosedChannelException e) {
			logger.error("Could not register selector: {}", e);		
		}		
	}

}
