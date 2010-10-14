package org.deftserver.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.deftserver.example.AsyncDbHandler;
import org.deftserver.web.handler.RequestHandler;
import org.deftserver.web.protocol.HttpRequest;
import org.junit.BeforeClass;
import org.junit.Test;


public class DeftSystemTest {

	private static final int PORT = 8081;

	public static final String expectedPayload = "hello test";

	private static class ExampleRequestHandler extends RequestHandler {
		@Override
		public void get(org.deftserver.web.protocol.HttpRequest request, org.deftserver.web.protocol.HttpResponse response) {
			response.write(expectedPayload);
		}
	}

	private static class WRequestHandler extends RequestHandler {
		@Override
		public void get(org.deftserver.web.protocol.HttpRequest request, org.deftserver.web.protocol.HttpResponse response) {
			response.write("1");
		}
	}

	private static class WWRequestHandler extends RequestHandler {
		@Override
		public void get(org.deftserver.web.protocol.HttpRequest request, org.deftserver.web.protocol.HttpResponse response) {
			response.write("1");
			response.write("2");
		}
	}

	private static class WWFWRequestHandler extends RequestHandler {
		@Override
		public void get(org.deftserver.web.protocol.HttpRequest request, org.deftserver.web.protocol.HttpResponse response) {
			response.write("1");
			response.write("2");
			response.flush();
			response.write("3");
		}
	}

	private static class WFWFRequestHandler extends RequestHandler {
		@Override
		public void get(org.deftserver.web.protocol.HttpRequest request, org.deftserver.web.protocol.HttpResponse response) {
			response.write("1");
			response.flush();
			response.write("2");
			response.flush();
		}
	}

	private static class DeleteRequestHandler extends RequestHandler {
		@Override
		public void delete(org.deftserver.web.protocol.HttpRequest request, org.deftserver.web.protocol.HttpResponse response) {
			response.write("del");
			response.flush();
			response.write("ete");
			response.flush();
		}
	}

	private static class PostRequestHandler extends RequestHandler {
		@Override
		public void post(org.deftserver.web.protocol.HttpRequest request, org.deftserver.web.protocol.HttpResponse response) {
			response.write("po");
			response.flush();
			response.write("st");
			response.flush();
		}
	}

	private static class PutRequestHandler extends RequestHandler {
		@Override
		public void put(org.deftserver.web.protocol.HttpRequest request, org.deftserver.web.protocol.HttpResponse response) {
			response.write("p");
			response.flush();
			response.write("ut");
			response.flush();
		}
	}

	private static class CapturingRequestRequestHandler extends RequestHandler {
		@Override
		public void get(org.deftserver.web.protocol.HttpRequest request, org.deftserver.web.protocol.HttpResponse response) {
			response.write(request.getRequestedPath());
		}
	}

	private static class ThrowingHttpExceptionRequestHandler extends RequestHandler {
		@Override
		public void get(org.deftserver.web.protocol.HttpRequest request, org.deftserver.web.protocol.HttpResponse response) {
			throw new HttpException(500, "exception message");
		}
	}

	private static class AsyncThrowingHttpExceptionRequestHandler extends RequestHandler {
		@Asynchronous
		@Override
		public void get(org.deftserver.web.protocol.HttpRequest request, org.deftserver.web.protocol.HttpResponse response) {
			throw new HttpException(500, "exception message");
		}
	}

	public static class NoBodyRequestHandler extends RequestHandler {
		@Override
		public void get(HttpRequest request, org.deftserver.web.protocol.HttpResponse response) {
			response.setStatusCode(200);
		}
	}

	public static class MovedPermanentlyRequestHandler extends RequestHandler {
		@Override
		public void get(HttpRequest request, org.deftserver.web.protocol.HttpResponse response) {
			response.setStatusCode(301);
			response.setHeader("Location", "/");
		}
	}

