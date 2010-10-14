package org.deftserver.web.handler;

import java.io.File;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;

import org.deftserver.web.HttpException;
import org.deftserver.web.protocol.HttpRequest;
import org.deftserver.web.protocol.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		response.setHeader("Cache-Control", "public");
		String mimeType = mimeTypeMap.getContentType(file);
		if ("text/plain".equals(mimeType)) {
			mimeType += "; charset=utf-8";
		}
		response.setHeader("Content-Type", mimeType);
		String ifModifiedSince = request.getHeader("If-Modified-Since");
		if (ifModifiedSince != null) {
			long ims = Long.parseLong(ifModifiedSince);
			if (lastModified <= ims) {
				response.setStatusCode(304);	//Not Modified
				logger.debug("not modified");
				return;
			}
		}
		response.write(file);
	}

}
