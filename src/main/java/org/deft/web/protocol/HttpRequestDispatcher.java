package org.deft.web.protocol;

import org.deft.web.HttpVerb;
import org.deft.web.handler.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpRequestDispatcher {

	private static final Logger logger = LoggerFactory.getLogger(HttpRequestDispatcher.class);
	
	public static void dispatch(RequestHandler rh, HttpRequest request, HttpResponse response) {
		if (rh != null) {
			HttpVerb method = request.getMethod();
			switch (method) {
			case GET:
				rh.get(request, response);
				break;
			case POST:
				rh.post(request, response);
				break;
			case HEAD:
				rh.head(request, response);
				break;
			case PUT:
				rh.put(request, response);
				break;
			case DELETE:
				rh.delete(request, response);
				break;
			case OPTIONS: //Fall through
			case TRACE:
			case CONNECT:
			default:
				logger.warn("Unimplemented Http metod received: {}", method);
				//TODO send "not supported page (501) back to client"
			}
		}
	}
}
