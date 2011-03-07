package org.deftserver.web.http;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

import org.deftserver.io.buffer.DynamicByteBuffer;
import org.deftserver.web.HttpVerb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMultimap;

public class HttpRequest {

    private static final Logger LOG = LoggerFactory
            .getLogger(HttpRequest.class);

    private static final HttpRequestParser parser = new HttpRequestParser();

    private final String requestLine;
    private final HttpVerb method;
    private final String requestedPath; // correct name?
    private final String version;
    protected Map<String, String> headers;
    private ImmutableMultimap<String, String> parameters;

    private boolean keepAlive;

    /** Regex to parse HttpRequest Request Line */
    public static final Pattern REQUEST_LINE_PATTERN = Pattern.compile(" ");
    /** Regex to parse out QueryString from HttpRequest */
    public static final Pattern QUERY_STRING_PATTERN = Pattern.compile("\\?");
    /** Regex to parse out parameters from query string */
    public static final Pattern PARAM_STRING_PATTERN = Pattern.compile("\\&|;"); // Delimiter
                                                                                 // is
                                                                                 // either
                                                                                 // &
                                                                                 // or
                                                                                 // ;
    /** Regex to parse out key/value pairs */
    public static final Pattern KEY_VALUE_PATTERN = Pattern.compile("=");

    private String bodyString;
    private DynamicByteBuffer body;
    protected int contentLength;

    public HttpRequest(String[] requestLine, Map<String, String> headers,
            DynamicByteBuffer _body) {
        this.requestLine = new StringBuffer(requestLine[0]).append(' ')
                .append(requestLine[1]).append(' ').append(requestLine[2])
                .toString();

        method = HttpVerb.valueOf(requestLine[0]);
        String[] pathFrags = QUERY_STRING_PATTERN.split(requestLine[1]);
        requestedPath = pathFrags[0];
        version = requestLine[2];
        this.headers = headers;
        body = _body;
        initKeepAlive();
        parameters = parseParameters(requestLine[1]);
        if (headers.containsKey("content-length")) {
            contentLength = Integer.parseInt(headers.get("content-length")
                    .trim());
        }
    }

    public HttpRequest(String requestLine, Map<String, String> headers) {
        this.requestLine = requestLine;

        String[] elements = REQUEST_LINE_PATTERN.split(requestLine);
        String[] pathFrags = QUERY_STRING_PATTERN.split(elements[1]);
        requestedPath = pathFrags[0];
        method = HttpVerb.valueOf(elements[0]);

        version = elements[2];
        this.headers = headers;
        body = null;
        initKeepAlive();
        parameters = parseParameters(elements[1]);
    }

    public HttpRequest(String requestLine, Map<String, String> headers,
            String body) {
        this(requestLine, headers);
        this.bodyString = body;
    }

    public static HttpRequest of(ByteBuffer buffer) {
        try {
            return parser.parseRequestBuffer(buffer);
        } catch (Exception t) {
            LOG.error("Bad HTTP format", t);
            return MalFormedHttpRequest.instance;
        }
    }

    public static HttpRequest continueParsing(ByteBuffer buffer,
            PartialHttpRequest unfinished) {

        return parser.parseRequestBuffer(buffer, unfinished);

    }

    public String getRequestLine() {
        return requestLine;
    }

    public String getRequestedPath() {
        return requestedPath;
    }

    public boolean isFinished() {
        return true;
    }

    public String getVersion() {
        return version;
    }

    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    public String getHeader(String name) {
        return headers.get(name.toLowerCase());
    }

    public HttpVerb getMethod() {
        return method;
    }

    public int getContentLength() {
        return contentLength;
    }

    /**
     * Returns the value of a request parameter as a String, or null if the
     * parameter does not exist.
     * 
     * You should only use this method when you are sure the parameter has only
     * one value. If the parameter might have more than one value, use
     * getParameterValues(java.lang.String). If you use this method with a
     * multi-valued parameter, the value returned is equal to the first value in
     * the array returned by getParameterValues.
     */
    public String getParameter(String name) {
        Collection<String> values = parameters.get(name);
        return values.isEmpty() ? null : values.iterator().next();
    }

    public Map<String, Collection<String>> getParameters() {
        return parameters.asMap();
    }

    public String getBody() {
        if (body != null) {

            return new String(body.getByteBuffer().array(), 0, body
                    .getByteBuffer().limit());

        }
        return bodyString;
    }

    public DynamicByteBuffer getBodyBuffer() {
        return body;
    }

    /**
     * Returns a collection of all values associated with the provided
     * parameter. If no values are found and empty collection is returned.
     */
    public Collection<String> getParameterValues(String name) {
        return parameters.get(name);
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("METHOD: ").append(method).append("\n");
        sb.append("VERSION: ").append(version).append("\n");
        sb.append("PATH: ").append(requestedPath).append("\n");

        sb.append("--- HEADER --- \n");
        for (String key : headers.keySet()) {
            sb.append(key).append(":").append(headers.get(key)).append("\n");
        }

        sb.append("--- PARAMETERS --- \n");
        for (String key : parameters.keySet()) {
            Collection<String> values = parameters.get(key);
            for (String value : values) {
                sb.append(key).append(":").append(value).append("\n");
            }
        }

        sb.append("---- BODY ---- \n");
        sb.append(this.getBody());
        return sb.toString();
    }

    private ImmutableMultimap<String, String> parseParameters(String requestLine) {
        ImmutableMultimap.Builder<String, String> builder = ImmutableMultimap
                .builder();
        String[] str = QUERY_STRING_PATTERN.split(requestLine);

        // Parameters exist
        if (str.length > 1) {
            String[] paramArray = PARAM_STRING_PATTERN.split(str[1]);
            for (String keyValue : paramArray) {
                String[] keyValueArray = KEY_VALUE_PATTERN.split(keyValue);
                // We need to check if the parameter has a value associated with
                // it.
                if (keyValueArray.length > 1) { // name, value
                    builder.put(keyValueArray[0], keyValueArray[1]);
                }
            }
        }
        return builder.build();
    }

    protected void initKeepAlive() {
        String connection = getHeader("Connection");
        if ("keep-alive".equalsIgnoreCase(connection)) {
            keepAlive = true;
        } else if ("close".equalsIgnoreCase(connection)
                || requestLine.contains("1.0")) {
            keepAlive = false;
        } else {
            keepAlive = true;
        }
    }

}