	@BeforeClass
	public static void setup() {
		Map<String, RequestHandler> reqHandlers = new HashMap<String, RequestHandler>();
		reqHandlers.put("/", new ExampleRequestHandler());
		reqHandlers.put("/mySql", new AsyncDbHandler());
		reqHandlers.put("/w", new WRequestHandler());
		reqHandlers.put("/ww", new WWRequestHandler());
		reqHandlers.put("/wwfw", new WWFWRequestHandler());
		reqHandlers.put("/wfwf", new WFWFRequestHandler());
		reqHandlers.put("/delete", new DeleteRequestHandler());
		reqHandlers.put("/post", new PostRequestHandler());
		reqHandlers.put("/put", new PutRequestHandler());
		reqHandlers.put("/capturing/([0-9]+)", new CapturingRequestRequestHandler());
		reqHandlers.put("/throw", new ThrowingHttpExceptionRequestHandler());
		reqHandlers.put("/async_throw", new AsyncThrowingHttpExceptionRequestHandler());
		reqHandlers.put("/no_body", new NoBodyRequestHandler());
		reqHandlers.put("/moved_perm", new MovedPermanentlyRequestHandler());

		final Application application = new Application(reqHandlers);
		application.setStaticContentDir("src/test/resources");

		// start deft instance from a new thread because the start invocation is blocking 
		// (invoking thread will be I/O loop thread)
		new Thread(new Runnable() {
			@Override public void run() { new HttpServer(application).listen(PORT).getIOLoop().start(); }
		}).start();
	}

	@Test
	public void simpleGetRequestTest() throws ClientProtocolException, IOException {
		doSimpleGetRequest();
	}

	private void doSimpleGetRequest() throws ClientProtocolException, IOException {
		List<Header> headers = new LinkedList<Header>();
		headers.add(new BasicHeader("Connection", "Close"));
		HttpParams params = new BasicHttpParams();
		params.setParameter("http.default-headers", headers);

		HttpClient httpclient = new DefaultHttpClient(params);
		HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/");
		HttpResponse response = httpclient.execute(httpget);
		List<String> expectedHeaders = Arrays.asList(new String[] {"Server", "Date", "Content-Length", "Etag", "Connection"});

		assertEquals(200, response.getStatusLine().getStatusCode());
		assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
		assertEquals("OK", response.getStatusLine().getReasonPhrase());

		assertEquals(expectedHeaders.size(), response.getAllHeaders().length);

		for (String header : expectedHeaders) {
			assertTrue(response.getFirstHeader(header) != null);
		}

		assertEquals(expectedPayload, convertStreamToString(response.getEntity().getContent()).trim());
		assertEquals(expectedPayload.length()+"", response.getFirstHeader("Content-Length").getValue());
	}

