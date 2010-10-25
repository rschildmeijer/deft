package org.deftserver.example.kv;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Mocked KeyValueStoreHandler server (accepts a connection and echoes back the input)
 *
 */
public class KeyValueStore extends Thread {

	private final static Logger logger = LoggerFactory.getLogger(KeyValueStore.class);

	public static final String HOST = "127.0.0.1";
	public static final int PORT = 6379;

	private final static Map<String, String> dict = new HashMap<String, String>() {
		{ put("deft", "kickass"); }
	};

	private ServerSocket serverSocket;

	public KeyValueStore() {
		logger.debug("Initializing KeyValueStore");
		initialize();
	}

	public void run() {
		try {
			logger.debug("KeyValueStore waiting for clients...");	
			Socket clientSocket = serverSocket.accept();
			logger.debug("KeyValueStore client connected...");
			BufferedWriter os = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
			BufferedReader is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			String input = is.readLine();
			if (input.split("\\s+").length == 2) {
				input = input.split("\\s+")[1];	// "GET deft" => "deft"
			}
			logger.debug("KeyValueStore received input: {}", input);
			if (input != null) {
				logger.debug("KeyValueStore server sleeps...");
				try {
					Thread.sleep(1*500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				logger.debug("KeyValueStore woke up...");
				String value = dict.get(input);
				os.write(value, 0, value.length());
				logger.debug("KeyValueStore echoed back");
				os.flush();
			}

			{	// cleanup
				try {
					if (is != null)
						is.close();
					if (os != null)
						os.close();
					if (clientSocket != null)
						clientSocket.close();
				}
				catch (IOException ignore) {}
			}
		} catch (IOException e) { e.printStackTrace(); }
		logger.debug("Closing KeyValueStore");
	}

	private void initialize() {
		try {
			serverSocket = new ServerSocket(PORT);
		} catch (IOException e) { e.printStackTrace(); }
	}

}
