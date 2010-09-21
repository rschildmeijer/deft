package org.deft.web.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpResponse {
	
	private final static Logger logger = LoggerFactory.getLogger(HttpProtocolImpl.class);
	
	private final static Charset CHAR_SET = Charset.forName("US-ASCII");
	
	private final SocketChannel clientChannel;
	
	public HttpResponse(SocketChannel sc) {
		clientChannel = sc;
	}

	public void write(String data) {
		data += (char)(10);	// "\\r"
		data += (char)(13);	// "\\n"
		ByteBuffer output = ByteBuffer.wrap(data.getBytes(CHAR_SET));
//		output.flip();
		try {
			long bytesWritten = clientChannel.write(output);
		} catch (IOException e) {
			logger.error("Error writing Http response");
		}
	}
	
	
	
}
