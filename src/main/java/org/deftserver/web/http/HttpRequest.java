package org.deftserver.web.http;


import java.nio.ByteBuffer;

import java.util.Collection;
import java.util.Collections;

import java.util.Map;

import org.deftserver.io.buffer.DynamicByteBuffer;

import org.deftserver.web.HttpVerb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



import com.google.common.collect.ImmutableMultimap;


public class HttpRequest {
	
	private static final Logger LOG = LoggerFactory.getLogger(HttpRequest.class);
	
	private static final HttpRequestParser parser = new HttpRequestParser();
	
	private final String requestLine;
	private final HttpVerb method;
	private final String requestedPath;	// correct name?
	private final String version; 
	protected Map<String, String> headers;
	private ImmutableMultimap<String, String> parameters;

	private boolean keepAlive;

    
	private String bodyString;
	private DynamicByteBuffer body;
	protected int contentLength;
	
	public HttpRequest(String[] requestLine, Map<String, String> headers, DynamicByteBuffer  _body) {
		this.requestLine = new StringBuffer(requestLine[0]).append(' ').append(requestLine[1]).append(' ').append(requestLine[2]).toString();
		
		method = HttpVerb.valueOf(requestLine[0]);
		requestedPath = requestLine[1];
		version = requestLine[2];
		this.headers = headers;	
		body = _body;
		initKeepAlive();
		parameters = parseParameters(requestedPath);
		if (headers.containsKey("content-length")){
			contentLength = Integer.parseInt(headers.get("content-length").trim());
		}
	}
	
	public HttpRequest(String requestLine, Map<String, String> headers) {
		this.requestLine = requestLine;
		String[] elements = requestLine.split(" ");
		method = HttpVerb.valueOf(elements[0]);
		requestedPath = elements[1];
		version = elements[2];
		this.headers = headers;	
		body = null;
		initKeepAlive();
		parameters = parseParameters(requestedPath);
	}
	
	public HttpRequest(String requestLine, Map<String, String> headers, String body) {
		this(requestLine, headers);
		this.bodyString = body;
	}
	
	public static HttpRequest of(ByteBuffer buffer) {
		try {
			return parser.parseRequestBuffer(buffer);
	/*		
			String raw =  decoder.decode(buffer).toString();
			
			String[] headersAndBody = raw.split("\\r\\n\\r\\n"); //
			String[] headerFields = headersAndBody[0].split("\\r\\n");
			headerFields = ArrayUtil.dropFromEndWhile(headerFields, "");
            
			String requestLine = headerFields[0];
			Map<String, String> generalHeaders = new HashMap<String, String>();
			for (int i = 1; i < headerFields.length; i++) {
				String[] header = headerFields[i].split(": ");
				generalHeaders.put(header[0].toLowerCase(), header[1]);
			}
            
			String body = "";
			for (int i = 1; i < headersAndBody.length; ++i) { //First entry contains headers
				body += headersAndBody[i];
			}
			
			if (requestLine.contains("POST")) {
				int contentLength = Integer.parseInt(generalHeaders.get("content-length"));
				if (contentLength > body.length()) {
					return new PartialHttpRequest(requestLine, generalHeaders, body);
				}
			}
			return new HttpRequest(requestLine, generalHeaders, body);*/
		} catch (Exception t) {
			LOG.error("Bad HTTP format", t);
			return MalFormedHttpRequest.instance;
		}
	}
	
	public static HttpRequest continueParsing(ByteBuffer buffer, PartialHttpRequest unfinished) {

		return parser.parseRequestBuffer(buffer, unfinished);		
/*		String nextChunk = null;
		try {
			nextChunk = decoder.decode(buffer).toString();
		} catch (CharacterCodingException e) {
			LOG.error("Bad encoding while reading to request body", e);
		}
		unfinished.appendBody(nextChunk);
		
		int contentLength = Integer.parseInt(unfinished.getHeader("Content-Length"));
		if (contentLength > unfinished.getBody().length()) {
			return unfinished;
		} else {
			return new HttpRequest(unfinished.getRequestLine(), unfinished.getHeaders(), unfinished.getBody());
		}*/
	}
	
	public String getRequestLine() {
		return requestLine;
	}
	
	public String getRequestedPath() {
		return requestedPath;
	}
    
	public boolean isFinished(){
		return true;
	}
	
	public String getVersion() {
		return version;
	}
	
	public Map<String, String> getHeaders() {
		return Collections.unmodifiableMap(headers);
	}
	
	public String getHeader(String name) {
		return headers.get(name.toLowerCase());
	}
	
	public HttpVerb getMethod() {
		return method;
	}
	
	public int getContentLength() {
		return contentLength;
	}
	
	/**
	 * Returns the value of a request parameter as a String, or null if the parameter does not exist. 
	 *
	 * You should only use this method when you are sure the parameter has only one value. If the parameter 
	 * might have more than one value, use getParameterValues(java.lang.String).
     * If you use this method with a multi-valued parameter, the value returned is equal to the first value in
     * the array returned by getParameterValues. 
	 */
	public String getParameter(String name) {
		Collection<String> values = parameters.get(name);		
		return values.isEmpty() ? null : values.iterator().next();
	}
	
	public Map<String, Collection<String>> getParameters() {
		return parameters.asMap();
	}	
	
	public String getBody() {
		if (body != null){

				return new String(body.getByteBuffer().array(), 0, body.getByteBuffer().limit());

		}
		return bodyString;
	}
	
	public DynamicByteBuffer getBodyBuffer(){
		return body;
	}
	
	
	/**
	 * Returns a collection of all values associated with the provided parameter.
	 * If no values are found and empty collection is returned.
	 */
	public Collection<String> getParameterValues(String name) {
		return parameters.get(name);
	}
	
	public boolean isKeepAlive() {
		return keepAlive;
	}
	
	@Override
	public String toString() {
		
		StringBuilder sb = new StringBuilder();
		sb.append("METHOD: ").append(method).append("\n");
		sb.append("VERSION: ").append(version).append("\n");
		sb.append("PATH: ").append(requestedPath).append("\n");
		
		sb.append("--- HEADER --- \n");
		for (String key : headers.keySet()) {
			sb.append(key).append(":").append( headers.get(key)).append("\n");
		}
		
		sb.append("--- PARAMETERS --- \n");
		for (String key : parameters.keySet()) {
			Collection<String> values = parameters.get(key);
			for (String value : values) {
				sb.append(key).append( ":").append(value).append("\n");
			}
		}
		
		sb.append("---- BODY ---- \n");
		sb.append(this.getBody());
		return sb.toString();
	}
	
	private ImmutableMultimap<String, String> parseParameters(String requestLine) {
		ImmutableMultimap.Builder<String, String> builder = ImmutableMultimap.builder();
		String[] str = requestLine.split("\\?");
		
		//Parameters exist
		if (str.length > 1) {
			String[] paramArray = str[1].split("\\&|;"); //Delimiter is either & or ;
			for (String keyValue : paramArray) {
				String[] keyValueArray = keyValue.split("=");
				
				//We need to check if the parameter has a value associated with it.
				if (keyValueArray.length > 1) {
					builder.put(keyValueArray[0], keyValueArray[1]); //name, value
				}
			}
		}
		return builder.build();
	}
	
	
	private void initKeepAlive() {
		String connection = getHeader("Connection");
		if ("keep-alive".equalsIgnoreCase(connection)) { 
			keepAlive = true;
		} else if ("close".equalsIgnoreCase(connection) || requestLine.contains("1.0")) {
			keepAlive = false;
		} else {
			keepAlive = true;
		}
	}
    
}
