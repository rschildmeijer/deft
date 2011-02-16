package org.deftserver.web.http;

import static org.deftserver.web.http.HttpServerDescriptor.KEEP_ALIVE_TIMEOUT;
import static org.deftserver.web.http.HttpServerDescriptor.READ_BUFFER_SIZE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Map;

import org.deftserver.io.IOHandler;
import org.deftserver.io.IOLoop;
import org.deftserver.io.IOLoopFactory;
import org.deftserver.io.buffer.DynamicByteBuffer;
import org.deftserver.util.Closeables;
import org.deftserver.util.TimeoutFactory;
import org.deftserver.web.Application;
import org.deftserver.web.handler.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

public class HttpProtocol implements IOHandler {
	
	private final static Logger logger = LoggerFactory.getLogger(HttpProtocol.class);

	private final Application application;

	// a queue of half-baked (pending/unfinished) HTTP post request
	//private final Map<SelectableChannel, PartialHttpRequest> partials = Maps.newHashMap();
 	
	public HttpProtocol(Application app) {
		application = app;
	}
	
	@Override
	public void handleAccept(SocketChannel clientChannel) throws IOException {
		logger.debug("handle accept...");
//		SocketChannel clientChannel = ((ServerSocketChannel) clientChannel.channel()).accept();
//		clientChannel.configureBlocking(false);
		IOLoopFactory.getLoopController().addHandler(clientChannel, this, SelectionKey.OP_READ, new HttpChannelContext(ByteBuffer.allocate(READ_BUFFER_SIZE)));
	}

	@Override
	public void handleRead(SelectionKey key) throws IOException {
		logger.debug("handle read...");
		SocketChannel clientChannel = (SocketChannel) key.channel();
		HttpRequest request = getHttpRequest(key, clientChannel);
		

		if (!(request instanceof PartialHttpRequest)) {
			HttpResponse response = new HttpResponse(this, key, request.isKeepAlive());
			RequestHandler rh = application.getHandler(request);
			HttpRequestDispatcher.dispatch(rh, request, response);
			
			//Only close if not async. In that case its up to RH to close it (+ don't close if it's a partial request).
			if (!rh.isMethodAsynchronous(request.getMethod())) {
				response.finish();

			}else {
				if (request.isKeepAlive()) {
					setKeepAlive(key, true);
//					IOLoopFactory.getLoopController().addKeepAliveTimeout(
//							clientChannel, 
//							TimeoutFactory.keepAliveTimeout(clientChannel, KEEP_ALIVE_TIMEOUT)
//					);
				}else {
					setKeepAlive(key, false);
				}
			}

		}
	}
	
	@Override
	public void handleWrite(SelectionKey key) {
		logger.debug("handle write...");
		HttpChannelContext ctx = (HttpChannelContext) key.attachment();
		logger.debug("pending data about to be written");
		ByteBuffer toSend = ctx.getBufferOut();
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
		if (key.isValid() && isKeepAlive(key)) {
			try {
				key.channel().register(key.selector(), SelectionKey.OP_READ, reuseAttachment(key));
				//key.channel().register(key.selector(), SelectionKey.OP_READ, ByteBuffer.allocate(READ_BUFFER_SIZE));
				logger.debug("keep-alive connection. registrating for read.");
			} catch (ClosedChannelException e) {
				logger.debug("ClosedChannelException while registrating key for read: {}", e.getMessage());
				Closeables.closeQuietly(key.channel());
			}		
		} else {
			// http request should be finished and no 'keep-alive' => close connection
			logger.debug("Closing finished (non keep-alive) http connection"); 
			Closeables.closeQuietly(key.channel());
			
		}
	}
	/**
	 * Clears the buffer (prepares for reuse) attached to the given SelectionKey.
	 * @return A cleared (position=0, limit=capacity) ByteBuffer which is ready for new reads
	 */
	private HttpChannelContext reuseAttachment(SelectionKey key) {
		HttpChannelContext ctx = (HttpChannelContext) key.attachment();
		ctx.clear();	// prepare for reuse
		return ctx;
	}
	
	private boolean isKeepAlive(SelectionKey key){
		return ((HttpChannelContext) key.attachment()).isKeepAlive();
	}


	private void setKeepAlive(SelectionKey key, boolean keep){
		((HttpChannelContext) key.attachment()).setKeepAlive(keep);
	}
	
	private HttpRequest getHttpRequest(SelectionKey key, SocketChannel clientChannel) {
		HttpChannelContext ctx = (HttpChannelContext) key.attachment();
		try {
			clientChannel.read(ctx.getBufferIn());
		} catch (IOException e) {
			logger.warn("Could not read buffer: {}", e.getMessage());
			Closeables.closeQuietly(clientChannel);
		}
		ctx.getBufferIn().flip();
		
		return doGetHttpRequest(key, clientChannel, ctx);
	}
	
	private HttpRequest doGetHttpRequest(SelectionKey key, SocketChannel clientChannel, HttpChannelContext ctx) {
		//do we have any unfinished http post requests for this channel?
		HttpRequest request = null;
		if (ctx.getContext() != null) {
			request = HttpRequest.continueParsing(ctx.getBufferIn(), ctx.getContext());
//			if (! (request instanceof PartialHttpRequest)) {	// received the entire payload/body
//				partials.remove(clientChannel);
//			}
		} else {
			request = HttpRequest.of(ctx.getBufferIn());
			if (request instanceof PartialHttpRequest) {
				ctx.setContext((PartialHttpRequest) request);
			}
			
		}
		return request;
	}
	
	@Override
	public String toString() { return "HttpProtocol"; }
	
}
