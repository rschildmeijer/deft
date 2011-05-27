package org.deftserver.util;

import java.io.IOException;
import java.nio.channels.SelectableChannel;

import org.deftserver.io.IOLoop;

public class Closeables {

	private Closeables() {}

	public static void closeQuietly(SelectableChannel channel) {
		closeQuietly(IOLoop.INSTANCE, channel);
	}
	
	public static void closeQuietly(IOLoop ioLoop, SelectableChannel channel) {
		try {
			ioLoop.removeHandler(channel);
			com.google.common.io.Closeables.close(channel, true);
		} catch (IOException ignore) { }
	}
	
}
