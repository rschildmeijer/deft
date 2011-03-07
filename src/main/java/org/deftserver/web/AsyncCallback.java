package org.deftserver.web;


/**
*	The generic interface a caller must implement to receive an arbitrary callback from an
* 	async call (similar to {@link AsyncResult}).
*/
public interface AsyncCallback {
	
	public static final AsyncCallback nopCb = new AsyncCallback() { @Override public void onCallback() { /*nop*/} };
	
	void onCallback();
	
}
