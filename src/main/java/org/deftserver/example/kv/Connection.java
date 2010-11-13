package org.deftserver.example.kv;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.deftserver.io.IOLoop;
import org.deftserver.io.buffer.DynamicByteBuffer;
import org.deftserver.io.timeout.Timeout;
import org.deftserver.web.AsyncResult;

import com.google.common.base.Charsets;

public class Connection {
	
	//private IOStream iostream;
	private SocketChannel channel;
	private final KeyValueStoreIOHandler handler = new KeyValueStoreIOHandler();
	private final String host;
	private final int port;
	private final Timeout timeout;

	public Connection(String host, int port, Timeout timeout) {
		this.host = host;
		this.port = port;
		this.timeout = timeout;
	}

	public void connect() {
		try {
			channel = SocketChannel.open(new InetSocketAddress(host, port));
			channel.configureBlocking(false);
		} catch (IOException e) { e.printStackTrace(); }
		//IOLoop.INSTANCE.addHandler(channel, handler, SelectionKey.OP_READ, ByteBuffer.allocate(1024));
		//iostream = new IOStream(channel, ioLoop);
	}

//	public void disconnect() {
//		iostream.close();
//	}
//
//	
//	public void readLine(int length, AsyncResult<String> cb) {
//		this.iostream.read_until("\r\n", cb);
//	}
//
//	public void read(int length, AsyncResult<String> cb) {
//		this.iostream.read_byte(length, cb);
//	}

	public void write(String data, AsyncResult<String> cb) {
		DynamicByteBuffer dbb = DynamicByteBuffer.allocate(1024);
		dbb.put(data.getBytes(Charsets.UTF_8));
		SelectionKey key = IOLoop.INSTANCE.addHandler(channel, handler, SelectionKey.OP_WRITE, dbb);
		handler.addReadCallback(key, cb);
		//this.iostream.write(data);		
	}

}
