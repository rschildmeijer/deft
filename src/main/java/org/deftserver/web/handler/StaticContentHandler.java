package org.deftserver.web.handler;

import java.io.File;
import java.io.IOException;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;

import org.deftserver.web.HttpException;
import org.deftserver.web.protocol.HttpRequest;
import org.deftserver.web.protocol.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

/**
 *	A RequestHandler that serves static content (files) from a predefined directory. 
 *
 *	"Cache-Control: public" indicates that the response MAY be cached by any cache, even if it would normally be 
 *  non-cacheable or cacheable only within a non- shared cache.
 *
 */
public class StaticContentHandler extends RequestHandler {

	private final static Logger logger = LoggerFactory.getLogger(StaticContentHandler.class);

	private final static StaticContentHandler instance = new StaticContentHandler();

	private final FileTypeMap mimeTypeMap =  MimetypesFileTypeMap.getDefaultFileTypeMap();

	public static StaticContentHandler getInstance() {
		return instance;
	}

	@Override
	public void get(HttpRequest request, HttpResponse response) { 
		String path = request.getRequestedPath();
		File file = new File(path.substring(1));	// remove the leading '/'
		if (!file.exists()) {
			throw new HttpException(404);
		} else if (!file.isFile()) {
			throw new HttpException(403, path + "is not a file");
		}

		long lastModified = file.lastModified();
		response.setHeader("Last-Modified", String.valueOf(lastModified));
		response.setHeader("Cache-Control", "no-cache");
		String mimeType = mimeTypeMap.getContentType(file);
		response.setHeader("Content-Type", mimeType + "; charset=utf-8");

		String ifModifiedSince = request.getHeader("If-Modified-Since");
//		if (ifModifiedSince != null) {
//			long ims = Long.parseLong(ifModifiedSince);
//			if (lastModified <= ims) {
//				response.setStatusCode(304);	//Not Modified
//				logger.debug("not modified");
//				return;
//			}
//		}
		String resource = "";
		try {
			//Charset charset = java.nio.charset.Charset.defaultCharset();
			//resource = new String(Files.map(file));
			resource = Files.toString(file, Charsets.UTF_8);
			logger.debug("reading file...");
			//resource = readFileAsString(file);
		} catch (IOException e) {
			logger.error("Unable to read file. {}", e.getMessage());
			response.setStatusCode(500);	// Internal Server Error
		}
		response.write(resource);
	}
//
//	private static String readFileAsString(File file) throws java.io.IOException{
//		StringBuilder fileData = new StringBuilder(1000);
//		BufferedReader reader = new BufferedReader(
//				new FileReader(file));
//		char[] buf = new char[1024];
//		int numRead=0;
//		while((numRead=reader.read(buf)) != -1){
//			String readData = String.valueOf(buf, 0, numRead);
//			fileData.append(readData);
//			buf = new char[1024];
//		}
//		reader.close();
//		return fileData.toString();
//	}

}
