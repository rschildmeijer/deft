package org.deftserver.io;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;


/**
 * {@link ChannelContext} stored as attachment of a SocketChannel.
 * It holds In and Out buffers, the {@link IOHandler} responsible for this channel
 * and a context object.  
 * 
 * 
 * @author slm
 *
 * @param <T>
 */
public interface ChannelContext<T> {
	
	/**
	 * Retrieves the {@link IOHandler} responsible for the {@link SocketChannel}
	 * holding this context
	 * @return
	 */
	IOHandler getHandler();
	
	/**
	 * Byte buffer used to read data from the holding {@link SocketChannel}
	 * @return
	 */
	ByteBuffer getBufferIn();
	
	/**
	 * Byte buffer used to read data from the holding {@link SocketChannel}
	 * @return
	 */
	void setBufferIn(ByteBuffer buffer);
	
	/**
	 * Byte buffer used to write data to the holding {@link SocketChannel}
	 * @return
	 */
	ByteBuffer getBufferOut();
	
	/**
	 * Context object stored with this channel
	 * @return
	 */
	T getContext();

}
