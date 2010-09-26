package org.deft.web.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

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
	private /*<> AtomicBoolean */ boolean initialResponseLineSent = false;
	
	public HttpResponse(SocketChannel sc) {
		clientChannel = sc;
		headers.put("Server", "DeftServer/0.0.1");
	}
	
	public void setStatusCode(int sc) {
		statusCode = sc;
	}
	
	public void setHeader(String header, String value) {
		headers.put(header, value);
	}

	public void write(String data) {
		maybeSendInitalLineAndHeaders();
		
		data += (char)(10);	// "\\r"
		data += (char)(13);	// "\\n"
		ByteBuffer output = ByteBuffer.wrap(data.getBytes(CHAR_SET));
		try {
			long bytesWritten = clientChannel.write(output);
		} catch (IOException e) {
			logger.error("Error writing Http response");
		}
	}
	
	private /*<> synchronzied */ void maybeSendInitalLineAndHeaders() {
		if (!initialResponseLineSent) {
			initialResponseLineSent = true;
			write(HttpUtil.createInitialLine(statusCode));
			for (Map.Entry<String, String> header : headers.entrySet()) {
				write(header.getKey() + ": " + header.getValue());
			}
		}
	}

	public void finish() {
		try {
			clientChannel.close();
		} catch (IOException ioe) {
			logger.error("Could not close client (SocketChannel) connection. {}", ioe);
		}
	}
	
}
