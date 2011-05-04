package org.deftserver.web.http.client;

import java.net.MalformedURLException;
import java.net.URL;

import org.deftserver.web.HttpVerb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Request {
	
	private static final Logger logger = LoggerFactory.getLogger(Request.class);

	private final URL url;
	private final HttpVerb verb;
	
	public Request(String url, HttpVerb verb) {
		try {
			this.url = new URL(url);
			this.verb = verb;
		} catch (MalformedURLException e) {
			logger.error("Malformed URL: {}", e.getMessage());
			throw new RuntimeException(e);
		}
	}

	public URL getURL() {
		return url;
	}
	
	/**
	 * 
	 * @return The verb (method) name, e.g. "GET" or "POST"
	 */
	public String getVerb() {
		return verb.name();
	}
	
}
