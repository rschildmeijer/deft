package org.deftserver.example.kv;

import org.deftserver.io.timeout.Timeout;
import org.deftserver.web.AsyncResult;

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
