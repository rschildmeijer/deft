package org.deftserver.web.protocol;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.deftserver.web.Application;
import org.deftserver.web.handler.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.io.Closeables;

public class HttpProtocol implements Protocol, HttpProtocolMXBean {
	
	private final static Logger logger = LoggerFactory.getLogger(HttpProtocol.class);

	/** The number of seconds Deft will wait for a subsequent request before closing the connection */
	private final static long KEEP_ALIVE_TIMEOUT = 30 * 1000;	// 30s 
	
	/** All {@link SocketChannel} connections where request header "Connection: Close" is missing. 
	 * ("In HTTP 1.1 all connections are considered persistent, unless declared otherwise")
	 * The value associated with each {@link SocketChannel} is the connection expiration time in ms.
	 */
	private final Map<SocketChannel, Long> persistentConnections = new HashMap<SocketChannel, Long>();
	
	private final Map<SelectionKey, List<ByteBuffer>> stagedData = Maps.newHashMap();
	private final Map<SelectionKey, File> stagedFiles = Maps.newHashMap();
	private final int readBufferSize;

	private final Application application;
	
	public HttpProtocol(Application app) {
		application = app;
		readBufferSize = app.getReadBufferSize();
		registerMXBean();
	}
	

	private void registerMXBean() {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        String mbeanName = "org.deftserver.web.protocol:type=HttpProtocol";
        try {
            mbs.registerMBean(this, new ObjectName(mbeanName));
        }
        catch (Exception e) {
            logger.error("Unable to register {} MXBean", this.getClass().getCanonicalName());
        }
	}
	
	@Override
	public void handleAccept(SelectionKey key) throws IOException {
		logger.debug("handle accept...");
		SocketChannel clientChannel = ((ServerSocketChannel) key.channel()).accept();
		clientChannel.configureBlocking(false);
		clientChannel.register(key.selector(), SelectionKey.OP_READ, ByteBuffer.allocate(readBufferSize));
	}

	@Override
	public void handleRead(SelectionKey key) throws IOException {
		logger.debug("handle read...");
		SocketChannel clientChannel = (SocketChannel) key.channel();
		HttpRequest request = getHttpRequest(key, clientChannel);
		
		if (request.isKeepAlive()) {
			persistentConnections.put(clientChannel, System.currentTimeMillis() + KEEP_ALIVE_TIMEOUT);
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
		List<ByteBuffer> pending = stagedData.get(key);
		logger.debug("pending data about to be written");
		ByteBuffer toSend = pending.get(0);
		if (pending != null && !pending.isEmpty()) {
			try {
				long bytesWritten = ((SocketChannel)key.channel()).write(toSend);
				logger.debug("sent {} bytes to wire", bytesWritten);
				if (!toSend.hasRemaining()) {
					logger.debug("sent all data in toSend buffer");
					pending.remove(0);
					if (pending.isEmpty()) {
						// last 'chunk' sent
						closeOrRegisterForRead(key);
					}
				}
			} catch (IOException e) {
				logger.error("Failed to send data to client: {}", e.getMessage());
				Closeables.closeQuietly(key.channel());
			}
		}
		if (stagedFiles.containsKey(key)) {
			File file = stagedFiles.get(key);
			try {
				SocketChannel clientChannel = (SocketChannel) key.channel();
				new RandomAccessFile(file, "r").getChannel().transferTo(0, file.length(), clientChannel);
			} catch (IOException e) {
				logger.error("Error writing (static file) response: {}", e.getMessage());
			}
			closeOrRegisterForRead(key);
		} 
	}
	
	private void closeOrRegisterForRead(SelectionKey key) {
		if (persistentConnections.containsKey(key.channel())) {
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

	/**
	 * Staging data to be written to wire.
	 * 
	 * @param key
	 * @param responseData
	 */
	public void stage(SelectionKey key, ByteBuffer responseData) {
		List<ByteBuffer> pending = stagedData.get(key);
		if (pending == null) {
			pending = new LinkedList<ByteBuffer>();
			stagedData.put(key, pending);
		}
		pending.add(responseData);
	}


	public void stage(SelectionKey key, File file) {
		stagedFiles.put(key, file);
	}

}
