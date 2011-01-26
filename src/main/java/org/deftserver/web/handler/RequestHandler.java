package org.deftserver.web.handler;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.deftserver.web.Asynchronous;
import org.deftserver.web.Authenticated;
import org.deftserver.web.HttpVerb;
import org.deftserver.web.http.HttpRequest;
import org.deftserver.web.http.HttpResponse;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public abstract class RequestHandler {

	private final ImmutableMap<HttpVerb, Boolean> asynchVerbs;
	private final ImmutableMap<HttpVerb, Boolean> authVerbs;

	public RequestHandler() {
		Map<HttpVerb, Boolean> asyncV = Maps.newHashMap();
		Map<HttpVerb, Boolean> authV = Maps.newHashMap();
		for (HttpVerb verb : HttpVerb.values()) {
			authV.put(verb, isMethodAnnotated(verb, Authenticated.class));
			asyncV.put(verb, isMethodAnnotated(verb, Asynchronous.class));
		}
		asynchVerbs = ImmutableMap.copyOf(asyncV);
		authVerbs = ImmutableMap.copyOf(authV);
	}

	private boolean isMethodAnnotated(HttpVerb verb, Class<? extends Annotation> annotation) {
		try {
			Class<?>[] parameterTypes = {HttpRequest.class, HttpResponse.class};
			return getClass().getMethod(verb.toString().toLowerCase(), parameterTypes).getAnnotation(annotation) != null; 
		} catch (NoSuchMethodException nsme) {
			return false;
		}
	}
	
	public boolean isMethodAsynchronous(HttpVerb verb) {
		return asynchVerbs.get(verb);
	}
	
	public boolean isMethodAuthenticated(HttpVerb verb) {
		return authVerbs.get(verb);
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
	
	public String getCurrentUser(HttpRequest request) { 
		return null; 
	}

}
