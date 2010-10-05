package org.deftserver.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.deftserver.example.AsyncDbHandler;
import org.deftserver.web.handler.RequestHandler;
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
		
		final Application application = new Application(reqHandlers);

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
		HttpParams params = new BasicHttpParams();
		params.setParameter(" Connection", "Close");
		HttpClient httpclient = new DefaultHttpClient(params);
		HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/");
		HttpResponse response = httpclient.execute(httpget);
		List<String> expectedHeaders = Arrays.asList(new String[] {"Server", "Date", "Content-Length", "Etag"});

		assertEquals(200, response.getStatusLine().getStatusCode());
		assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
		assertEquals("OK", response.getStatusLine().getReasonPhrase());

		assertEquals(expectedHeaders.size(), response.getAllHeaders().length);

		for (String header : expectedHeaders) {
			assertTrue(response.getFirstHeader(header) != null);
		}
		
		assertEquals(expectedPayload, convertStreamToString(response.getEntity().getContent()).trim());
	}
	
	/**
	 * Test a RH that does a single write
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	@Test
	public void wTest() throws ClientProtocolException, IOException {
		HttpParams params = new BasicHttpParams();
		params.setParameter(" Connection", "Close");
		HttpClient httpclient = new DefaultHttpClient(params);
		HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/w");
		HttpResponse response = httpclient.execute(httpget);
		
		assertNotNull(response);
		assertEquals(200, response.getStatusLine().getStatusCode());
		assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
		assertEquals("OK", response.getStatusLine().getReasonPhrase());
		String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
		assertEquals("1", payLoad);
		assertEquals(4, response.getAllHeaders().length);
	}

	
	@Test
	public void wwTest() throws ClientProtocolException, IOException {
		HttpParams params = new BasicHttpParams();
		params.setParameter(" Connection", "Close");
		HttpClient httpclient = new DefaultHttpClient(params);
		HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/ww");
		HttpResponse response = httpclient.execute(httpget);
		
		assertNotNull(response);
		assertEquals(200, response.getStatusLine().getStatusCode());
		assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
		assertEquals("OK", response.getStatusLine().getReasonPhrase());
		String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
		assertEquals("12", payLoad);
		assertEquals(4, response.getAllHeaders().length);
	}

	@Test
	public void wwfwTest() throws ClientProtocolException, IOException {
		HttpParams params = new BasicHttpParams();
		params.setParameter(" Connection", "Close");
		HttpClient httpclient = new DefaultHttpClient(params);
		HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/wwfw");
		HttpResponse response = httpclient.execute(httpget);
		
		assertNotNull(response);
		assertEquals(200, response.getStatusLine().getStatusCode());
		assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
		assertEquals("OK", response.getStatusLine().getReasonPhrase());
		String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
		assertEquals("123", payLoad);
		assertEquals(2, response.getAllHeaders().length);
	}
	
	@Test
	public void wfwfTest() throws ClientProtocolException, IOException {
		HttpParams params = new BasicHttpParams();
		params.setParameter(" Connection", "Close");
		HttpClient httpclient = new DefaultHttpClient(params);
		HttpGet httpget = new HttpGet("http://localhost:" + PORT + "/wfwf");
		HttpResponse response = httpclient.execute(httpget);
		
		assertNotNull(response);
		assertEquals(200, response.getStatusLine().getStatusCode());
		assertEquals(new ProtocolVersion("HTTP", 1, 1), response.getStatusLine().getProtocolVersion());
		assertEquals("OK", response.getStatusLine().getReasonPhrase());
		String payLoad = convertStreamToString(response.getEntity().getContent()).trim();
		assertEquals("12", payLoad);
		assertEquals(2, response.getAllHeaders().length);
	}

	@Test
	public void deleteTest() throws ClientProtocolException, IOException {
		HttpParams params = new BasicHttpParams();
		params.setParameter(" Connection", "Close");
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
		HttpParams params = new BasicHttpParams();
		params.setParameter(" Connection", "Close");
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
		HttpParams params = new BasicHttpParams();
		params.setParameter(" Connection", "Close");
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
		HttpParams params = new BasicHttpParams();
		params.setParameter(" Connection", "Close");
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
		HttpParams params = new BasicHttpParams();
		params.setParameter(" Connection", "Close");
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
