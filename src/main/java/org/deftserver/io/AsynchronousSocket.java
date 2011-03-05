package org.deftserver.io;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.deftserver.util.Closeables;
import org.deftserver.util.NopAsyncResult;
import org.deftserver.web.AsyncCallback;
import org.deftserver.web.AsyncResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

public class AsynchronousSocket implements IOHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(AsynchronousSocket.class);
	
	private final int DEFAULT_BYTEBUFFER_SIZE = 1024;
	
	private final AsyncResult<String> nopAsyncResult = NopAsyncResult.of(String.class).nopAsyncResult;
	
	private final SelectableChannel channel;
	private int interestOps;
	
	private String readDelimiter = "";
	private int readBytes = Integer.MAX_VALUE;
	
	private AsyncCallback connectCallback = AsyncCallback.nopCb;
	private AsyncCallback closeCallback = AsyncCallback.nopCb;
	private AsyncResult<String> readCallback = nopAsyncResult;
	private AsyncCallback writeCallback = AsyncCallback.nopCb;
	
	private final StringBuilder readBuffer = new StringBuilder();
	private final StringBuilder writeBuffer = new StringBuilder();
	
	private boolean reachedEOF = false;
	
	public AsynchronousSocket(SelectableChannel channel) {
		this.channel = channel;
		interestOps = SelectionKey.OP_CONNECT;
		if (channel instanceof SocketChannel && (((SocketChannel) channel).isConnected())) {
			interestOps |= SelectionKey.OP_READ;
		}
		IOLoop.INSTANCE.addHandler(channel, this, interestOps, null);
	}
	
	/**
	 * Connects to the given host port tuple and invokes the given callback when a successful connection is established.
	 */
	public void connect(String host, int port, AsyncCallback ccb) {
		IOLoop.INSTANCE.updateHandler(channel, interestOps |= SelectionKey.OP_CONNECT);
		connectCallback = ccb;
		if (channel instanceof SocketChannel) {
			try {
				((SocketChannel) channel).connect(new InetSocketAddress(host, port));
			} catch (IOException e) {
				logger.error("Failed to connect to: {}, message: {} ", host, e.getMessage());
			}
		}
	}
	
	/**
	 * Close the socket.
	 */
	public void close() {
		Closeables.closeQuietly(channel);
		invokeCloseCallback();
	}
	
	public void setCloseCallback(AsyncCallback ccb) {
		closeCallback = ccb;
	}
	
	/**
	 * Should only be invoked by the IOLoop
	 */
	@Override
	public void handleAccept(SelectionKey key) throws IOException {
		logger.debug("handle accept...");
	}

	/**
	 * Should only be invoked by the IOLoop
	 */
	@Override
	public void handleConnect(SelectionKey key) throws IOException {
		logger.debug("handle connect...");
		if (((SocketChannel) channel).isConnectionPending()) {
			((SocketChannel) channel).finishConnect();
			invokeConnectCallback();
			interestOps &= ~SelectionKey.OP_CONNECT;
			IOLoop.INSTANCE.updateHandler(channel, interestOps |= SelectionKey.OP_READ);
		}
	}
	
	/**
	 * Should only be invoked by the IOLoop
	 */
	@Override
	public void handleRead(SelectionKey key) throws IOException {
		logger.debug("handle read...");
		ByteBuffer buffer = ByteBuffer.allocate(DEFAULT_BYTEBUFFER_SIZE);
		int read = ((SocketChannel) key.channel()).read(buffer);
		if (read == -1) {	// EOF
			reachedEOF = true;
			IOLoop.INSTANCE.updateHandler(channel, interestOps &= ~SelectionKey.OP_READ);
			return;
		}
		readBuffer.append(new String(buffer.array(), 0, buffer.position(), Charsets.ISO_8859_1));
		logger.debug("readBuffer size: {}", readBuffer.length());
		checkReadState();
	}

	/**
	 * Should only be invoked by the IOLoop
	 */
	@Override
	public void handleWrite(SelectionKey key) {
		logger.debug("handle write...");
		doWrite();
	}

	/**
	 * Reads from the underlaying SelectableChannel until delimiter is reached. When it its, the given
	 * AsyncResult will be invoked.
	 */
	public void readUntil(String delimiter, AsyncResult<String> rcb) {
		logger.debug("readUntil delimiter: {}", delimiter);
		readDelimiter = delimiter;
		readCallback = rcb;
		checkReadState();
	}

	/**
	 * Reads from the underlaying SelectableChannel until n bytes are read. When it its, the given
	 * AsyncResult will be invoked.
	 */
	public void readBytes(int n, AsyncResult<String> rcb) {
		logger.debug("readBytes #bytes: {}", n);
		readBytes = n;
		readCallback = rcb;
		checkReadState();
	}
	
	/**
	 *  If readBuffer contains readDelimiter, client read is finished => invoke readCallback (onSuccess)
	 *  Or if readBytes bytes are read, client read is finished => invoke readCallback (onSuccess)
	 *  Of if end-of-stream is reached => invoke readCallback (onFailure)
	 */
	private void checkReadState() {
		if (reachedEOF) {
			invokeReadFailureCallback(new EOFException("Reached end-of-stream"));
			return;
		}
		int index = readBuffer.indexOf(readDelimiter);
		if (index != -1 && !readDelimiter.isEmpty()) {
			String result = readBuffer.substring(0, index /*+ readDelimiter.length()*/);
			readBuffer.delete(0, index + readDelimiter.length());
			logger.debug("readBuffer size: {}", readBuffer.length());
			readDelimiter = "";
			invokeReadSuccessfulCallback(result);
		} else if (readBuffer.length() >= readBytes) {
			String result = readBuffer.substring(0, readBytes);
			readBuffer.delete(0, readBytes);
			logger.debug("readBuffer size: {}", readBuffer.length());
			readBytes = Integer.MAX_VALUE;
			invokeReadSuccessfulCallback(result);
		}
	}

	private void invokeReadSuccessfulCallback(String result) {
		AsyncResult<String> cb = readCallback;
		readCallback = nopAsyncResult;
		cb.onSuccess(result);
	}
	
	private void invokeReadFailureCallback(Exception e) {
		AsyncResult<String> cb = readCallback;
		readCallback = nopAsyncResult;
		cb.onFailure(e);
	}
	
	private void invokeWriteCallback() {
		AsyncCallback cb = writeCallback;
		writeCallback = AsyncCallback.nopCb;
		cb.onCallback();
	}
	
	private void invokeCloseCallback() {
		AsyncCallback cb = closeCallback;
		closeCallback = AsyncCallback.nopCb;
		cb.onCallback();
	}
	
	private void invokeConnectCallback() {
		AsyncCallback cb = connectCallback;
		connectCallback = AsyncCallback.nopCb;
		cb.onCallback();
	}

	/**
	 * Writes the given data to the underlaying SelectableChannel. When all data is successfully transmitted, the given 
	 * AsyncCallback will be invoked 
	 */
	public void write(String data, AsyncCallback wcb) {
		logger.debug("write data: {}", data);
		writeBuffer.append(data);
		logger.debug("writeBuffer size: {}", writeBuffer.length());
		writeCallback = wcb;
		doWrite();
	}
	
	/**
	 * If we succeed to write everything in writeBuffer, client write is finished => invoke writeCallback
	 */
	private void doWrite() {
		int written = 0;
		try {
			if (((SocketChannel)channel).isConnected()) {
				written = ((SocketChannel) channel).write(ByteBuffer.wrap(writeBuffer.toString().getBytes()));
			}
		} catch (IOException e) {
			logger.error("IOException during write: {}", e.getMessage());
			invokeCloseCallback();
			Closeables.closeQuietly(channel);
		}
		writeBuffer.delete(0, written);
		logger.debug("wrote: {} bytes", written);
		logger.debug("writeBuffer size: {}", writeBuffer.length());
		if (writeBuffer.length() > 0) {
			IOLoop.INSTANCE.updateHandler(channel, interestOps |= SelectionKey.OP_WRITE);
		} else {
			IOLoop.INSTANCE.updateHandler(channel, interestOps &= ~SelectionKey.OP_WRITE);
			invokeWriteCallback();
		}
	}

}
