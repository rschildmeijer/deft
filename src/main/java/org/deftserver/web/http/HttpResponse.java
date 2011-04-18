package org.deftserver.web.http;

import static org.deftserver.web.http.HttpServerDescriptor.WRITE_BUFFER_SIZE;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

import org.deftserver.io.buffer.DynamicByteBuffer;
import org.deftserver.util.Closeables;
import org.deftserver.util.DateUtil;
import org.deftserver.util.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

public class HttpResponse {
	
	private final static Logger logger = LoggerFactory.getLogger(HttpResponse.class);
	
	private final HttpProtocol protocol;
	private final SelectionKey key;
	
	private int statusCode = 200;	// default response status code
	
	private final Map<String, String> headers = new HashMap<String, String>();
	private boolean headersCreated = false;
	private DynamicByteBuffer responseData = DynamicByteBuffer.allocate(WRITE_BUFFER_SIZE);
	
	public HttpResponse(HttpProtocol protocol, SelectionKey key, boolean keepAlive) {
		this.protocol = protocol;
		this.key = key;
		headers.put("Server", "Deft/0.3.0-SNAPSHOT");
		headers.put("Date", DateUtil.getCurrentAsString());
		headers.put("Connection", keepAlive ? "Keep-Alive" : "Close");
	}
	
	public void setStatusCode(int sc) {
		statusCode = sc;
	}
	
	public void setHeader(String header, String value) {
		headers.put(header, value);
	}

	/**
	 * The given data data will be sent as the HTTP response upon next flush or when the response is finished.
	 *
	 * @return this for chaining purposes.
	 */
	public HttpResponse write(String data) {
		byte[] bytes = data.getBytes(Charsets.UTF_8);
		responseData.put(bytes);
		return this;
	}

	/**
	 * Explicit flush. 
	 * 
	 * @return the number of bytes that were actually written as the result of this flush.
	 */
	public long flush() {
		if (!headersCreated) {
			String initial = createInitalLineAndHeaders();			
			responseData.prepend(initial);
			headersCreated = true;
		}

		SocketChannel channel = (SocketChannel) key.channel();
		responseData.flip();	// prepare for write
		try {
			channel.write(responseData.getByteBuffer());
		} catch (IOException e) {
			logger.error("ClosedChannelException during channel.write(): {}", e.getMessage());
			Closeables.closeQuietly(key.channel());
		}
		long bytesFlushed = responseData.position();

		if (responseData.hasRemaining()) { 
			responseData.compact();	// make room for more data be "read" in
			try {
				key.channel().register(key.selector(), SelectionKey.OP_WRITE);
			} catch (ClosedChannelException e) {
				logger.error("ClosedChannelException during flush(): {}", e.getMessage());
				Closeables.closeQuietly(key.channel());
			}
			key.attach(responseData);
		} else {
			responseData.clear();
		}
		return bytesFlushed;
	}
	
	/**
	 * Should only be invoked by third party asynchronous request handlers 
	 * (or by the Deft framework for synchronous request handlers). 
	 * If no previous (explicit) flush is invoked, the "Content-Length" and "Etag" header will be calculated and 
	 * inserted to the HTTP response.
	 * 
	 */
	public long finish() {
		long bytesWritten = 0;
		SocketChannel clientChannel = (SocketChannel) key.channel();
		if (clientChannel.isOpen()) {
			if (!headersCreated) {
				setEtagAndContentLength();
			}
			bytesWritten = flush();
		}
		
		// close (or register for read) iff 
		// (a) DBB is attached but all data is sent to wire (hasRemaining == false)
		// (b) no DBB is attached (never had to register for write)
		if (key.attachment() instanceof DynamicByteBuffer) {
			DynamicByteBuffer dbb = (DynamicByteBuffer) key.attachment();
			if (!(dbb).hasRemaining()) {
				protocol.closeOrRegisterForRead(key);
			} 
		} else {
			protocol.closeOrRegisterForRead(key);
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
	 * Experimental support.
	 * Before use, read https://github.com/rschildmeijer/deft/issues/75
	 */
	public long write(File file) {
		//setHeader("Etag", HttpUtil.getEtag(file));
		setHeader("Content-Length", String.valueOf(file.length()));
		long bytesWritten = 0;
		flush(); // write initial line + headers
		try {
			bytesWritten = new RandomAccessFile(file, "r").getChannel().transferTo(0, file.length(), (SocketChannel) key.channel());
			logger.debug("sent file, bytes sent: {}", bytesWritten);
		} catch (IOException e) {
			logger.error("Error writing (static file) response: {}", e.getMessage());
		}
		return bytesWritten;
	}
	
}
