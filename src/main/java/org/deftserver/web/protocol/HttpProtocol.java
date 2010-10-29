package org.deftserver.web.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.deftserver.util.Closeables;
import org.deftserver.util.TimeoutFactory;
import org.deftserver.web.Application;
import org.deftserver.web.IOHandler;
import org.deftserver.web.IOLoop;
import org.deftserver.web.buffer.DynamicByteBuffer;
import org.deftserver.web.handler.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpProtocol implements IOHandler {
	
	private final static Logger logger = LoggerFactory.getLogger(HttpProtocol.class);

	/** The number of seconds Deft will wait for a subsequent request before closing the connection */
	private final static long KEEP_ALIVE_TIMEOUT = 30 * 1000;	// 30s 
	
	private final int readBufferSize;

	private final Application application;
	
	public HttpProtocol(Application app) {
		application = app;
		readBufferSize = app.getReadBufferSize();
	}
	
	@Override
	public void handleAccept(SelectionKey key) throws IOException {
		logger.debug("handle accept...");
		SocketChannel clientChannel = ((ServerSocketChannel) key.channel()).accept();
		clientChannel.configureBlocking(false);
		IOLoop.INSTANCE.addHandler(clientChannel, this, SelectionKey.OP_READ, ByteBuffer.allocate(readBufferSize));
	}

	@Override
	public void handleRead(SelectionKey key) throws IOException {
		logger.debug("handle read...");
		SocketChannel clientChannel = (SocketChannel) key.channel();
		HttpRequest request = getHttpRequest(key, clientChannel);
		
		if (request.isKeepAlive()) {
			IOLoop.INSTANCE.addKeepAliveTimeout(
					clientChannel, 
					TimeoutFactory.keepAliveTimeout(clientChannel, KEEP_ALIVE_TIMEOUT)
			);
		}
		HttpResponse response = new HttpResponse(this, key, request.isKeepAlive());
		RequestHandler rh = application.getHandler(request);
		HttpRequestDispatcher.dispatch(rh, request, response);
		
		//Only close if not async. In that case its up to RH to close it
		if (!rh.isMethodAsynchronous(request.getMethod())) {
			response.finish();
		}
	}
	
	@Override
	public void handleWrite(SelectionKey key) {
		logger.debug("handle write...");
		DynamicByteBuffer dbb = (DynamicByteBuffer) key.attachment();
		logger.debug("pending data about to be written");
		ByteBuffer toSend = dbb.getByteBuffer();
		try {
			toSend.flip();	// prepare for write
			long bytesWritten = ((SocketChannel)key.channel()).write(toSend);
			logger.debug("sent {} bytes to wire", bytesWritten);
			if (!toSend.hasRemaining()) {
				logger.debug("sent all data in toSend buffer");
				closeOrRegisterForRead(key);	// should probably only be done if the HttpResponse is finished
			} else {
				toSend.compact();	// make room for more data be "read" in
			}
		} catch (IOException e) {
			logger.error("Failed to send data to client: {}", e.getMessage());
			Closeables.closeQuietly(key.channel());
		}
	}
	
	public void closeOrRegisterForRead(SelectionKey key) {
		if (IOLoop.INSTANCE.hasKeepAliveTimeout(key.channel())) {
			try {
				key.channel().register(key.selector(), SelectionKey.OP_READ, ByteBuffer.allocate(readBufferSize));
				logger.debug("keep-alive connection. registrating for read.");
			} catch (ClosedChannelException e) {
				logger.debug("ClosedChannelException while registrating key for read");
				Closeables.closeQuietly(key.channel());
			}		
		} else {
			// http request should be finished and no 'keep-alive' => close connection
			logger.debug("Closing finished (non keep-alive) http connection"); 
			Closeables.closeQuietly(key.channel());
		}
	}


	private HttpRequest getHttpRequest(SelectionKey key, SocketChannel clientChannel) {
		ByteBuffer buffer = (ByteBuffer) key.attachment();
		try {
			clientChannel.read(buffer);
		} catch (IOException e) {
			logger.warn("Could not read buffer: {}", e.getMessage());
			Closeables.closeQuietly(clientChannel);
		}
		buffer.flip();
		return HttpRequest.of(buffer);
	}
	
}
