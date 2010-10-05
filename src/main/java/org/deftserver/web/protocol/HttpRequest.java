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

	public HttpRequest(String requestLine, Map<String, String> headers) {
		this.requestLine = requestLine;
		String[] elements = requestLine.split(" ");
		method = HttpVerb.valueOf(elements[0]);
		requestedPath = elements[1];
		version = elements[2];
		this.headers = headers;
		parameters = parseParameters(requestedPath);
	}
	
	public static HttpRequest of(ByteBuffer buffer) {
		String raw = new String(buffer.array(), CHAR_SET);
		String[] fields = raw.split("\\r\\n");
		fields = ArrayUtil.dropFromEndWhile(fields, "");
		
		String requestLine = fields[0];
		Map<String, String> generalHeaders = new HashMap<String, String>();
		for (int i = 1; i < fields.length; i++) {
			String[] header = fields[i].split(": ");
			generalHeaders.put(header[0], header[1]);
		}
		return new HttpRequest(requestLine, generalHeaders);
	}

	public String getRequestLine() {
		return requestLine;
	}
	
	public String getRequestedPath() {
		return requestedPath;
	}

	public Map<String, String> getHeaders() {
		return Collections.unmodifiableMap(headers);
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
	
	/**
	 * Returns a collection of all values associated with the provided parameter.
	 * If no values are found and empty collection is returned.
	 */
	public Collection<String> getParameterValues(String name) {
		return parameters.get(name);
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
	
}
