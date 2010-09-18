/**
 * 
 */
package org.ulme.example;

import org.ulme.web.HttpContext;
import org.ulme.web.RequestHandler;

public class ExampleRequestHandler implements RequestHandler {

	@Override
	public void get(HttpContext ctx) {
		// ctx.getHttpResponse.write("hello world");
	}

}
