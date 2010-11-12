package org.deftserver.web.handler;

import java.util.HashMap;
import java.util.Map;

import org.deftserver.web.Asynchronous;
import org.deftserver.web.HttpVerb;
import org.deftserver.web.http.HttpRequest;
import org.deftserver.web.http.HttpResponse;

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
			Class<?>[] parameterTypes = {HttpRequest.class, HttpResponse.class};
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

	//Default implementation of HttpMethods return a 501 page
	public void get(HttpRequest request, HttpResponse response) {
		response.setStatusCode(501);
		response.write("");
	}

	public void post(HttpRequest request, HttpResponse response) { 
		response.setStatusCode(501);
		response.write("");
	}

	public void put(HttpRequest request, HttpResponse response) { 
		response.setStatusCode(501);
		response.write("");
	}

	public void delete(HttpRequest request, HttpResponse response) { 
		response.setStatusCode(501);
		response.write("");
	}

	public void head(HttpRequest request, HttpResponse response) { 
		response.setStatusCode(501);
		response.write("");
	}
}
