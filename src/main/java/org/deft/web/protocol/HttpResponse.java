package org.deft.web.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.deft.util.DateUtil;
import org.deft.util.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpResponse {
	
	private final static Logger logger = LoggerFactory.getLogger(HttpProtocolImpl.class);
	
	private final static Charset CHAR_SET = Charset.forName("US-ASCII");
	
	private final SocketChannel clientChannel;
	
	private /*<> AtomicInteger */ int statusCode = 200;	// default response status code
	
	//<> TODO RS 100924 could experiment with cliff clicks high scale lib (e.g. NonBlockingHashMap) instead of
	//<> the CCHM used below 
	//<> private final ConcurrentMap<String, String> headers = new ConcurrentHashMap<String, String>();
	
	private final Map<String, String> headers = new HashMap<String, String>();
	private boolean headersCreated = false;
	private String responseData = "";
	
	public HttpResponse(SocketChannel sc) {
		clientChannel = sc;
		headers.put("Server", "DeftServer/0.0.1");
		headers.put("Date", DateUtil.getCurrentAsString());
	}
	
	public void setStatusCode(int sc) {
		statusCode = sc;
	}
	
	public void setHeader(String header, String value) {
		headers.put(header, value);
	}

	public HttpResponse write(String data) {
		responseData +=data;
		return this;
	}
		
	public long flush() {
		if (!headersCreated) {
			String initial = createInitalLineAndHeaders();			
			responseData = initial + responseData;
			headersCreated = true;
		}
		ByteBuffer output = ByteBuffer.wrap(responseData.getBytes(CHAR_SET));
		long bytesWritten = 0;
		try {
			bytesWritten = clientChannel.write(output);
		} catch (IOException e) {
			logger.error("Error writing response: {}", e);
		} finally {
			responseData = "";
		}
		return bytesWritten;
	}
	
	public long finish() {
		long bytesWritten = 0;
		if (clientChannel.isOpen()) {
			bytesWritten = flush();
		}	
		try {
			clientChannel.close();
		} catch (IOException ioe) {
			logger.error("Could not close client (SocketChannel) connection. {}", ioe);
		}
		return bytesWritten;
	}
	
	private /*<> synchronzied */ String createInitalLineAndHeaders() {
		StringBuilder sb = new StringBuilder(HttpUtil.createInitialLine(statusCode));
		for (Map.Entry<String, String> header : headers.entrySet()) {
			sb.append(header.getKey());
			sb.append(" :");
			sb.append(header.getValue());
			sb.append("\r\n");
		}
		sb.append("\r\n");
		return sb.toString();
	}

	
}
