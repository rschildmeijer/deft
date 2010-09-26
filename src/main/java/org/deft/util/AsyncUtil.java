package org.deft.util;

import org.deft.web.AsyncCallback;

public class AsyncUtil {
	
	public static <T> AsyncCallback<T> wrap(final AsyncCallback<T> clientCb) {
		return new AsyncCallback<T>() {

			@Override
			public void onFailure(Throwable caught) {
				clientCb.onFailure(caught);
			}

			@Override
			public void onSuccess(T result) {
				try {
					clientCb.onSuccess(result);
				} catch(Throwable t) {
					
				}
			}
		};
	}
	
}
