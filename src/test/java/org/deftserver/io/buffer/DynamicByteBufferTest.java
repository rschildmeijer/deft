package org.deftserver.io.buffer;

import static org.junit.Assert.assertEquals;

import org.deftserver.io.buffer.DynamicByteBuffer;
import org.junit.Before;
import org.junit.Test;

public class DynamicByteBufferTest {

	private DynamicByteBuffer dbb;
	private static final int INITIAL_CAPACITY = 10;	// bytes
	
	@Before
	public void allocation() {
		this.dbb = DynamicByteBuffer.allocate(INITIAL_CAPACITY);
	}
	
	@Test
	public void testAllocation() {
		assertInternalState(INITIAL_CAPACITY, 0, INITIAL_CAPACITY, INITIAL_CAPACITY);
	}
	
	private void assertInternalState(int expectedCapacity, int expectedPosition, int expectedLimit, int arrayLength) {
		assertEquals(expectedCapacity, dbb.capacity());
		assertEquals(expectedPosition, dbb.position());
		assertEquals(expectedLimit, dbb.limit());
		assertEquals(arrayLength, dbb.array().length);
	}
	
	@Test
	public void testNoReallactionPut() {
		byte[] data = new byte[] {'q', 'w', 'e', 'r', 't', 'y'};
		dbb.put(data);
		assertInternalState(INITIAL_CAPACITY, data.length, INITIAL_CAPACITY, INITIAL_CAPACITY);
	}
	
	@Test
	public void testReallocationTriggeredPut() {
		byte[] data = new byte[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A'};
		dbb.put(data);
		assertInternalState(16, 11, 16, 16);
		
		dbb.put(data);
		assertInternalState(24, 22, 24, 24);
		
		dbb.put(data);
		dbb.put(data);
		assertInternalState(54, 44, 54, 54);
	}
	
	@Test
	public void testPrepend() {
		byte[] data = new byte[] {'[', 'q', 'w', 'e', 'r', 't', 'y', ']'};
		dbb.put(data);
		
		String initial = "|HTTP/1.1 200 OK|";
		int initialLength = initial.length();
		dbb.prepend(initial);
		
		int expectedCapacity = data.length + initialLength;
		int expectedPosition = data.length + initialLength;
		int expectedLimit 	 = data.length + initialLength;
		int arrayLength 	 = data.length + initialLength;
		assertInternalState(expectedCapacity, expectedPosition, expectedLimit, arrayLength);
		
		dbb.put(data);
		expectedCapacity += expectedCapacity * 0.5;	
		expectedPosition += data.length;
		expectedLimit 	 += expectedLimit * 0.5;
		arrayLength 	 += arrayLength * 0.5;
		assertInternalState(expectedCapacity, expectedPosition, expectedLimit, arrayLength);
	}
	
}
