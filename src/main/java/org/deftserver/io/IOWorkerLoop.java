package org.deftserver.io;


import java.nio.channels.SocketChannel;

public interface IOWorkerLoop extends Runnable{
	
	
	/**
	 * Adds an accepted channel to the worker
	 * @param key
	 */
	void addChannel(SocketChannel clientChannel);
	
	/**
	 * Commits all added channels so the worker selector 
	 * will wake up and register the new channels
	 */
	void commitAddedChannels();
	
}
