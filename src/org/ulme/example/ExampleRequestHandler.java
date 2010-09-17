/**
 * 
 */
package org.ulme.example;

import org.ulme.web.Context;
import org.ulme.web.RequestHandler;

public class ExampleRequestHandler implements RequestHandler {

	@Override
	public void get(Context ctx) {
		// ctx.getHttpResponse.write("hello world");
	}

}
