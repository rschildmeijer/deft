package org.deftserver.web;

public class HttpException extends RuntimeException {

	private final int statusCode;
	private String body;

	public HttpException(int statusCode) {
		this.statusCode = statusCode;
	}

	public HttpException(int statusCode, String body) {
		this(statusCode);
		this.body = body;
	}
	
	public int getStatusCode() {
		return statusCode;
	}
	
	public String getBody() {
		return body;
	}

}
