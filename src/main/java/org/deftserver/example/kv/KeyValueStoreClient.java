package org.deftserver.example.kv;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.deftserver.io.AsynchronousSocket;
import org.deftserver.io.IOLoop;
import org.deftserver.web.AsyncCallback;
import org.deftserver.web.AsyncResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyValueStoreClient {
	
	private final static Logger logger = LoggerFactory.getLogger(KeyValueStoreClient.class);
	
	private AsynchronousSocket socket;
	private SocketChannel channel;
	private final String host;
	private final int port;
	
	public KeyValueStoreClient(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public void connect() {
		try {
			channel = SocketChannel.open(new InetSocketAddress(host, port));
			channel.configureBlocking(false);
		} catch (IOException e) { e.printStackTrace(); }
		socket = new AsynchronousSocket(channel);
		IOLoop.INSTANCE.addHandler(channel, socket, SelectionKey.OP_READ, ByteBuffer.allocate(1024));
	}
	
	public void get(String value, AsyncResult<String> cb) {
		socket.write("GET deft\r\n", new WriteCallback(cb));
	}
	
	private class WriteCallback implements AsyncCallback {

		private final AsyncResult<String> cb;
		
		public WriteCallback(AsyncResult<String> cb) { 
			this.cb = cb; 
		}
		
		@Override
		public void onCallback() {
			// write is finished. read response from server
			logger.debug("readUntil: \r\n");
			socket.readUntil("\r\n", cb);
		}
		
	}
	
}
