package org.deftserver.web.http;

import java.nio.ByteBuffer;
import java.util.Map;

import org.deftserver.io.buffer.DynamicByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;


public class HttpRequestParser {
	
	private static final Logger LOG = LoggerFactory.getLogger(HttpRequestParser.class);

	private final ByteBuffer buffer;
	
	private byte currentChar;
	
	private boolean headersCompleted = false;
	
	private HttpRequest result = null;
	
	public HttpRequestParser(HttpRequest request, ByteBuffer buffer) {
		this( buffer);
		this.result = request;
	}
	
	public HttpRequestParser(ByteBuffer buffer) {
		this.buffer = buffer;
		if (LOG.isDebugEnabled()){
			LOG.debug("Building new HTTPParser for buffer:{}", buffer);
		}
	}
	
	public HttpRequest parseRequestBuffer(){

		if (result == null){
			parse();
			
		}
		else {
			continueParsing();
		}
		buffer.clear();
		return result;
	}

	
 
	private void continueParsing(){
		
		LOG.debug("Continue parsing - buffer.position():{} - buffer.limit():{}, new size:{}", new Object[]{buffer.position(), buffer.limit(), result.getBodyBuffer().position()});
		result.getBodyBuffer().getByteBuffer().put(buffer.array(), buffer.position(), buffer.limit());

			if (result.getContentLength() <= result.getBodyBuffer().position()) {
				
				((PartialHttpRequest)result).finish();
			}
			LOG.debug("Continue Parsing request|finished:{}|content-length:{}|buffer-size:{}", new Object[] {result.isFinished(),result.getContentLength(), result.getBodyBuffer().position()});
		
	}
	
	
	private void parse(){

		if (!buffer.hasRemaining()){
			return;
		}
		
		LOG.debug("Start Parsing request");
		byte c = skipWhiteSpaceAndLine();
		LOG.debug("After white space char is {}", c);
		// Read request line First meaning line of the request
		String [] requestLine = this.readRequestLine(c);
		LOG.debug("After request line is {} {} {}", requestLine);
		Map<String, String> headers = Maps.newHashMap();
		DynamicByteBuffer body = DynamicByteBuffer.allocate(buffer.capacity()); 
		while (buffer.hasRemaining()){
			c = skipEndOfLine();
			if (headersCompleted)
				break;
			String name = readHeaderName(c);
			String value = readHeaderValue(currentChar);
			LOG.debug("After header:  {}{}", name, value);
			headers.put(name, value);
		}
		
		 // All headers parsed
		if (headersCompleted){
			LOG.debug("All header read - buffer.position():{} - buffer.limit():{}, new size:{}", new Object[]{buffer.position(), buffer.limit(), buffer.limit() - buffer.position()});
			System.arraycopy(buffer.array(),  buffer.position() -1, body.array(), 0, buffer.limit() - buffer.position() +1);
			body.getByteBuffer().position(buffer.limit() - buffer.position()+1);
			if (requestLine[0].equalsIgnoreCase("POST") || requestLine[0].equalsIgnoreCase("PUT")) {
				int contentLength = Integer.parseInt(headers.get("content-length").trim());
				LOG.debug("content-length is {} and position is {}", contentLength, body.position());
				if (contentLength > body.position()) {
					
					result = new PartialHttpRequest(requestLine, headers, body);
					LOG.debug("building PartialHttpRequest for incomplete request ");
				}
			} 
			
			if (result == null){
				result = new HttpRequest(requestLine, headers, body);
				LOG.debug("building HttpRequest for complete request");
			}
		}
		
		if (result == null){
			LOG.debug("Bad HTTP request received");
			result = MalFormedHttpRequest.instance;
		}
		else {
		LOG.debug("End parsing request|finished:{}|content-length:{}|buffer-size:{}", new Object[] {
				 result.isFinished(),
				 result.getContentLength(),
				 result.getBodyBuffer().getByteBuffer().position()});
		}
	}
	
	/**
	 * Reads a header value from buffer
	 * @param c
	 * @return
	 */
	private String readHeaderValue(byte c){
		
		return readLine(c);
	}
	
	/**
	 * Reads a header name from buffer current position
	 * @param c
	 * @return
	 */
	private String readHeaderName(byte c){
		
		StringBuffer buff = new StringBuffer(50);
		while ((c != ':') && buffer.hasRemaining()){
			
			buff.append((char)c);
			
			c = buffer.get();
		}
		currentChar = c;
		return buff.toString().toLowerCase();
	}
	
	/**
	 * Reads the buffer till end of line
	 * @param c
	 * @return
	 */
	private String readLine(byte c){
		StringBuffer buff = new StringBuffer(200);
		c = buffer.get();
		while (((c != '\r' )&&(c != '\n')) && buffer.hasRemaining()){
			buff.append((char)c);
			c = buffer.get();
		}
		currentChar = c;
		return buff.toString();
	}
	
	/**
	 * Parse the request line
	 * @param c
	 * @return
	 */
	private String[] readRequestLine (byte c){
		String [] res = new String [3];
		int i = 0;
		StringBuffer buff = new StringBuffer(255);
		while (((c != '\r' )&& (c != '\n')) && buffer.hasRemaining()){
			if (c == ' '){
				res [i] = buff.toString();
				buff.delete(0, buff.length());
				i++;
			}else {
				buff.append((char)c);
			}
			c = buffer.get();
		}
		if ((c == '\r' )||(c == '\n')){
			res[i] = buff.toString();
		}
		currentChar = c;
		return res;
	}
	
	private byte skipWhiteSpaceAndLine(){
		
		byte c = buffer.get();
		while(((c == '\r' )||(c == '\n') || (c == ' ') || (c == '\t')) && (buffer.hasRemaining())){
			c = buffer.get();
		}
		return (currentChar = c);
	}

	
	private byte skipEndOfLine(){

		byte c = buffer.get();
		int i = 1;
		while(((c == '\r' )||(c == '\n')) && buffer.hasRemaining() && i < 4){
			c = buffer.get();
			i++;
		}
		
	//	LOG.debug("{} empty lines character read!", i);
		if (i >= 3){
			headersCompleted = true;
		}
		
		return (currentChar = c);
	}
}
