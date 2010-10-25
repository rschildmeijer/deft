package org.deftserver.example.kv;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Map;

import org.deftserver.util.Closeables;
import org.deftserver.web.AsyncResult;
import org.deftserver.web.IOHandler;
import org.deftserver.web.IOLoop;
import org.deftserver.web.buffer.DynamicByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;

public class KeyValueStoreIOHandler implements IOHandler {
	
	private final Logger logger = LoggerFactory.getLogger(KeyValueStoreIOHandler.class);
	
	// Read callbacks (callbacks that are supposed to be invoked upon a successful read)
	private final Map<SelectionKey, AsyncResult<String>> rcbs = Maps.newHashMap();

	@Override
	public void handleAccept(SelectionKey key) throws IOException {
//		logger.debug("[KeyValueStoreHandler] handle accept...");
//		SocketChannel clientChannel = ((ServerSocketChannel) key.channel()).accept();
//		clientChannel.configureBlocking(false);
//		IOLoop.INSTANCE.addHandler(clientChannel, this, SelectionKey.OP_READ, ByteBuffer.allocate(1024));
	}

	@Override
	public void handleRead(SelectionKey key) throws IOException {
		logger.debug("[KeyValueStoreHandler] handle read...");
		SocketChannel channel = (SocketChannel) key.channel();
		ByteBuffer buffer = (ByteBuffer) key.attachment();
		try {
			long bytesRead = channel.read(buffer);
			logger.debug("[KeyValueStoreHandler] read data: {} bytes", bytesRead);
		} catch (IOException e) {
			logger.debug("[KeyValueStoreHandler] could not read data: ", e.getMessage());
			Closeables.closeQuietly(channel);
		}
		int length = buffer.position();
		buffer.flip();
		String data = new String(buffer.array(), 0, length, Charsets.UTF_8);
		logger.debug("[KeyValueStoreHandler] KeyValueStore server sent: {}", data);
		Closeables.closeQuietly(channel);
		logger.debug("[KeyValueStoreHandler] closed connection to KeyValueStore");
		if (rcbs.containsKey(key)) {
			rcbs.get(key).onSuccess(data);
		}
	}

	@Override
	public void handleWrite(SelectionKey key) {
		logger.debug("[KeyValueStoreHandler] handle write...");
		DynamicByteBuffer dbb = (DynamicByteBuffer) key.attachment();
		ByteBuffer toSend = dbb.getByteBuffer();
		SocketChannel channel = (SocketChannel)key.channel();
		logger.debug("[KeyValueStoreHandler] about to send: {} bytes", dbb.position());
		try {
			toSend.flip();	// prepare for write
			long bytesWritten = channel.write(toSend);
			logger.debug("[KeyValueStoreHandler] sent: {} bytes", bytesWritten);
			if (!toSend.hasRemaining()) {
			} else {
				toSend.compact();	// make room for more data be "read" in
			}
		} catch (IOException e) {
			logger.error("[KeyValueStoreHandler] Failed to send data to client: {}", e.getMessage());
		}
		IOLoop.INSTANCE.addHandler(channel, this, SelectionKey.OP_READ, ByteBuffer.allocate(1024));
	}
	
	void addReadCallback(SelectionKey key, AsyncResult<String> cb) {
		rcbs.put(key, cb);
	}

}
