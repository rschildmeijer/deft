package org.deftserver.util;

import java.io.IOException;
import java.nio.channels.SelectableChannel;

import org.deftserver.io.IOLoop;
import org.deftserver.io.IOLoopFactory;

public class Closeables {

	private Closeables() {}

	public static void closeQuietly(SelectableChannel channel) {
		try {
			IOLoopFactory.getLoopController().removeHandler(channel);
			com.google.common.io.Closeables.close(channel, true);
		} catch (IOException ignore) { }
	}

}
