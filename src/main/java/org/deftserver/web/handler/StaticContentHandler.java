package org.deftserver.web.handler;

import java.io.File;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;

import org.deftserver.web.http.HttpException;
import org.deftserver.web.http.HttpRequest;
import org.deftserver.web.http.HttpResponse;
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

	/** {inheritDoc} */
	@Override
	public void get(HttpRequest request, HttpResponse response) {
		this.perform(request, response, true);
	}
	
	/** {inheritDoc} */
	@Override
	public void head(final HttpRequest request, final HttpResponse response) {
		this.perform(request, response, false);
	}

	/**
	 * @param request the <code>HttpRequest</code>
	 * @param response the <code>HttpResponse</code> 
	 * @param hasBody <code>true</code> to write the message body; <code>false</code> otherwise.
	 */
	private void perform(final HttpRequest request, final HttpResponse response, boolean hasBody) {
		
		final String path = request.getRequestedPath();
		final File file = new File(path.substring(1));	// remove the leading '/'
		if (!file.exists()) {
			throw new HttpException(404);
		} else if (!file.isFile()) {
			throw new HttpException(403, path + "is not a file");
		}

		final long lastModified = file.lastModified();
		response.setHeader("Last-Modified", String.valueOf(lastModified));
		response.setHeader("Cache-Control", "public");
		String mimeType = mimeTypeMap.getContentType(file);
		if ("text/plain".equals(mimeType)) {
			mimeType += "; charset=utf-8";
		}
		response.setHeader("Content-Type", mimeType);
		final String ifModifiedSince = request.getHeader("If-Modified-Since");
		if (ifModifiedSince != null) {
			long ims = Long.parseLong(ifModifiedSince);
			if (lastModified <= ims) {
				response.setStatusCode(304);	//Not Modified
				logger.debug("not modified");
				return;
			}
		}
		
		if(hasBody) {
			response.write(file);
		}
	}
}
