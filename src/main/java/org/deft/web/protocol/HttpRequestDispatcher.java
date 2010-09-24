package org.deft.web.protocol;

import org.deft.web.HttpVerb;
import org.deft.web.handler.RequestHandler;

public class HttpRequestDispatcher {

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
			}
		}
	}
}
