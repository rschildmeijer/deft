package org.deftserver.web.http;

import java.nio.ByteBuffer;
import java.util.Map;

import org.deftserver.io.buffer.DynamicByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;


public class HttpRequestParser {
	
	private static final Logger LOG = LoggerFactory.getLogger(HttpRequestParser.class);


	private boolean headersCompleted = false;
	
	private HttpRequest result = null;
	

	
	public HttpRequest parseRequestBuffer(ByteBuffer buffer){
		return parseRequestBuffer(buffer, null);
	}
	
	public HttpRequest parseRequestBuffer(ByteBuffer buffer,HttpRequest result){
		HttpParser parser= new HttpParser(buffer);
		if (result == null){
			result = parse(parser);
			
		}
		else {
		    continueParsing(parser,result);
		}
		return result;
	}

	
 
	private void continueParsing(HttpParser parser, HttpRequest result){
		
//		LOG.debug("Continue parsing - buffer.position():{} - buffer.limit():{}, new size:{}", new Object[]{buffer.position(), buffer.limit(), result.getBodyBuffer().position()});
		PartialHttpRequest req = (PartialHttpRequest)result;
		if (!req.isHeaderFinished()){	// Finish reading headers 
			if (this.readHeaders(parser, req.getModifiableHeaders())){
				req.finishHeaders();
				int contentLength = processContentLength(req.getModifiableHeaders());

				this.pushRemainingToBody(parser.getBuffer(), result.getBodyBuffer(), contentLength);
				
				if (req.getHeaders().containsKey("content-length")){
					req.setContentLength(contentLength);
				}
			}
			
		}else {	// we just have to put the data in buffer
			this.pushRemainingToBody(parser.getBuffer(),result.getBodyBuffer(), req.getContentLength());
		}
		
		// If body size is equal to content-length, finish the request
		if (result.getContentLength() <= result.getBodyBuffer().position()) {
				
			((PartialHttpRequest)result).finish();
		}
//		LOG.debug("Continue Parsing request|finished:{}|content-length:{}|buffer-size:{}", new Object[] {result.isFinished(),result.getContentLength(), result.getBodyBuffer().position()});
		
	}
	
	
	private HttpRequest parse(HttpParser parser){
		HttpRequest result = null;
		if (!parser.hasRemaining()){
			return result;
		}
		
//		LOG.debug("Start Parsing request");
		if (!parser.skipWhiteSpaceAndLine()){
			return result;
		}
//		LOG.debug("After white space char is {}", c);
		// Read request line First meaning line of the request
		String [] requestLine = parser.readRequestLine();
		if (requestLine == null){
			return result;
		}
//		LOG.debug("After request line is {} {} {}", requestLine);
		Map<String, String> headers = Maps.newHashMap();
		DynamicByteBuffer body = DynamicByteBuffer.allocate(parser.getBuffer().capacity()); 
		headersCompleted = readHeaders(parser, headers);
		
		 // All headers parsed
		if (headersCompleted){
//			LOG.debug("All header read - buffer.position():{} - buffer.limit():{}, new size:{}", new Object[]{buffer.position(), buffer.limit(), buffer.limit() - buffer.position()});
			if (requestLine[0].equalsIgnoreCase("POST") || requestLine[0].equalsIgnoreCase("PUT")) {
				int contentLength = processContentLength(headers);
				
				this.pushRemainingToBody(parser.getBuffer(),body, contentLength);
//				LOG.debug("content-length is {} and position is {}", contentLength, body.position());
				if (contentLength > body.position()) {
					
					result = new PartialHttpRequest(requestLine, headers, body, true);
//					LOG.debug("building PartialHttpRequest for incomplete request ");
				}
			}
			
			if (result == null){
				result = new HttpRequest(requestLine, headers, body);
//				LOG.debug("building HttpRequest for complete request");
			}
		}
		else if (!headers.isEmpty()){
			result = new PartialHttpRequest(requestLine, headers, body);
//			LOG.debug("building PartialHttpRequest for incomplete request ");
		}
		
		if (result == null){
//			LOG.debug("Bad HTTP request received");
			result = MalFormedHttpRequest.instance;
		}
//		else {
//		LOG.debug("End parsing request|finished:{}|content-length:{}|buffer:{}", new Object[] {
//				 result.isFinished(),
//				 result.getContentLength(),
//				 result.getBodyBuffer()});
//		}
		
		return result;
	}
	
	
	private int processContentLength(Map<String, String> headers){
		int contentLength = 0;
		String value = headers.get("content-length");
		if ( value != null){
			try {
				contentLength = Integer.parseInt(headers.get("content-length"));
			} catch (NumberFormatException e) {
				LOG.debug("Bad content length format");
			}
		}
		return contentLength;
	}
	
	private void pushRemainingToBody(ByteBuffer buffer, DynamicByteBuffer body, int clength){
		// If buffer is empty or there is no clength then skip this
		if (clength == 0 || !buffer.hasRemaining()){
			return;
		}
		
		if (body.position() + buffer.remaining() > clength){
			body.put(buffer.array(),  buffer.position(), clength - body.position());
		}
		else {
			body.put(buffer.array(),  buffer.position(), buffer.remaining());
		}
		
		return;
	}
	
	private boolean readHeaders(HttpParser parser, Map<String, String> headers)
	{
		String name = null;
		StringBuilder value = new StringBuilder(255);
		int count = headers.size();
		while (parser.hasRemaining() && count < HttpServerDescriptor.MAX_HEADER_LINE_COUNT){
			headersCompleted = parser.skipEndOfLine();
			if (headersCompleted)
				break;
			
			// Handle multi line header value 
			if (parser.isWhiteSpace()){
				String line = parser.readLine();
				if (line == null){
					return false;
				}
				value.append(line);
			}else { 
				
				if (name !=null ){
					count++;
					this.pushToHeaders(name, value, headers);
				}
				
				name = parser.readHeaderName();
				
				String line = parser.readLine();
				if (line == null || name == null){
					return false;
				}
				value.append(line);
			}
			

//			LOG.debug("After header:  {}{}", name, value);
			
		}
		
		this.pushToHeaders(name, value, headers);
		
		return headersCompleted;
	}
	
	
	private void pushToHeaders(String name, StringBuilder value, Map<String, String> headers){
		StringBuilder newValue = value;
		if (name != null){
			name = name.toLowerCase();
			// Handle repeated header-name like Cookies 
			if (headers.containsKey(name)){
				newValue = new StringBuilder(headers.get(name));
				newValue.append(';').append(value.toString().trim());
			}
			headers.put(name, newValue.toString().trim());
			value.delete(0, Integer.MAX_VALUE);
		}
	}
	

}
