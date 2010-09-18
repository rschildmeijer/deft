package org.ulme.web;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;

public class IOLoop {

	private static final boolean BLOCK = false;
	
	private ServerSocketChannel channel;
	
	protected IOLoop() {
		try {
			channel = ServerSocketChannel.open();
			channel.configureBlocking(BLOCK);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void start() {
		//Thread.currentThread().setName("IO-LOOP-THREAD");
		//Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
	}

	protected void listen(int port) {
		InetSocketAddress endpoint = new InetSocketAddress(port);	// use "any" address
		try {
			channel.socket().bind(endpoint);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
