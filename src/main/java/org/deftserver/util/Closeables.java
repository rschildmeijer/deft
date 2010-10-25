package org.deftserver.util;

import java.io.IOException;
import java.nio.channels.SelectableChannel;

import org.deftserver.web.IOLoop;

public class Closeables {

	private Closeables() {}

	public static void closeQuietly(SelectableChannel channel) {
		try {
			IOLoop.INSTANCE.removeHandler(channel);
			com.google.common.io.Closeables.close(channel, true);
		} catch (IOException ignore) { }
	}

}
