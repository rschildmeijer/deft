package org.deftserver.io.buffer;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import org.deftserver.web.http.HttpServerDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

public class DynamicByteBuffer {
	
	private final static Logger logger = LoggerFactory.getLogger(DynamicByteBuffer.class);

	private ByteBuffer backend;

	private DynamicByteBuffer(ByteBuffer bb) { 	
		this.backend = bb;
	}
	
	public static DynamicByteBuffer allocate(int capacity) {
		return new DynamicByteBuffer(ByteBuffer.allocate(capacity));
	}

	public void put(byte[] src) {
		ensureCapacity(src.length);
		backend.put(src);
	}
	
	public void prepend(String data) {
		byte[] bytes = data.getBytes(Charsets.UTF_8);
		int newSize = bytes.length + backend.position();
		byte[] newBuffer = new byte[newSize];
		System.arraycopy(bytes, 0, newBuffer, 0, bytes.length);	// initial line and headers
		System.arraycopy(backend.array(), 0, newBuffer, bytes.length, backend.position()); // body
		backend = ByteBuffer.wrap(newBuffer);
		backend.position(newSize);
	}
	
	/**
	 * Ensures that its safe to append size data to backend. 
	 * @param size The size of the data that is about to be appended.
	 */
	private void ensureCapacity(int size) {
		int remaining = backend.remaining();
		if (size > remaining) {
			
			int missing = size - remaining;
			logger.debug("allocating new DynamicByteBuffer, old capacity {}: ", backend.capacity());
			int newSize =  backend.capacity() + missing + HttpServerDescriptor.WRITE_BUFFER_SIZE; // Add a load factor to improve capacity when needed
			reallocate(newSize);
		}
	}
	
	// Preserves position.
	private void reallocate(int newCapacity) {
		int oldPosition = backend.position();
		byte[] newBuffer = new byte[newCapacity];
		System.arraycopy(backend.array(), 0, newBuffer, 0, backend.position());
		backend = ByteBuffer.wrap(newBuffer);
		backend.position(oldPosition);

		logger.debug("allocated new DynamicByteBufer, new capacity: {}", backend.capacity());
	}
	
	public ByteBuffer getByteBuffer() {
		return backend;
	}

	public void flip() {
		backend.flip();
	}

	public int limit() {
		return backend.limit();
	}

	public int position() {
		return backend.position();
	}

	public byte[] array() {
		return backend.array();
	}

	public int capacity() {
		return backend.capacity();
	}

	public boolean hasRemaining() {
		return backend.hasRemaining();
	}

	public DynamicByteBuffer compact() {
		backend.compact();
		return this;
	}

	public DynamicByteBuffer clear() {
		backend.clear();
		return this;
	}
}
