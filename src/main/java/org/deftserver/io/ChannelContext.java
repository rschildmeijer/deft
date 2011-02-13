package org.deftserver.io;

import java.nio.ByteBuffer;

import org.deftserver.io.buffer.DynamicByteBuffer;

public interface ChannelContext<T> {
	
	
	ByteBuffer getBufferIn();
	
	ByteBuffer getBufferOut();
	
	T getContext();

}
