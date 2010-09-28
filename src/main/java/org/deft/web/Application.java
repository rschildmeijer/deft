package org.deft.web;

import java.util.Map;

import org.deft.web.handler.RequestHandler;

import com.google.common.collect.ImmutableMap;

public class Application {
	
	private final ImmutableMap<String, RequestHandler> handlers;
	private final ImmutableMap<String, RequestHandler> capturingHandlers;

	public Application(Map<String, RequestHandler> handlers) {
		ImmutableMap.Builder<String, RequestHandler> builder = new ImmutableMap.Builder<String, RequestHandler>();
		ImmutableMap.Builder<String, RequestHandler> capturingBuilder = new ImmutableMap.Builder<String, RequestHandler>();
		for (String path : handlers.keySet()) {
			int index = path.lastIndexOf("/");
			String group = path.substring(index+1, path.length());
			if (containsCapturingGroup(group)) {
				// path ends with capturing group, e.g path == "/person/([0-9]+)"
				capturingBuilder.put(path.substring(0, index+1), handlers.get(path));
			} else {
				// "normal" path, e.g. path == "/"
				builder.put(path, handlers.get(path));
			}
		}
		this.handlers = builder.build();
		this.capturingHandlers = capturingBuilder.build();
	}
	
	public RequestHandler getHandler(String path) {
		RequestHandler rh = handlers.get(path);
		if (rh == null) {
			// path could contain capturing groups which we could have a handler associated with.
			int index = path.lastIndexOf("/");
			if (index != -1) {
				String init = path.substring(0, index+1);	// path without its last segment
				rh = capturingHandlers.get(init);
			}
		} 
		return rh;
	}
	
	private boolean containsCapturingGroup(String group) {
		return group.matches("^\\(.*\\)$");	// TODO RS 100928 maybe we should verify that the string inside the capturing
											// is a regular expression. 
	}

}
