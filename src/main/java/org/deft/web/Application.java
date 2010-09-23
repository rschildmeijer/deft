package org.deft.web;

import java.util.Map;

import org.deft.web.handler.RequestHandler;


import com.google.common.collect.ImmutableMap;

public class Application {
	
	private final ImmutableMap<String, RequestHandler> handlers;

	public Application(Map<String, RequestHandler> handlers) {
		this.handlers = ImmutableMap.copyOf(handlers);
	}
	
	public RequestHandler getHandler(String path) {
		return handlers.get(path);
	}

}
