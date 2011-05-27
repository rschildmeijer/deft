package org.deftserver.web.http.client;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeoutException;

import org.deftserver.io.AsynchronousSocket;
import org.deftserver.io.IOLoop;
import org.deftserver.io.timeout.Timeout;
import org.deftserver.util.NopAsyncResult;
import org.deftserver.util.UrlUtil;
import org.deftserver.web.AsyncCallback;
import org.deftserver.web.AsyncResult;
import org.deftserver.web.HttpVerb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* This class implements a simple HTTP 1.1 client on top of Deft's {@code AsynchronousSocket}.
* It does not currently implement all applicable parts of the HTTP
* specification.
* <pre>
* E.g the following is not supported.
*  - POST and PUT
*  
* </pre>
* This class has not been tested extensively in production and
* should be considered experimental as of the release of
* Deft 0.3.
* 
* This http client is inspired by https://github.com/facebook/tornado/blob/master/tornado/simple_httpclient.py
* and part of the documentation is simply copy pasted.
*/
public class AsynchronousHttpClient {

	private static final Logger logger = LoggerFactory.getLogger(AsynchronousHttpClient.class);

	private static final long TIMEOUT = 15 * 1000;	// 15s
	
	private static final AsyncResult<Response> nopAsyncResult = NopAsyncResult.of(Response.class).nopAsyncResult;

	private AsynchronousSocket socket;
	
	private Request request;
	private long requestStarted;
	private Response response;
	private AsyncResult<Response> responseCallback;
	
	private Timeout timeout;
	
	private final IOLoop ioLoop;
	
	private static final String HTTP_VERSION = "HTTP/1.1\r\n";
	private static final String USER_AGENT_HEADER = "User-Agent: Deft AsynchronousHttpClient/0.2-SNAPSHOT\r\n";
	private static final String NEWLINE = "\r\n";
	
	public AsynchronousHttpClient() { 
		this(IOLoop.INSTANCE);
	}
	
	public AsynchronousHttpClient(IOLoop ioLoop) {
		this.ioLoop = ioLoop;
	}

	/**
	 * Makes an asynchronous HTTP GET request against the specified url and invokes the given 
	 * callback when the response is fetched.
	 * 
	 * @param url e.g "http://tt.se:80/start/"
	 * @param cb callback that will be executed when the response is received.
	 */
	public void fetch(String url, AsyncResult<Response> cb) {
		request = new Request(url, HttpVerb.GET);
		doFetch(cb, System.currentTimeMillis());
	}
	
	public void fetch(Request request, AsyncResult<Response> cb) {
		this.request = request;
		doFetch(cb, System.currentTimeMillis());
	}
	
	private void doFetch(AsyncResult<Response> cb, long requestStarted) {
		this.requestStarted = requestStarted;
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
	
	/**
	 * Close the underlaying {@code AsynchronousSocket}.
	 */
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
		ioLoop.addTimeout(timeout);		
	}
	
	private void cancelTimeout() {
		logger.debug("cancel timeout...");
		timeout.cancel();
		timeout = null;
	}
	
	private void onTimeout() {
		logger.debug("Pending operation (connect, read or write) timed out...");
		AsyncResult<Response> cb = responseCallback;
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
		AsyncResult<Response> cb = responseCallback;
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
		response = new Response(requestStarted);
		String[] headers = result.split("\r\n");
		response.setStatuLine(headers[0]);	// first entry contains status line (e.g. HTTP/1.1 200 OK)
		for (int i = 1; i < headers.length; i++) {
			String[] header = headers[i].split(": ");
			response.setHeader(header[0], header[1]);
		}
		
		String contentLength = response.getHeader("Content-Length");
		startTimeout();
		if (contentLength != null) {
			socket.readBytes(
					Integer.parseInt(contentLength), 
					new NaiveAsyncResult() { public void onSuccess(String body) { onBody(body); } }
			);
		} else {  // Transfer-Encoding: chunked
			socket.readUntil(
					NEWLINE, 	/* chunk delimiter*/
					new NaiveAsyncResult() { public void onSuccess(String octet) { onChunkOctet(octet); } }
			);
		}
	}
				
	private void onBody(String body) {
		logger.debug("body size: {}", body.length());
		cancelTimeout();
		response.setBody(body);
		if ((response.getStatusLine().contains("301") || response.getStatusLine().contains("302")) && 
			request.isFollowingRedirects() && 
			request.getMaxRedirects() > 0) {
				String newUrl = UrlUtil.urlJoin(request.getURL(), response.getHeader("Location"));
				request = new Request(newUrl, HttpVerb.valueOf(request.getVerb()), true, request.getMaxRedirects() - 1);
				logger.debug("Following redirect, new url: {}, redirects left: {}", newUrl, request.getMaxRedirects());
				doFetch(responseCallback, requestStarted);
		} else {
			close();
			invokeResponseCallback();
		}
	} 
	
	private void onChunk(String chunk) {
		logger.debug("chunk size: {}", chunk.length());
		cancelTimeout();
		response.addChunk(chunk.substring(0, chunk.length() - NEWLINE.length()));
		startTimeout();
		socket.readUntil(
				NEWLINE, 	/* chunk delimiter*/
				new NaiveAsyncResult() { public void onSuccess(String octet) { onChunkOctet(octet); } }
		);
	}
	
	private void onChunkOctet(String octet) {
		int readBytes = Integer.parseInt(octet, 16);
		logger.debug("chunk octet: {} (decimal: {})", octet, readBytes);
		cancelTimeout();
		startTimeout();
		if (readBytes != 0) {
			socket.readBytes(
					readBytes + NEWLINE.length(),	// chunk delimiter is \r\n
					new NaiveAsyncResult() { public void onSuccess(String chunk) { onChunk(chunk); } }
			);
		} else {
			onBody(response.getBody());
		}
	}
	
	private void invokeResponseCallback() {
		AsyncResult<Response> cb = responseCallback;
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
