package org.deft.exception;

public class MultipleWritesException extends RuntimeException {

	public MultipleWritesException() {
	}
	
	public MultipleWritesException(String msg) {
		super(msg);
	}
}
