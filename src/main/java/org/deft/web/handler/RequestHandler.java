package org.deft.web.handler;

import java.util.HashMap;
import java.util.Map;

import org.deft.web.Asynchronous;
import org.deft.web.HttpVerb;

import org.deft.web.protocol.HttpRequest;
import org.deft.web.protocol.HttpResponse;

import com.google.common.collect.ImmutableMap;

public abstract class RequestHandler {

	private final ImmutableMap<HttpVerb, Boolean> asynchVerbs;

	public RequestHandler() {
		Map<HttpVerb, Boolean> av = new HashMap<HttpVerb, Boolean>();
		av.put(HttpVerb.GET, hasAsynchronousMethod(HttpVerb.GET));
		av.put(HttpVerb.POST, hasAsynchronousMethod(HttpVerb.POST));
		av.put(HttpVerb.PUT, hasAsynchronousMethod(HttpVerb.PUT));
		av.put(HttpVerb.DELETE, hasAsynchronousMethod(HttpVerb.DELETE));
		asynchVerbs = ImmutableMap.copyOf(av);
	}

	private boolean hasAsynchronousMethod(HttpVerb verb) {
		try {
			Class<?>[] parameterTypes = new Class<?>[] {HttpRequest.class, HttpResponse.class};
			switch (verb) {
			case GET:
				return getClass().getMethod("get", parameterTypes).getAnnotation(Asynchronous.class) != null;
			case POST:
				return getClass().getMethod("post", parameterTypes).getAnnotation(Asynchronous.class) != null;
			case PUT:
				return getClass().getMethod("put", parameterTypes).getAnnotation(Asynchronous.class) != null;
			default: /* DELETE:*/
				return getClass().getMethod("delete", parameterTypes).getAnnotation(Asynchronous.class) != null; 
			}
		} catch (NoSuchMethodException nsme) {
			return false;
		}
	}

	public boolean isMethodAsynchronous(HttpVerb verb) {
		return asynchVerbs.get(verb);
	}

	public void get(HttpRequest request, HttpResponse response) { /* default nop */ }

	public void post(HttpRequest request, HttpResponse response) { /* default nop */ }

	public void put(HttpRequest request, HttpResponse response) { /* default nop */ }

	public void delete(HttpRequest request, HttpResponse response) { /* default nop */ }

	public void head(HttpRequest request, HttpResponse response) { /* default nop */ }

}
