package org.deftserver.util;

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Test;

public class UrlUtilTest {

	@Test
	public void urlJoinTest() throws MalformedURLException {
		assertEquals("http://tt.se/start/", UrlUtil.urlJoin(new URL("http://tt.se/"), "/start/"));
		assertEquals("http://localhost.com/", UrlUtil.urlJoin(new URL("http://localhost.com/moved_perm"), "/"));
		assertEquals("https://github.com/", UrlUtil.urlJoin(new URL("http://github.com/"), "https://github.com/"));
	}
	
}
