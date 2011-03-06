package org.deftserver.web.http.client;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeoutException;

import org.deftserver.io.AsynchronousSocket;
import org.deftserver.io.IOLoop;
import org.deftserver.io.timeout.Timeout;
import org.deftserver.util.NopAsyncResult;
import org.deftserver.web.AsyncCallback;
import org.deftserver.web.AsyncResult;
import org.deftserver.web.HttpVerb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsynchronousHttpClient {

	private static final Logger logger = LoggerFactory.getLogger(AsynchronousHttpClient.class);

	private static final long TIMEOUT = 15 * 1000;	// 15s
	
	private static final AsyncResult<HttpResponse> nopAsyncResult = NopAsyncResult.of(HttpResponse.class).nopAsyncResult;

	private AsynchronousSocket socket;
	
	private HttpRequest request;
	private long requestStarted;
	private HttpResponse response;
	private AsyncResult<HttpResponse> responseCallback;
	
	private Timeout timeout;
	
	private static final String HTTP_VERSION = "HTTP/1.1\r\n";
	private static final String USER_AGENT_HEADER = "User-Agent: Deft AsynchronousHttpClient/0.1\r\n";
	private static final String NEWLINE = "\r\n";
	
	public AsynchronousHttpClient() { }

	/**
	 * Makes an asynchronous HTTP GET request against the specified url and invokes the given 
	 * callback when the response is fetched.
	 * 
	 * @param url e.g "http://tt.se:80/start/"
	 * @param cb callback that will be executed when the response is received.
	 */
	public void fetch(String url, AsyncResult<HttpResponse> cb) {
		request = new HttpRequest(url, HttpVerb.GET);
		doFetch(cb);
	}
	
	public void fetch(HttpRequest request, AsyncResult<HttpResponse> cb) {
		this.request = request;
		doFetch(cb);
	}
	
	private void doFetch(AsyncResult<HttpResponse> cb) {
		requestStarted = System.currentTimeMillis();
		try {
			socket = new AsynchronousSocket(SocketChannel.open().configureBlocking(false));
		} catch (IOException e) {
			logger.error("Error opening SocketChannel: {}" + e.getMessage());
		}
		responseCallback = cb;
		int port = request.getURL().getPort();
		port = port == -1 ? 80 : port;
		startTimeout();
		socket.connect(
				request.getURL().getHost(), 
				port,
				new AsyncResult<Boolean>() {
					public void onFailure(Throwable t) { onConnectFailure(t); }
					public void onSuccess(Boolean result) { onConnect(); }
				}
		);
	}
	
	public void close() {
		logger.debug("Closing http client connection...");
		socket.close(); 
	}
	
	private void startTimeout() {
		logger.debug("start timeout...");
		timeout = new Timeout(
				System.currentTimeMillis() + TIMEOUT, 
				new AsyncCallback() { public void onCallback() { onTimeout(); } }
		);
		IOLoop.INSTANCE.addTimeout(timeout);		
	}
	
	private void cancelTimeout() {
		logger.debug("cancel timeout...");
		timeout.cancel();
		timeout = null;
	}
	
	private void onTimeout() {
		logger.debug("Pending operation (connect, read or write) timed out...");
		AsyncResult<HttpResponse> cb = responseCallback;
		responseCallback = nopAsyncResult;
		cb.onFailure(new TimeoutException("Connection timed out"));
		close();
	}

	private void onConnect() {
		logger.debug("Connected...");
		cancelTimeout();
		startTimeout();
		socket.write(
				makeRequestLineAndHeaders(), 
				new AsyncCallback() { public void onCallback() { onWriteComplete(); }}
		);
	}
	
	private void onConnectFailure(Throwable t) {
		logger.debug("Connect failed...");
		cancelTimeout();
		AsyncResult<HttpResponse> cb = responseCallback;
		responseCallback = nopAsyncResult;
		cb.onFailure(t);
		close();
	}

	/**
	 * 
	 * @return Eg. 
	 * 				GET /path/to/file/index.html HTTP/1.0
	 * 				From: a@b.com
	 * 				User-Agent: HTTPTool/1.0
	 * 
	 */
	private String makeRequestLineAndHeaders() {
		return request.getVerb() + " " + request.getURL().getPath() + " " + HTTP_VERSION +
				"From: a@b.com\r\n" +
				"Host: " + request.getURL().getHost() + "\r\n" +
				USER_AGENT_HEADER +
				NEWLINE;
	}
	
	private void onWriteComplete() {
		logger.debug("onWriteComplete...");
		cancelTimeout();
		startTimeout();
		socket.readUntil(
				"\r\n\r\n", 	/* header delimiter */
				new NaiveAsyncResult() { public void onSuccess(String headers) { onHeaders(headers); }
		});
	}
	
	private void onHeaders(String result) {
		logger.debug("headers: {}", result);
		cancelTimeout();
		response = new HttpResponse(requestStarted);
		String[] headers = result.split("\r\n");
		response.setStatuLine(headers[0]);	// first entry contains status line (e.g. HTTP/1.1 200 OK)
		for (int i = 1; i < headers.length; i++) {
			String[] header = headers[i].split(": ");
			response.setHeader(header[0], header[1]);
		}
		int readBytes = Integer.parseInt(response.getHeader("Content-Length"));
		startTimeout();
		socket.readBytes(
				readBytes, 
				new NaiveAsyncResult() { public void onSuccess(String result) { onBody(result); } }
		);
	}
				
	private void onBody(String body) {
		logger.debug("body size: {}", body.length());
		cancelTimeout();
		response.setBody(body);
		close();
		invokeResponseCallback();
	} 
	
	private void invokeResponseCallback() {
		AsyncResult<HttpResponse> cb = responseCallback;
		responseCallback = nopAsyncResult;
		cb.onSuccess(response);
	}
				
	/**
	 * Naive because all it does when an exception is thrown is log the exception.
	 */
	private abstract class NaiveAsyncResult implements AsyncResult<String> {
		
		@Override
		public void onFailure(Throwable caught) {
			logger.debug("onFailure: {}", caught);
		}
		
	}

}