	/**
	 * Test a RH that does a single write
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	@Test
	public void wTest() throws ClientProtocolException, IOException {
		List<Header> headers = new LinkedList<Header>();
		headers.add(new BasicHeader("Connection", "Close"));
		HttpParams params = new BasicHttpParams();
		params.setParameter("http.default-headers", headers);

		HttpClient httpclient = new DefaultHttpClient(params);
		HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/w");
		HttpResponse response = httpclient.execute(httpget);

		assertNotNull(response);
		assertEquals(200, response.getStatusLine().getStatusCode());
		assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
		assertEquals("OK", response.getStatusLine().getReasonPhrase());
		String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
		assertEquals("1", payLoad);
		assertEquals(5, response.getAllHeaders().length);
		assertEquals("1", response.getFirstHeader("Content-Length").getValue());
	}


	@Test
	public void wwTest() throws ClientProtocolException, IOException {
		List<Header> headers = new LinkedList<Header>();
		headers.add(new BasicHeader("Connection", "Close"));
		HttpParams params = new BasicHttpParams();
		params.setParameter("http.default-headers", headers);

		HttpClient httpclient = new DefaultHttpClient(params);
		HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/ww");
		HttpResponse response = httpclient.execute(httpget);

		assertNotNull(response);
		assertEquals(200, response.getStatusLine().getStatusCode());
		assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
		assertEquals("OK", response.getStatusLine().getReasonPhrase());
		String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
		assertEquals("12", payLoad);
		assertEquals(5, response.getAllHeaders().length);
		assertEquals("2", response.getFirstHeader("Content-Length").getValue());
	}

	@Test
	public void wwfwTest() throws ClientProtocolException, IOException {
		List<Header> headers = new LinkedList<Header>();
		headers.add(new BasicHeader("Connection", "Close"));
		HttpParams params = new BasicHttpParams();
		params.setParameter("http.default-headers", headers);

		HttpClient httpclient = new DefaultHttpClient(params);
		HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/wwfw");
		HttpResponse response = httpclient.execute(httpget);

		assertNotNull(response);
		assertEquals(200, response.getStatusLine().getStatusCode());
		assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
		assertEquals("OK", response.getStatusLine().getReasonPhrase());
		String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
		assertEquals("123", payLoad);
		assertEquals(3, response.getAllHeaders().length);
	}

	@Test
	public void wfwfTest() throws ClientProtocolException, IOException {
		List<Header> headers = new LinkedList<Header>();
		headers.add(new BasicHeader("Connection", "Close"));
		HttpParams params = new BasicHttpParams();
		params.setParameter("http.default-headers", headers);

		HttpClient httpclient = new DefaultHttpClient(params);
		HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/wfwf");
		HttpResponse response = httpclient.execute(httpget);

		assertNotNull(response);
		assertEquals(200, response.getStatusLine().getStatusCode());
		assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
		assertEquals("OK", response.getStatusLine().getReasonPhrase());
		String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
		assertEquals("12", payLoad);
		assertEquals(3, response.getAllHeaders().length);
	}

	@Test
	public void deleteTest() throws ClientProtocolException, IOException {
		List<Header> headers = new LinkedList<Header>();
		headers.add(new BasicHeader("Connection", "Close"));
		HttpParams params = new BasicHttpParams();
		params.setParameter("http.default-headers", headers);

		HttpClient httpclient = new DefaultHttpClient(params);
		HttpDelete httpdelete = new HttpDelete("http://localhost:" + PORT + "/delete");
		HttpResponse response = httpclient.execute(httpdelete);

		assertNotNull(response);
		assertEquals(200, response.getStatusLine().getStatusCode());
		assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
		assertEquals("OK", response.getStatusLine().getReasonPhrase());
		String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
		assertEquals("delete", payLoad);
	}

	@Test
	public void PostTest() throws ClientProtocolException, IOException {
		List<Header> headers = new LinkedList<Header>();
		headers.add(new BasicHeader("Connection", "Close"));
		HttpParams params = new BasicHttpParams();
		params.setParameter("http.default-headers", headers);

		HttpClient httpclient = new DefaultHttpClient(params);
		HttpPost httppost = new HttpPost("http://localhost:" + PORT + "/post");
		HttpResponse response = httpclient.execute(httppost);

		assertNotNull(response);
		assertEquals(200, response.getStatusLine().getStatusCode());
		assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
		assertEquals("OK", response.getStatusLine().getReasonPhrase());
		String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
		assertEquals("post", payLoad);
	}

	@Test
	public void putTest() throws ClientProtocolException, IOException {
		List<Header> headers = new LinkedList<Header>();
		headers.add(new BasicHeader("Connection", "Close"));
		HttpParams params = new BasicHttpParams();
		params.setParameter("http.default-headers", headers);

		HttpClient httpclient = new DefaultHttpClient(params);
		HttpPut httpput = new HttpPut("http://localhost:" + PORT + "/put");
		HttpResponse response = httpclient.execute(httpput);

		assertNotNull(response);
		assertEquals(200, response.getStatusLine().getStatusCode());
		assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
		assertEquals("OK", response.getStatusLine().getReasonPhrase());
		String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
		assertEquals("put", payLoad);
	}

	@Test
	public void capturingTest() throws ClientProtocolException, IOException {
		List<Header> headers = new LinkedList<Header>();
		headers.add(new BasicHeader("Connection", "Close"));
		HttpParams params = new BasicHttpParams();
		params.setParameter("http.default-headers", headers);

		HttpClient httpclient = new DefaultHttpClient(params);
		HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/capturing/1911");
		HttpResponse response = httpclient.execute(httpget);

		assertNotNull(response);
		assertEquals(200, response.getStatusLine().getStatusCode());
		assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
		assertEquals("OK", response.getStatusLine().getReasonPhrase());
		String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
		assertEquals("/capturing/1911", payLoad);
	}

	@Test
	public void erroneousCapturingTest() throws ClientProtocolException, IOException {
		List<Header> headers = new LinkedList<Header>();
		headers.add(new BasicHeader("Connection", "Close"));
		HttpParams params = new BasicHttpParams();
		params.setParameter("http.default-headers", headers);

		HttpClient httpclient = new DefaultHttpClient(params);
		HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/capturing/r1911");
		HttpResponse response = httpclient.execute(httpget);

		assertNotNull(response);
		assertEquals(404, response.getStatusLine().getStatusCode());
		assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
		assertEquals("Not Found", response.getStatusLine().getReasonPhrase());
		String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
		assertEquals("Requested URL: /capturing/r1911 was not found", payLoad);
	}

	@Test
	public void simpleConcurrentGetRequestTest() {
		int nThreads = 8;
		int nRequests = 2048;
		final CountDownLatch latch = new CountDownLatch(nRequests);
		ExecutorService executor = Executors.newFixedThreadPool(nThreads);

		for (int i = 1; i <= nRequests; i++) {
			executor.submit(new Runnable() {

				@Override
				public void run() {
					try {
						doSimpleGetRequest();
						latch.countDown();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			});
		}
		try {
			latch.await(15 * 1000, TimeUnit.MILLISECONDS);	// max wait time
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (latch.getCount() != 0) {
			assertTrue("Did not finish " + nRequests + " # of requests", false);
		}
	}

	@Test
	public void asynchronousRequestTest() throws ClientProtocolException, IllegalStateException, IOException {
		for (int i = 1; i <= 40; i++) {
			doAsynchronousRequestTest();
		}
	}

	private void doAsynchronousRequestTest() throws ClientProtocolException, IOException, IllegalStateException {
		List<Header> headers = new LinkedList<Header>();
		headers.add(new BasicHeader("Connection", "Close"));
		HttpParams params = new BasicHttpParams();
		params.setParameter("http.default-headers", headers);

		DefaultHttpClient httpclient = new DefaultHttpClient(params);
		HttpConnectionParams.setConnectionTimeout(params, 40 * 1000);
		HttpConnectionParams.setSoTimeout(params, 100 * 1000);

		HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/mySql");
		HttpResponse response = httpclient.execute(httpget);

		assertNotNull(response);
		assertEquals(200, response.getStatusLine().getStatusCode());
		assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
		assertEquals("OK", response.getStatusLine().getReasonPhrase());
		String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
		assertEquals("Name: Jim123", payLoad);
	}

	@Test
	public void keepAliveRequestTest() throws ClientProtocolException, IOException {
		List<Header> headers = new LinkedList<Header>();
		headers.add(new BasicHeader("Connection", "Keep-Alive"));
		HttpParams params = new BasicHttpParams();
		params.setParameter("http.default-headers", headers);

		DefaultHttpClient httpclient = new DefaultHttpClient(params);

		for (int i = 1; i <= 25; i++) {
			doKeepAliveRequestTest(httpclient);
		}
	}

	private void doKeepAliveRequestTest(DefaultHttpClient httpclient)
	throws IOException, ClientProtocolException {
		HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/");
		HttpResponse response = httpclient.execute(httpget);

		assertNotNull(response);
		assertEquals(200, response.getStatusLine().getStatusCode());
		assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
		assertEquals("OK", response.getStatusLine().getReasonPhrase());
		assertEquals(5, response.getAllHeaders().length);
		String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
		assertEquals(expectedPayload, payLoad);
	}

	@Test
	public void HTTP_1_0_noConnectionHeaderTest() throws ClientProtocolException, IOException {
		HttpParams params = new BasicHttpParams();
		HttpProtocolParams.setVersion(params, new ProtocolVersion("HTTP", 1, 0));
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/");
		HttpResponse response = httpclient.execute(httpget);

		assertNotNull(response);
		assertEquals(200, response.getStatusLine().getStatusCode());
		assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
		assertEquals("OK", response.getStatusLine().getReasonPhrase());
		assertEquals(5, response.getAllHeaders().length);
		String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
		assertEquals(expectedPayload, payLoad);
	}


	@Test
	public void httpExceptionTest() throws ClientProtocolException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/throw");
		HttpResponse response = httpclient.execute(httpget);

		assertNotNull(response);
		assertEquals(500, response.getStatusLine().getStatusCode());
		assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
		assertEquals("Internal Server Error", response.getStatusLine().getReasonPhrase());
		assertEquals(5, response.getAllHeaders().length);
		String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
		assertEquals("exception message", payLoad);
	}

	@Test
	public void asyncHttpExceptionTest() throws ClientProtocolException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/async_throw");
		HttpResponse response = httpclient.execute(httpget);

		assertNotNull(response);
		assertEquals(500, response.getStatusLine().getStatusCode());
		assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
		assertEquals("Internal Server Error", response.getStatusLine().getReasonPhrase());
		assertEquals(5, response.getAllHeaders().length);
		String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
		assertEquals("exception message", payLoad);
	}

	@Test
	public void staticFileRequestTest() throws ClientProtocolException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/src/test/resources/test.txt");
		HttpResponse response = httpclient.execute(httpget);

		assertNotNull(response);
		assertEquals(200, response.getStatusLine().getStatusCode());
		assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
		assertEquals("OK", response.getStatusLine().getReasonPhrase());
		assertEquals(7, response.getAllHeaders().length);
		String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
		assertEquals("test.txt", payLoad);
	}

	@Test
	public void pictureStaticFileRequestTest() throws ClientProtocolException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/src/test/resources/n792205362_2067.jpg");
		HttpResponse response = httpclient.execute(httpget);

		assertNotNull(response);
		assertEquals(200, response.getStatusLine().getStatusCode());
		assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
		assertEquals("OK", response.getStatusLine().getReasonPhrase());
		assertEquals(7, response.getAllHeaders().length);
		assertEquals("54963", response.getFirstHeader("Content-Length").getValue());
		assertEquals("image/jpeg", response.getFirstHeader("Content-Type").getValue());
		assertNotNull(response.getFirstHeader("Last-Modified"));
	}

	@Test
	public void noBodyRequest() throws ClientProtocolException, IOException {
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/no_body");
		HttpResponse response = httpclient.execute(httpget);
		List<String> expectedHeaders = Arrays.asList(new String[] {"Server", "Date", "Content-Length", "Connection"});

		assertEquals(200, response.getStatusLine().getStatusCode());
		assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
		assertEquals("OK", response.getStatusLine().getReasonPhrase());

		assertEquals(expectedHeaders.size(), response.getAllHeaders().length);

		for (String header : expectedHeaders) {
			assertTrue(response.getFirstHeader(header) != null);
		}

		assertEquals("", convertStreamToString(response.getEntity().getContent()).trim());
		assertEquals("0", response.getFirstHeader("Content-Length").getValue());
	}

	@Test
	public void movedPermanentlyRequest() throws ClientProtocolException, IOException {
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/moved_perm");
		HttpResponse response = httpclient.execute(httpget);
		List<String> expectedHeaders = Arrays.asList(new String[] {"Server", "Date", "Content-Length", "Connection", "Etag"});

		assertEquals(200, response.getStatusLine().getStatusCode());
		assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
		assertEquals("OK", response.getStatusLine().getReasonPhrase());

		assertEquals(expectedHeaders.size(), response.getAllHeaders().length);

		for (String header : expectedHeaders) {
			assertTrue(response.getFirstHeader(header) != null);
		}

		assertEquals(expectedPayload, convertStreamToString(response.getEntity().getContent()).trim());
		assertEquals(expectedPayload.length()+"", response.getFirstHeader("Content-Length").getValue());
	}

	@Test
	public void sendGarbageTest() throws IOException {
		InetSocketAddress socketAddress = new InetSocketAddress(PORT);
		SocketChannel channel = SocketChannel.open(socketAddress);
		channel.write(
				ByteBuffer.wrap(
						new byte[] {1, 1, 1, 1}	// garbage
				)
		);
		channel.close();
	}

	public String convertStreamToString(InputStream is) throws IOException {
		if (is != null) {
			StringBuilder sb = new StringBuilder();
			String line;

			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
				while ((line = reader.readLine()) != null) {
					sb.append(line).append("\n");
				}
			} finally {
				is.close();
			}
			return sb.toString();
		} else {       
			return "";
		}
	}

}
