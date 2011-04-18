package org.deftserver.util;

import org.deftserver.web.AsyncResult;

/**
 * Convenience class used to limit the Java verboseness.
 */
public class NopAsyncResult<T> {
	
	private NopAsyncResult() {}
	
	public final AsyncResult<T> nopAsyncResult = new AsyncResult<T>() {

		@Override public void onFailure(Throwable caught) {}
		
		@Override public void onSuccess(T result) {} 
	
	};
	
	public static <T> NopAsyncResult<T> of(Class<T> type) {
		return new NopAsyncResult<T>();
	}
}
