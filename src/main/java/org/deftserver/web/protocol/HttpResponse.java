package org.deftserver.web.protocol;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

import org.deftserver.util.DateUtil;
import org.deftserver.util.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.Closeables;

public class HttpResponse {
	
	private final static Logger logger = LoggerFactory.getLogger(HttpProtocol.class);
	
	private final static int WRITE_BUFFER_SIZE = 1500;	// in bytes

	private final HttpProtocol protocol;
	private final SelectionKey key;
	
	private int statusCode = 200;	// default response status code
	
	private final Map<String, String> headers = new HashMap<String, String>();
	private boolean headersCreated = false;
	private ByteBuffer responseData = ByteBuffer.allocate(WRITE_BUFFER_SIZE);
//	private final boolean keepAlive;
	
	public HttpResponse(HttpProtocol protocol, SelectionKey key, boolean keepAlive) {
		this.protocol = protocol;
		this.key = key;
		headers.put("Server", "DeftServer/0.2.0-SNAPSHOT");
		headers.put("Date", DateUtil.getCurrentAsString());

		if (keepAlive) {
//			this.keepAlive = true;
			headers.put("Connection", "Keep-Alive");
		} else {
//			this.keepAlive = false;
			headers.put("Connection", "Close");	
		}
	}
	
	public void setStatusCode(int sc) {
		statusCode = sc;
	}
	
	public void setHeader(String header, String value) {
		headers.put(header, value);
	}

	public HttpResponse write(String data) {
		byte[] bytes = data.getBytes(Charsets.UTF_8);
		ensureCapacity(bytes.length);
		responseData.put(bytes);
		return this;
	}

	public long flush() {
		if (!headersCreated) {
			String initial = createInitalLineAndHeaders();			
			prepend(initial);
			headersCreated = true;
		}
		if (!key.isWritable()) {
			logger.debug("registrating key for writes");
			try {
				key.channel().register(key.selector(), SelectionKey.OP_READ | SelectionKey.OP_WRITE);
			} catch (ClosedChannelException e) {
				logger.error("ClosedChannelException during flush(): {}", e.getMessage());
				Closeables.closeQuietly(key.channel());
			}
		}
		responseData.flip();	// prepare for write
		protocol.stage(key, responseData);
		logger.debug("{} bytes staged for writing", responseData.limit());
		long bytesFlushed = responseData.limit();
		responseData = ByteBuffer.allocate(WRITE_BUFFER_SIZE);
		return bytesFlushed;
	}
	
	public long finish() {
		long bytesWritten = 0;
		SocketChannel clientChannel = (SocketChannel) key.channel();
		if (clientChannel.isOpen()) {
			if (!headersCreated) {
				setEtagAndContentLength();
			}
			bytesWritten = flush();
		}	
		return bytesWritten;
	}
	
	private void setEtagAndContentLength() {
		if (responseData.position() > 0) {
			setHeader("Etag", HttpUtil.getEtag(responseData.array()));
		}
		setHeader("Content-Length", String.valueOf(responseData.position()));
	}
	
	private String createInitalLineAndHeaders() {
		StringBuilder sb = new StringBuilder(HttpUtil.createInitialLine(statusCode));
		for (Map.Entry<String, String> header : headers.entrySet()) {
			sb.append(header.getKey());
			sb.append(": ");
			sb.append(header.getValue());
			sb.append("\r\n");
		}
		
		sb.append("\r\n");
		return sb.toString();
	}
	
	/**
	 * Ensures that its safe to append size data to responseData. If we need to allocate a new ButeBuffer, the new
	 * buffer will be twice as large as the old one.
	 * @param size The size of the data that is about to be appended.
	 */
	private void ensureCapacity(int size) {
		int remaining = responseData.remaining();
		if (size > remaining) {
			// allocate new ByteBuffer
			logger.debug("allocation new responseData buffer.");
			int newSize = Math.max(2 * responseData.capacity(), 2 * size);
			allocate(newSize);
		}
	}

	private void allocate(int newCapacity) {
		byte[] newBuffer = new byte[newCapacity];
		System.arraycopy(responseData.array(),0 , newBuffer, 0, responseData.position());
		responseData = ByteBuffer.wrap(newBuffer);
		logger.debug("allocated new responseData buffer, new size: {}", newBuffer.length);
	}
	
	private void prepend(String data) {
		byte[] bytes = data.getBytes(Charsets.UTF_8);
		int newSize = bytes.length + responseData.position();
		byte[] newBuffer = new byte[newSize];
		System.arraycopy(bytes, 0, newBuffer, 0, bytes.length);	// initial line and headers
		System.arraycopy(responseData.array(), 0, newBuffer, bytes.length, responseData.position()); // body
		responseData = ByteBuffer.wrap(newBuffer);
		responseData.position(newSize);
	}

	/**
	 * @param file Static resource/file to send
	 */
	public long write(File file) {
		//setHeader("Etag", HttpUtil.getEtag(file));
		setHeader("Content-Length", String.valueOf(file.length()));
		long bytesWritten = 0;
		flush();	// write initial line + headers
		try {
			SocketChannel clientChannel = (SocketChannel) key.channel();
			bytesWritten = new RandomAccessFile(file, "r").getChannel().transferTo(0, file.length(), clientChannel);
		} catch (IOException e) {
			logger.error("Error writing (static file) response: {}", e.getMessage());
		}
		return bytesWritten;
	}

	
}
