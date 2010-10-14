package org.deftserver.web.protocol;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.deftserver.util.ArrayUtil;
import org.deftserver.web.HttpVerb;

import com.google.common.collect.ImmutableMultimap;

public class HttpRequest {

	private final static Charset CHAR_SET = Charset.forName("US-ASCII");

	private final String requestLine;
	private final HttpVerb method;
	private final String requestedPath;	// correct name?
	private final String version; 
	private Map<String, String> headers;
	private ImmutableMultimap<String, String> parameters;
	private String body;
	private boolean keepAlive;

	
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
		this.body = body;
	}
	
	public static HttpRequest of(ByteBuffer buffer) {
		try {
			String raw = new String(buffer.array(), CHAR_SET);
			String[] headersAndBody = raw.split("\\r\\n\\r\\n"); //TODO fix a better regexp for this
			String[] headerFields = headersAndBody[0].split("\\r\\n");
			headerFields = ArrayUtil.dropFromEndWhile(headerFields, "");

			String requestLine = headerFields[0];
			Map<String, String> generalHeaders = new HashMap<String, String>();
			for (int i = 1; i < headerFields.length; i++) {
				String[] header = headerFields[i].split(": ");
				generalHeaders.put(header[0], header[1]);
			}

			String body = "";
			for (int i=1; i<headersAndBody.length; ++i) { //First entry contains headers
				body += headersAndBody[i];
			}

			return new HttpRequest(requestLine, generalHeaders, body);
		} catch (Exception t) {
			return MalFormedHttpRequest.instance;
		}
	}

	public String getRequestLine() {
		return requestLine;
	}
	
	public String getRequestedPath() {
		return requestedPath;
	}

	public String getVersion() {
		return version;
	}
	
	public Map<String, String> getHeaders() {
		return Collections.unmodifiableMap(headers);
	}
	
	public String getHeader(String name) {
		return headers.get(name);
	}
	
	public HttpVerb getMethod() {
		return method;
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
		String result = "METHOD: " + method + "\n";
		result += "VERSION: " + version + "\n";
		result += "PATH: " + requestedPath + "\n";
		
		result += "--- HEADER --- \n";
		for (String key : headers.keySet()) {
			String value = headers.get(key);
			result += key + ":" + value + "\n";
		}
		
		result += "--- PARAMETERS --- \n";
		for (String key : parameters.keySet()) {
			Collection<String> values = parameters.get(key);
			for (String value : values) {
				result += key + ":" + value + "\n";
			}
		}
		return result;
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
