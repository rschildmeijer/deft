package org.deftserver.web;

/**
*	The generic interface a caller must implement to receive a result from an
* 	async call
*/
public interface AsyncResult<T> {
	
    /**
     * The asynchronous call failed to complete normally.
     * 
     * @param caught failure encountered while executing an async operation
     */
    void onFailure(Throwable caught);

    /**
     * Called when an asynchronous call completes successfully.
     * 
     * @param result return value of the async call
     */
    void onSuccess(T result);
	
}
