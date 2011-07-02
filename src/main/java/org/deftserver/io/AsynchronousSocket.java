package org.deftserver.io;

import java.io.EOFException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;

import org.deftserver.util.Closeables;
import org.deftserver.util.NopAsyncResult;
import org.deftserver.web.AsyncCallback;
import org.deftserver.web.AsyncResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

public class AsynchronousSocket implements IOHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(AsynchronousSocket.class);
	
	private final IOLoop ioLoop;
	
	private final int DEFAULT_BYTEBUFFER_SIZE = 1024;
	
	private final AsyncResult<String> nopAsyncStringResult = NopAsyncResult.of(String.class).nopAsyncResult;
	private final AsyncResult<Boolean> nopAsyncBooleanResult = NopAsyncResult.of(Boolean.class).nopAsyncResult;
	
	private final SelectableChannel channel;
	private int interestOps;
	
	private String readDelimiter = "";
	private int readBytes = Integer.MAX_VALUE;
	
	private AsyncResult<Boolean> connectCallback = nopAsyncBooleanResult;
	private AsyncCallback closeCallback = AsyncCallback.nopCb;
	private AsyncResult<String> readCallback = nopAsyncStringResult;
	private AsyncCallback writeCallback = AsyncCallback.nopCb;
	
	private final StringBuilder readBuffer = new StringBuilder();
	private final StringBuilder writeBuffer = new StringBuilder();
	
	private boolean reachedEOF = false;
	
	/**
	 * Creates a new {@code AsynchronousSocket} that will delegate its io operations to the given 
	 * {@link SelectableChannel}. 
	 * <p>
	 * Support for three non-blocking asynchronous methods that take callbacks:
	 * <p> 
	 * {@link #readUntil(String, AsyncResult)}
	 * <p>  
	 * {@link #readBytes(int, AsyncResult)} and
	 * <p>
	 * {@link #write(String, AsyncCallback)} 
	 * <p>
	 * The {@link SelectableChannel} should be the result of either {@link SocketChannel#open()} (client operations, 
	 * connected or  unconnected) or {@link ServerSocketChannel#accept()} (server operations).
	 * <p> 
	 * The given {@code SelectableChannel} will be configured to be in non-blocking mode, even if it is non-blocking
	 * already. 
	 * 
	 * <p>Below is an example of how a simple server could be implemented.
	 * <pre>
     *   final ServerSocketChannel server = ServerSocketChannel.open();
     *   server.socket().bind(new InetSocketAddress(9090));
     * 	
     *   AcceptUtil.accept(server, new AsyncCallback() { public void onCallback() { onAccept(server);} });
     *   IOLoop.INSTANCE.start();
	 * 
     *   private static void onAccept(ServerSocketChannel channel) {
     *       SocketChannel client = channel.accept();
     *       AsynchronousSocket socket = new AsynchronousSocket(client);
     *       // use socket
     *   }
	 * </pre>
	 */
	public AsynchronousSocket(SelectableChannel channel) {
		this(IOLoop.INSTANCE, channel);
	}
	
	public AsynchronousSocket(IOLoop ioLoop, SelectableChannel channel) {
		this.ioLoop = ioLoop;
		this.channel = channel;
		interestOps = SelectionKey.OP_CONNECT;	// TODO RS110628 should probably be moved to connect(..)
		try {
			channel.configureBlocking(false);
		} catch (IOException e) {
			logger.error("Could not configure SocketChannel to be non-blocking");
		}
		if (channel instanceof SocketChannel && (((SocketChannel) channel).isConnected())) {
			interestOps |= SelectionKey.OP_READ;
		}
		ioLoop.addHandler(channel, this, interestOps, null);
	}
	
	/**
	 * Connects to the given host port tuple and invokes the given callback when a successful connection is established.
	 * <p>
	 * You can both read and write on the {@code AsynchronousSocket} before it is connected
	 * (in which case the data will be written/read as soon as the connection is ready).
	 */
	public void connect(String host, int port, AsyncResult<Boolean> ccb) {
		ioLoop.updateHandler(channel, interestOps |= SelectionKey.OP_CONNECT);
		connectCallback = ccb;
		if (channel instanceof SocketChannel) {
			try {
				((SocketChannel) channel).connect(new InetSocketAddress(host, port));
			} catch (IOException e) {
				logger.error("Failed to connect to: {}, message: {} ", host, e.getMessage());
				invokeConnectFailureCallback(e);
			} catch (UnresolvedAddressException e) {
				logger.warn("Unresolvable host: {}", host);
				invokeConnectFailureCallback(e);
			}
		}
	}
	
	/**
	 * Close the socket.
	 */
	public void close() {
		Closeables.closeQuietly(ioLoop, channel);
		invokeCloseCallback();
	}
	
	/**
	 * The given callback will invoked when the underlaying {@code SelectableChannel} is closed. 
	 */
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
		SocketChannel sc = (SocketChannel) channel;
		if (sc.isConnectionPending()) {
			try {
				sc.finishConnect();
				invokeConnectSuccessfulCallback();
				interestOps &= ~SelectionKey.OP_CONNECT;
				ioLoop.updateHandler(channel, interestOps |= SelectionKey.OP_READ);
			} catch (ConnectException e) {
				logger.warn("Connect failed: {}", e.getMessage());
				invokeConnectFailureCallback(e);
			}
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
			ioLoop.updateHandler(channel, interestOps &= ~SelectionKey.OP_READ);
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
		readCallback = nopAsyncStringResult;
		cb.onSuccess(result);
	}
	
	private void invokeReadFailureCallback(Exception e) {
		AsyncResult<String> cb = readCallback;
		readCallback = nopAsyncStringResult;
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
	
	private void invokeConnectSuccessfulCallback() {
		AsyncResult<Boolean> cb = connectCallback;
		connectCallback = nopAsyncBooleanResult;
		cb.onSuccess(true);
	}
	
	private void invokeConnectFailureCallback(Exception e) {
		AsyncResult<Boolean> cb = connectCallback;
		connectCallback = nopAsyncBooleanResult;
		cb.onFailure(e);;
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
			Closeables.closeQuietly(ioLoop, channel);
		}
		writeBuffer.delete(0, written);
		logger.debug("wrote: {} bytes", written);
		logger.debug("writeBuffer size: {}", writeBuffer.length());
		if (writeBuffer.length() > 0) {
			ioLoop.updateHandler(channel, interestOps |= SelectionKey.OP_WRITE);
		} else {
			ioLoop.updateHandler(channel, interestOps &= ~SelectionKey.OP_WRITE);
			invokeWriteCallback();
		}
	}

}
