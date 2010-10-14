package org.deftserver.web.protocol;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.deftserver.web.Application;
import org.deftserver.web.handler.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Closeables;

public class HttpProtocol implements Protocol, HttpProtocolImplMXBean {
	
	private final static Logger logger = LoggerFactory.getLogger(HttpProtocol.class);

	/** The number of seconds Deft will wait for a subsequent request before closing the connection */
	private final static long KEEP_ALIVE_TIMEOUT = 30 * 1000;	// 30s 
	
	/** All {@link SocketChannel} connections where request header "Connection: Close" is missing. 
	 * ("In HTTP 1.1 all connections are considered persistent, unless declared otherwise")
	 * The value associated with each {@link SocketChannel} is the connection expiration time in ms.
	 */
	private final Map<SocketChannel, Long> persistentConnections = new HashMap<SocketChannel, Long>();
	
	private final int readBufferSize;

	private final Application application;
	
	public HttpProtocol(Application app) {
		application = app;
		readBufferSize = app.getReadBufferSize();
		registerMXBean();
	}
	

	private void registerMXBean() {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        String mbeanName = "org.deftserver.web.protocol:type=HttpProtocolImpl";
        try {
            mbs.registerMBean(this, new ObjectName(mbeanName));
        }
        catch (Exception e) {
            logger.error("Unable to register {} MXBean", this.getClass().getCanonicalName());
        }
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
		
		if (request.isKeepAlive()) {
			persistentConnections.put(clientChannel, System.currentTimeMillis() + KEEP_ALIVE_TIMEOUT);
		}
		
		HttpResponse response = new HttpResponse(clientChannel, request.isKeepAlive());
		RequestHandler rh = application.getHandler(request);
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
			logger.warn("Could not read buffer: {}", e.getMessage());
			Closeables.closeQuietly(clientChannel);
		}
		buffer.clear();	// reuse the read buffer, (hint: "Connection: Keep-Alive" header)
		return HttpRequest.of(buffer);
	}
	
	public void handleCallback() {
		long now = System.currentTimeMillis();
		Iterator<Entry<SocketChannel, Long>> iter = persistentConnections.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<SocketChannel, Long> candidate = iter.next();
			long expires = candidate.getValue();
			if (now >= expires) {
				logger.debug("Closing expired keep-alive connection");
				Closeables.closeQuietly(candidate.getKey());
				iter.remove();
			}
		}
	}

	@Override
	public int getPersistentConnections() {
		return persistentConnections.size();
	}

}
