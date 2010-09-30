package org.deft.web;

import java.util.Map;
import java.util.regex.Pattern;

import org.deft.web.handler.RequestHandler;

import com.google.common.collect.ImmutableMap;

public class Application {

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
		this.absoluteHandlers = builder.build();
		this.capturingHandlers = capturingBuilder.build();
		this.patterns = patternsBuilder.build();
	}

	public RequestHandler getHandler(String path) {
		RequestHandler rh = absoluteHandlers.get(path);
		if (rh == null) {
			// path could contain capturing groups which we could have a handler associated with.
			rh = getCapturingHandler(path);
		} 
		return rh;
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

}
