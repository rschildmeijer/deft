package org.deftserver.web.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.deftserver.util.ArrayUtil;
import org.deftserver.util.HttpRequestHelper;
import org.deftserver.util.HttpUtil;
import org.deftserver.web.http.HttpRequest;
import org.junit.Test;


public class HttpRequestParserTest {
	private static final HttpRequestParser parser = new HttpRequestParser();

	@Test 
	public void testDeserializeHttpGetRequest() {
		HttpRequestHelper helper = new HttpRequestHelper();
		helper.addHeader("Host", "127.0.0.1:8080");
		helper.addHeader("User-Agent", "curl/7.19.5 (i386-apple-darwin10.0.0) libcurl/7.19.5 zlib/1.2.3");
		helper.addHeader("Accept", "*/*");
		ByteBuffer bb1 = helper.getRequestAsByteBuffer(); 

		helper = new HttpRequestHelper();
		helper.addHeader("Host", "127.0.0.1:8080");
		helper.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.6; sv-SE; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
		helper.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		helper.addHeader("Accept-Language", "sv-se,sv;q=0.8,en-us;q=0.5,en;q=0.3");
		helper.addHeader("Accept-Encoding", "gzip,deflate");
		helper.addHeader("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
		helper.addHeader("Keep-Alive", "115");
		helper.addHeader("Connection", "keep-alve");
		ByteBuffer bb2 = helper.getRequestAsByteBuffer();

		HttpRequest request1 = parser.parseRequestBuffer(bb1);
		HttpRequest request2 = parser.parseRequestBuffer(bb2);

		assertEquals("GET / HTTP/1.1", request1.getRequestLine());
		assertEquals("GET / HTTP/1.1", request2.getRequestLine());

		assertEquals(4, request1.getHeaders().size());
		assertEquals(9, request2.getHeaders().size());

		List<String> expectedHeaderNamesInRequest1 = Arrays.asList(new String[]{"User-Agent", "Host", "Accept", "From"});
		for (String expectedHeaderName : expectedHeaderNamesInRequest1) {
			assertTrue(request1.getHeaders().containsKey(expectedHeaderName.toLowerCase()));
		}

		List<String> expectedHeaderNamesInRequest2 = Arrays.asList(new String[]{"Host", "User-Agent", "Accept", "From",
				"Accept-Language", "Accept-Encoding", "Accept-Charset", "Keep-Alive", "Connection"});
		for (String expectedHeaderName : expectedHeaderNamesInRequest2) {
			assertTrue(request2.getHeaders().containsKey(expectedHeaderName.toLowerCase()));
		}

		// TODO RS 100920 verify that the headers exist
	}
	
	/**
	 * Test multi-line header support
	 */
	@Test
	public void testHeadersSupport(){
		HttpRequestHelper helper = new HttpRequestHelper();
		helper.addHeader("Host", "127.0.0.1:8080");
		helper.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.6; sv-SE; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
		helper.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		helper.addHeader("Accept-Language", "sv-se,sv;q=0.8,en-us;q=0.5,en;q=0.3");
		helper.addHeader("Accept-Encoding", "gzip,deflate");
		helper.addHeader("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
		helper.addHeader("Cookie", "coooooooockie1\r\n ;coucou");
		helper.addHeader("Keep-Alive", "115");
		helper.addHeader("Connection", "keep-alve");
		ByteBuffer bb2 = helper.getRequestAsByteBuffer();

		HttpRequest request2 = parser.parseRequestBuffer(bb2);
		assertEquals("coooooooockie1;coucou", request2.getHeader("cookie"));
	}
	
	/**
	 * Test multi-line header support
	 */
	@Test
	public void testPostBodySupport(){
		HttpRequestHelper helper = new HttpRequestHelper();
		helper.addHeader("Host", "127.0.0.1:8080");
		helper.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.6; sv-SE; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
		helper.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		helper.addHeader("Accept-Language", "sv-se,sv;q=0.8,en-us;q=0.5,en;q=0.3");
		helper.addHeader("Accept-Encoding", "gzip,deflate");
		helper.addHeader("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
		helper.addHeader("Cookie", "coooooooockie1\r\n ;coucou");
		helper.addHeader("X-Test", "Test1; Test2; Test3");
		helper.addHeader("X-Test", "Test4; Test5; Test6");
		helper.addHeader("Keep-Alive", "115");
		helper.addHeader("Connection", "keep-alve");
		helper.setBody("This is my test body");
		helper.setMethod("POST");
		ByteBuffer bb2 = helper.getRequestAsByteBuffer();

		HttpRequest request2 =parser.parseRequestBuffer(bb2);
		request2.getBodyBuffer().flip();
		assertEquals("coooooooockie1;coucou", request2.getHeader("cookie"));
		assertEquals("Test1; Test2; Test3;Test4; Test5; Test6", request2.getHeader("X-Test"));
		assertEquals("This is my test body", request2.getBody());
	}


	
	/**
	 * Test multi-line header support
	 */
	@Test
	public void testPartialRequestSupport(){
		String request1 = "POST /path/script.cgi HTTP/1.0\r\n"+
		"From: frog@jmarshall.com\r\n";
		String request2 = "User-Agent: HTTPTool/1.0\r\n"+
		"Content-Type: application/x-www-form-urlencoded\r\n"+
		"Content-Length: 32\r\n\r\n";
		String request3 = "home=Cosby&favorite+flavor=flies\r\n";
		

		HttpRequest request = parser.parseRequestBuffer(ByteBuffer.wrap(request1.getBytes()));
		request = parser.parseRequestBuffer(ByteBuffer.wrap( request2.getBytes()), request);
		request = parser.parseRequestBuffer(ByteBuffer.wrap( request3.getBytes()),request);
		request.getBodyBuffer().flip();
		
		assertTrue("Parsed request should be finished after all buffers has been processed",request.isFinished());
		assertEquals("frog@jmarshall.com", request.getHeader("from"));
		assertEquals("HTTPTool/1.0", request.getHeader("user-agent"));
		assertEquals("application/x-www-form-urlencoded", request.getHeader("content-type"));
		assertEquals("32", request.getHeader("content-length"));
		assertEquals("POST", request.getMethod().toString());
		assertEquals("home=Cosby&favorite+flavor=flies", request.getBody());
	}
	
	
	/**
	 * Test Splitted buffer request support
	 */
	@Test
	public void testPartialRequestSupportSplittedBody(){
		String request1 = "POST /path/script.cgi HTTP/1.0\r\n"+
		"From: frog@jmarshall.com\r\n";
		String request2 = "User-Agent: HTTPTool/1.0\r\n"+
		"Content-Type: application/x-www-form-urlencoded\r\n"+
		"Content-Length: 32\r\n\r\n";
		String request3 = "home=Cosby&favorite";
		String request4 = "+flavor=flies\r\n";
		

		HttpRequest request = parser.parseRequestBuffer(ByteBuffer.wrap(request1.getBytes()));
		request = parser.parseRequestBuffer(ByteBuffer.wrap( request2.getBytes()), request);
		request = parser.parseRequestBuffer(ByteBuffer.wrap( request3.getBytes()), request);
		request = parser.parseRequestBuffer(ByteBuffer.wrap( request4.getBytes()), request);
		request.getBodyBuffer().flip();
		
		assertTrue("Parsed request should be finished after all buffers has been processed",request.isFinished());
		assertEquals("frog@jmarshall.com", request.getHeader("from"));
		assertEquals("HTTPTool/1.0", request.getHeader("user-agent"));
		assertEquals("application/x-www-form-urlencoded", request.getHeader("content-type"));
		assertEquals("32", request.getHeader("content-length"));
		assertEquals("POST", request.getMethod().toString());
		assertEquals("home=Cosby&favorite+flavor=flies", request.getBody());
	}
	
	@Test
	public void testSingleGetParameter() {
		HttpRequestHelper helper = new HttpRequestHelper();
		helper.addGetParameter("firstname", "jim");

		HttpRequest request = parser.parseRequestBuffer(helper.getRequestAsByteBuffer());

		assertEquals(1, request.getParameters().size());
		assertEquals("jim", request.getParameter("firstname"));
	}

	@Test
	public void testMultipleGetParameter() {
		HttpRequestHelper helper = new HttpRequestHelper();
		helper.addGetParameter("firstname", "jim");
		helper.addGetParameter("lastname", "petersson");
		helper.addGetParameter("city", "stockholm");

		HttpRequest request = parser.parseRequestBuffer(helper.getRequestAsByteBuffer());
		Map<String, Collection<String>> params = request.getParameters();

		assertEquals(3, getSize(params));
		assertEquals("jim", request.getParameter("firstname"));
		assertEquals("petersson", request.getParameter("lastname"));
		assertEquals("stockholm", request.getParameter("city"));
	}

	private int getSize(Map<String, Collection<String>> mmap) {
		int size = 0;
		for (Collection<String> values : mmap.values()) {
			size += values.size();
		}
		return size;
	}


	@Test
	public void testSingleParameterWithoutValue() {
		HttpRequestHelper helper = new HttpRequestHelper();
		helper.addGetParameter("firstname", null);

		HttpRequest request = parser.parseRequestBuffer(helper.getRequestAsByteBuffer());
		Map<String, Collection<String>> params = request.getParameters();
		assertEquals(0, getSize(params));
		assertEquals(null, request.getParameter("firstname"));
	}

	@Test
	public void testMultipleParametersWithoutValue() {
		HttpRequestHelper helper = new HttpRequestHelper();
		helper.addGetParameter("firstname", null);
		helper.addGetParameter("lastName", "");

		HttpRequest request = parser.parseRequestBuffer(helper.getRequestAsByteBuffer());
		Map<String, Collection<String>> params = request.getParameters();

		assertEquals(0, getSize(params));
		assertEquals(null, request.getParameter("firstname"));
		assertEquals(null, request.getParameter("lastName"));
	}

	@Test
	public void testMultipleParametersWithAndWithoutValue() {
		HttpRequestHelper helper = new HttpRequestHelper();
		helper.addGetParameter("firstname", null);
		helper.addGetParameter("lastName", "petersson");
		helper.addGetParameter("city", "");
		helper.addGetParameter("phoneno", "12345");
		helper.addGetParameter("age", "30");

		HttpRequest request = parser.parseRequestBuffer(helper.getRequestAsByteBuffer());
		Map<String, Collection<String>> params = request.getParameters();

		assertEquals(3, getSize(params));
		assertEquals(null, request.getParameter("firstname"));
		assertEquals("petersson", request.getParameter("lastName"));
		assertEquals(null, request.getParameter("city"));
		assertEquals("12345", request.getParameter("phoneno"));
		assertEquals("30", request.getParameter("age"));
	}

	@Test
	public void testSingleGetParameterMultipleValues() {
		HttpRequestHelper helper = new HttpRequestHelper();
		helper.addGetParameter("letters", "x");
		helper.addGetParameter("letters", "y");
		helper.addGetParameter("letters", "z");

		HttpRequest request = parser.parseRequestBuffer(helper.getRequestAsByteBuffer());
		Map<String, Collection<String>> params = request.getParameters();

		assertEquals(3, getSize(params));
		Collection<String> values = params.get("letters");
		assertEquals(3, values.size());
		assertTrue(values.contains("x"));
		assertTrue(values.contains("y"));
		assertTrue(values.contains("z"));
	}

	@Test
	public void testMultipleGetParametersMultipleValues() {
		HttpRequestHelper helper = new HttpRequestHelper();
		helper.addGetParameter("letters", "x");
		helper.addGetParameter("letters", "y");
		helper.addGetParameter("letters", "z");
		helper.addGetParameter("numbers", "23");
		helper.addGetParameter("numbers", "54");
		helper.addGetParameter("country", "swe");

		HttpRequest request = parser.parseRequestBuffer(helper.getRequestAsByteBuffer());
		Map<String, Collection<String>> params = request.getParameters();

		assertEquals(6, getSize(params));
		Collection<String> letters = params.get("letters");
		Collection<String> numbers = params.get("numbers");
		Collection<String> country = params.get("country");

		assertEquals(3, letters.size());
		assertEquals(2, numbers.size());
		assertEquals(1, country.size());

		assertTrue(letters.contains("x"));
		assertTrue(letters.contains("y"));
		assertTrue(letters.contains("z"));

		assertTrue(numbers.contains("23"));
		assertTrue(numbers.contains("54"));

		assertTrue(country.contains("swe"));
	}

	@Test
	public void testSingleGetParameterMultipleValuesIncludingNull() {
		HttpRequestHelper helper = new HttpRequestHelper();
		helper.addGetParameter("letters", "x");
		helper.addGetParameter("letters", "y");
		helper.addGetParameter("letters", null);
		helper.addGetParameter("letters", "z");

		HttpRequest request = parser.parseRequestBuffer(helper.getRequestAsByteBuffer());
		Map<String, Collection<String>> params = request.getParameters();

		assertEquals(3, getSize(params));
		Collection<String> values = params.get("letters");
		assertEquals(3, values.size());
		assertTrue(values.contains("x"));
		assertTrue(values.contains("y"));
		assertTrue(values.contains("z"));
	}

	@Test
	public void testEmptyParameters() {
		HttpRequestHelper helper = new HttpRequestHelper();
		HttpRequest request = parser.parseRequestBuffer(helper.getRequestAsByteBuffer());
		Map<String, Collection<String>> params = request.getParameters();
		assertNotNull(params);
		assertEquals(0, getSize(params));
	}

	@Test(expected=UnsupportedOperationException.class)
	public void testImmutableParameters() {
		HttpRequestHelper helper = new HttpRequestHelper();
		helper.addGetParameter("letter", "x");

		HttpRequest request = parser.parseRequestBuffer(helper.getRequestAsByteBuffer());
		Map<String, Collection<String>> params = request.getParameters();
		params.put("not", new ArrayList<String>());	
	}

	@Test
	public void testHostVerification_exists_HTTP_1_0() {
		HttpRequestHelper helper = new HttpRequestHelper();
		helper.setVersion("1.0");
		HttpRequest request = parser.parseRequestBuffer(helper.getRequestAsByteBuffer());
		boolean requestOk = HttpUtil.verifyRequest(request);
		assertTrue(requestOk);
	}

	@Test
	public void testHostVerification_nonExisting_HTTP_1_0() {
		HttpRequestHelper helper = new HttpRequestHelper();
		helper.setVersion("1.0");
		helper.removeHeader("Host");
		HttpRequest request = parser.parseRequestBuffer(helper.getRequestAsByteBuffer());
		boolean requestOk = HttpUtil.verifyRequest(request);
		assertTrue(requestOk);
	}

	@Test
	public void testHostVerification_exists_HTTP_1_1() {
		HttpRequestHelper helper = new HttpRequestHelper();
		HttpRequest request = parser.parseRequestBuffer(helper.getRequestAsByteBuffer());
		boolean requestOk = HttpUtil.verifyRequest(request);
		assertTrue(requestOk);
	}

	@Test
	public void testHostVerification_nonExisting_HTTP_1_1() {
		HttpRequestHelper helper = new HttpRequestHelper();
		helper.removeHeader("Host");
		HttpRequest request = parser.parseRequestBuffer(helper.getRequestAsByteBuffer());
		boolean requestOk = HttpUtil.verifyRequest(request);
		assertFalse(requestOk);
	}

	@Test
	public void testGarbageRequest() {
		HttpRequest.of(ByteBuffer.wrap(
				new byte[] {1, 1, 1, 1}	// garbage
		));
	}
	
	/**
	 * Ensure that header keys are converted to lower case, to facilitate
	 * case-insensitive retrieval through {@link HttpRequest#getHeader(String)}.
	 */
	@Test
	public void testOfConvertsHeaderKeysToLowerCase() {

		HttpRequestHelper helper = new HttpRequestHelper();
		helper.addHeader("TESTKEY", "unimportant");
		HttpRequest request = parser.parseRequestBuffer(helper.getRequestAsByteBuffer());

		assertFalse(request.getHeaders().containsKey("TESTKEY"));
		assertTrue(request.getHeaders().containsKey("testkey"));
	}

	/**
	 * Ensure that the case of any header values is correctly maintained.
	 */
	@Test
	public void testOfMaintainsHeaderValueCase() {

		String expected = "vAlUe";

		HttpRequestHelper helper = new HttpRequestHelper();
		helper.addHeader("TESTKEY", expected);
		HttpRequest request = parser.parseRequestBuffer(helper.getRequestAsByteBuffer());

		String actual = request.getHeader("TESTKEY");
		assertEquals(expected, actual);
	}

	/**
	 * Ensure that case for any key passed to the method is unimportant
	 * for its retrieval.
	 */
	@Test
	public void testGetHeader() {

		String expected = "value";

		HttpRequestHelper helper = new HttpRequestHelper();
		helper.addHeader("TESTKEY", expected);
		HttpRequest request = parser.parseRequestBuffer(helper.getRequestAsByteBuffer());

		assertEquals(expected, request.getHeader("TESTKEY"));
		assertEquals(expected, request.getHeader("testkey"));
	}

}
