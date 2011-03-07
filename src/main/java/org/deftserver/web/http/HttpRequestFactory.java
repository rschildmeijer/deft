package org.deftserver.web.http;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.deftserver.util.ArrayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

/**
 * Contains code for old parser support
 * 
 * @author slm
 * 
 */
public class HttpRequestFactory {

    private static final Logger LOG = LoggerFactory
            .getLogger(HttpRequest.class);

    /** Regex to parse raw headers and body */
    public static final Pattern RAW_VALUE_PATTERN = Pattern
            .compile("\\r\\n\\r\\n"); // TODO fix a better regexp for this
    /** Regex to parse raw headers from body */
    public static final Pattern HEADERS_BODY_PATTERN = Pattern
            .compile("\\r\\n");
    /** Regex to parse header name and value */
    public static final Pattern HEADER_VALUE_PATTERN = Pattern.compile(": ");

    public static HttpRequest of(ByteBuffer buffer) {
        try {
            String raw = new String(buffer.array(), 0, buffer.limit(),
                    Charsets.ISO_8859_1);
            String[] headersAndBody = RAW_VALUE_PATTERN.split(raw);
            String[] headerFields = HEADERS_BODY_PATTERN
                    .split(headersAndBody[0]);
            headerFields = ArrayUtil.dropFromEndWhile(headerFields, "");

            String requestLine = headerFields[0];
            Map<String, String> generalHeaders = new HashMap<String, String>();
            for (int i = 1; i < headerFields.length; i++) {
                String[] header = HEADER_VALUE_PATTERN.split(headerFields[i]);
                generalHeaders.put(header[0].toLowerCase(), header[1]);
            }

            String body = "";
            for (int i = 1; i < headersAndBody.length; ++i) { // First entry
                                                              // contains
                                                              // headers
                body += headersAndBody[i];
            }

            if (requestLine.contains("POST")) {
                int contentLength = Integer.parseInt(generalHeaders
                        .get("content-length"));
                if (contentLength > body.length()) {
                    return new PartialHttpRequest(requestLine, generalHeaders,
                            body);
                }
            }
            return new HttpRequest(requestLine, generalHeaders, body);
        } catch (Exception t) {
            return MalFormedHttpRequest.instance;
        }
    }

    public static HttpRequest continueParsing(ByteBuffer buffer,
            PartialHttpRequest unfinished) {
        String nextChunk = new String(buffer.array(), 0, buffer.limit(),
                Charsets.US_ASCII);
        unfinished.appendBody(nextChunk);

        int contentLength = Integer.parseInt(unfinished
                .getHeader("Content-Length"));
        if (contentLength > unfinished.getBody().length()) {
            return unfinished;
        } else {
            return new HttpRequest(unfinished.getRequestLine(),
                    unfinished.getHeaders(), unfinished.getBody());
        }
    }

}
