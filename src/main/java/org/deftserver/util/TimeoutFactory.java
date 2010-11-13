package org.deftserver.util;

import java.nio.channels.SocketChannel;

import org.deftserver.io.timeout.Timeout;
import org.deftserver.web.AsyncCallback;

public class TimeoutFactory {

	public static Timeout keepAliveTimeout(final SocketChannel clientChannel, long keepAliveTimeout) {
		return new Timeout(
				System.currentTimeMillis() + keepAliveTimeout,
				new AsyncCallback() {
					
					@Override public void onCallback() { Closeables.closeQuietly(clientChannel); }
				
				}
		);
	}
	
}
