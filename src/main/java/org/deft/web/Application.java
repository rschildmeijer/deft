package org.deft.web;

import java.util.Map;
import java.util.regex.Pattern;

import org.deft.web.handler.RequestHandler;

import com.google.common.collect.ImmutableMap;

public class Application {

	private final ImmutableMap<String, RequestHandler> handlers;

	private final ImmutableMap<String, RequestHandler> capturingHandlers;
	private final ImmutableMap<RequestHandler, Pattern> regexps;


	public Application(Map<String, RequestHandler> handlers) {
		ImmutableMap.Builder<String, RequestHandler> builder = new ImmutableMap.Builder<String, RequestHandler>();
		ImmutableMap.Builder<String, RequestHandler> capturingBuilder = new ImmutableMap.Builder<String, RequestHandler>();
		ImmutableMap.Builder<RequestHandler, Pattern> regexpsBuilder = new ImmutableMap.Builder<RequestHandler, Pattern>();

		for (String path : handlers.keySet()) {
			int index = path.lastIndexOf("/");
			String group = path.substring(index+1, path.length());
			if (containsCapturingGroup(group)) {
				// path ends with capturing group, e.g path == "/person/([0-9]+)"
				capturingBuilder.put(path.substring(0, index+1), handlers.get(path));
				regexpsBuilder.put(handlers.get(path), Pattern.compile(group));
			} else {
				// "normal" path, e.g. path == "/"
				builder.put(path, handlers.get(path));
			}
		}
		this.handlers = builder.build();
		this.capturingHandlers = capturingBuilder.build();
		this.regexps = regexpsBuilder.build();
	}

	public RequestHandler getHandler(String path) {
		RequestHandler rh = handlers.get(path);
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
				Pattern regex = regexps.get(handler);
				if (regex.matcher(group).matches()) {
					return handler;
				}
			}
		}
		return null;
	}

}
