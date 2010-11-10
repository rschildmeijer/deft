package org.deftserver.example.kv;

import org.deftserver.web.AsyncResult;
import org.deftserver.web.Timeout;

public class Client {
	
	private final Connection connection;
	
		
	public Client() {
		Timeout timeout = new Timeout(0, null);
		connection = new Connection(KeyValueStore.HOST, KeyValueStore.PORT, timeout);
	}

	public void connect() {
		connection.connect();
	}
	
//	public void disconnect() {
//		connection.disconnect();
//	}

	public void get(String value, AsyncResult<String> cb) {
		connection.write("GET deft\r\n", cb);
	}

}
