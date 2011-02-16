package org.deftserver.io;

public class IOLoopFactory {

	public static enum Mode {
		SINGLE_THREADED,
		MULTI_THREADED;
	}
	
	private static  Mode mode;
	
	 static void setMode(Mode _mode){
		 mode  = _mode;
	 }
	
	public static IOLoopController getLoopController (){
		if(mode == Mode.SINGLE_THREADED){
			return IOLoopFactory.getLoopController();
		}else {
			return DefaultIOWorkerLoop.getInstance();
		}
	}
}
