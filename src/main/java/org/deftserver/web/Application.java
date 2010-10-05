package org.deftserver.web;

import java.util.Map;
import java.util.regex.Pattern;

import org.deftserver.web.handler.NotFoundRequestHandler;
import org.deftserver.web.handler.RequestHandler;

import com.google.common.collect.ImmutableMap;

public class Application {
	
	/**
	 * Default size of the read buffer that the I/O loop allocates per each read
	 */
	private int readBufferSize = 1500;	// in bytes

	/**
	 * "Normal/Absolute" (non group capturing) RequestHandlers
	 * e.g. "/", "/persons"
	 */
	private final ImmutableMap<String, RequestHandler> absoluteHandlers;

	/**
	 * Group capturing RequestHandlers
	 * e.g. "/persons/([0-9]+)", "/persons/(\\d{1,3})"  
	 */
	private final ImmutableMap<String, RequestHandler> capturingHandlers;
	
	/**
	 * A mapping between group capturing RequestHandlers and their corresponding pattern ( e.g. "([0-9]+)" )
	 */
	private final ImmutableMap<RequestHandler, Pattern> patterns;

	public Application(Map<String, RequestHandler> handlers) {
		ImmutableMap.Builder<String, RequestHandler> builder = new ImmutableMap.Builder<String, RequestHandler>();
		ImmutableMap.Builder<String, RequestHandler> capturingBuilder = new ImmutableMap.Builder<String, RequestHandler>();
		ImmutableMap.Builder<RequestHandler, Pattern> patternsBuilder = new ImmutableMap.Builder<RequestHandler, Pattern>();

		for (String path : handlers.keySet()) {
			int index = path.lastIndexOf("/");
			String group = path.substring(index+1, path.length());
			if (containsCapturingGroup(group)) {
				// path ends with capturing group, e.g path == "/person/([0-9]+)"
				capturingBuilder.put(path.substring(0, index+1), handlers.get(path));
				patternsBuilder.put(handlers.get(path), Pattern.compile(group));
			} else {
				// "normal" path, e.g. path == "/"
				builder.put(path, handlers.get(path));
			}
		}
		absoluteHandlers = builder.build();
		capturingHandlers = capturingBuilder.build();
		patterns = patternsBuilder.build();
	}

	/**
	 * 
	 * @param path Requested path
	 * @return Returns the {@link RequestHandler} associated with the given path. If no mapping exists a 
	 * {@link NotFoundRequestHandler} is returned.
	 */
	public RequestHandler getHandler(String path) {
		RequestHandler rh = absoluteHandlers.get(path);
		if (rh == null) {
			// path could contain capturing groups which we could have a handler associated with.
			rh = getCapturingHandler(path);
		} 
		return rh != null ? rh : NotFoundRequestHandler.getInstance();	// TODO RS store in a final field for improved performance?
	}
	
	private boolean containsCapturingGroup(String group) {
		boolean containsGroup =  group.matches("^\\(.*\\)$");
		Pattern.compile(group);	// throws PatternSyntaxException if group is malformed regular expression
		return containsGroup;
	}

	private RequestHandler getCapturingHandler(String path) {
		int index = path.lastIndexOf("/");
		if (index != -1) {
			String init = path.substring(0, index+1);	// path without its last segment
			String group = path.substring(index+1, path.length()); 
			RequestHandler handler = capturingHandlers.get(init);
			if (handler != null) {
				Pattern regex = patterns.get(handler);
				if (regex.matcher(group).matches()) {
					return handler;
				}
			}
		}
		return null;
	}
	
	/**
	 * Sets the size of the read buffer that the I/O loop allocates per each read.
	 * 
	 * "Ideally, an HTTP request should not go beyond 1 packet. 
	 * The most widely used networks limit packets to approximately 1500 bytes, so if you can constrain each request 
	 * to fewer than 1500 bytes, you can reduce the overhead of the request stream." (from: http://bit.ly/bkksUu)
	 */
	public void setReadBufferSize(int readBufferSize) {
		this.readBufferSize = readBufferSize;
	}
	
	/**
	 * 
	 * @return Size of the read buffer that the I/O loop allocates per each read.
	 */
	public int getReadBufferSize() {
		return readBufferSize;
	}

}
