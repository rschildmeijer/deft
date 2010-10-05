package org.deftserver.web.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.deftserver.web.Application;
import org.deftserver.web.handler.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpProtocolImpl implements HttpProtocol {
	
	private final static Logger logger = LoggerFactory.getLogger(HttpProtocolImpl.class);
	
	private final int readBufferSize;

	private final Application application;
	
	public HttpProtocolImpl(Application app) {
		application = app;
		readBufferSize = app.getReadBufferSize();
	}
	
	@Override
	public void handleAccept(SelectionKey key) throws IOException {
		SocketChannel clientChannel = ((ServerSocketChannel) key.channel()).accept();
		clientChannel.configureBlocking(false);
		clientChannel.register(key.selector(), SelectionKey.OP_READ, ByteBuffer.allocate(readBufferSize));
	}

	@Override
	public void handleRead(SelectionKey key) throws IOException {
		SocketChannel clientChannel = (SocketChannel) key.channel();
		HttpRequest request = getHttpRequest(key, clientChannel);
		HttpResponse response = new HttpResponse(clientChannel);
		
		RequestHandler rh = application.getHandler(request.getRequestedPath());
		HttpRequestDispatcher.dispatch(rh, request, response);
		
		//Only close if not async. In that case its up to RH to close it
		if (!rh.isMethodAsynchronous(request.getMethod())) {
			response.finish();
		}
	}
	
	private HttpRequest getHttpRequest(SelectionKey key, SocketChannel clientChannel) {
		ByteBuffer buffer = (ByteBuffer) key.attachment();
		try {
			clientChannel.read(buffer);
		} catch (IOException e) {
			logger.error("Could not read buffer: {}", e);
		}
		return HttpRequest.of(buffer);
	}
	


}
