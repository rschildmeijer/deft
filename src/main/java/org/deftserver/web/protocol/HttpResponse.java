package org.deftserver.web.protocol;

import java.io.File;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

import org.deftserver.util.Closeables;
import org.deftserver.util.DateUtil;
import org.deftserver.util.HttpUtil;
import org.deftserver.web.buffer.DynamicByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

public class HttpResponse {
	
	private final static Logger logger = LoggerFactory.getLogger(HttpProtocol.class);
	
	private final static int WRITE_BUFFER_SIZE = 1500;	// in bytes

	private final HttpProtocol protocol;
	private final SelectionKey key;
	
	private int statusCode = 200;	// default response status code
	
	private final Map<String, String> headers = new HashMap<String, String>();
	private boolean headersCreated = false;
	private DynamicByteBuffer responseData = DynamicByteBuffer.allocate(WRITE_BUFFER_SIZE);
	
	public HttpResponse(HttpProtocol protocol, SelectionKey key, boolean keepAlive) {
		this.protocol = protocol;
		this.key = key;
		headers.put("Server", "DeftServer/0.2.0-SNAPSHOT");
		headers.put("Date", DateUtil.getCurrentAsString());
		headers.put("Connection", keepAlive ? "Keep-Alive" : "Close");
	}
	
	public void setStatusCode(int sc) {
		statusCode = sc;
	}
	
	public void setHeader(String header, String value) {
		headers.put(header, value);
	}

	public HttpResponse write(String data) {
		byte[] bytes = data.getBytes(Charsets.UTF_8);
		responseData.put(bytes);
		return this;
	}

	public long flush() {
		if (!headersCreated) {
			String initial = createInitalLineAndHeaders();			
			responseData.prepend(initial);
			headersCreated = true;
		}
		
		logger.debug("registrating key for writes");
		try {
			key.channel().register(key.selector(), /*SelectionKey.OP_READ | */SelectionKey.OP_WRITE);
		} catch (ClosedChannelException e) {
			logger.error("ClosedChannelException during flush(): {}", e.getMessage());
			Closeables.closeQuietly(key.channel());
		}
		
		key.attach(responseData);
		logger.debug("{} bytes staged for writing", responseData.position());
		long bytesFlushed = responseData.position();
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
	 * @param file Static resource/file to send
	 */
	public void write(File file) {
		//setHeader("Etag", HttpUtil.getEtag(file));
		setHeader("Content-Length", String.valueOf(file.length()));
		flush();	// write initial line + headers
		protocol.stage(key, file);
	}
	
}
