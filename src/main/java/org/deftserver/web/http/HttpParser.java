package org.deftserver.web.http;

import java.nio.ByteBuffer;


public class HttpParser {
	private final ByteBuffer buffer;
	
	private byte currentChar;
	
	private int currentLineLength;
	
	private int lineCount;
	

	
	private ErrorStatus status = ErrorStatus.OK;
	
	enum ErrorStatus {
		OK,
		TOO_LONG_REQUEST_LINE, 
		BAD_HEADER_NAME_FORMAT,
		BAD_REQUEST,
		
	}
	
	
	public HttpParser(ByteBuffer buffer) {
		this.buffer = buffer;
		lineCount = 0;
	}
	
	
	public boolean skipWhiteSpaceAndLine(){

		while(buffer.hasRemaining()){
			currentChar = buffer.get();
			if ((!isCRLF(currentChar) && !isWhiteSpace(currentChar))){
				currentLineLength = 0;
				return true;
			}
		}
		return false;
	}
	
	
	/**
	 * Reads the buffer till end of line
	 * @return
	 */
	public String readLine(){
		int position = buffer.position();

		while (buffer.hasRemaining() && currentLineLength < HttpServerDescriptor.REQUEST_LINE_MAX_SIZE){
			currentChar = buffer.get();
			
			if(isCRLF(currentChar)){
				break;
			}
			currentLineLength++;
		}
		
		if (currentLineLength > 0 && isCRLF(currentChar)){
			return new String (buffer.array(), position, buffer.position() - position-1);
		}
		
		if (currentLineLength >= HttpServerDescriptor.REQUEST_LINE_MAX_SIZE){
			status = ErrorStatus.TOO_LONG_REQUEST_LINE;
		}
		
		return null;

	}
	
	/**
	 * Reads a header name from buffer current position
	 * @param c
	 * @return
	 */
	public String readHeaderName(){

		int position = buffer.position();
		while (buffer.hasRemaining() && currentLineLength < HttpServerDescriptor.REQUEST_LINE_MAX_SIZE){
			currentChar = buffer.get();
			currentLineLength++;
			if (isCRLF(currentChar)){
				break;
			}
			if (currentChar == ':'){
				return new String (buffer.array(), position -1, buffer.position() - position );
			}
		}
		
		if (currentLineLength >= HttpServerDescriptor.REQUEST_LINE_MAX_SIZE){
			status = ErrorStatus.TOO_LONG_REQUEST_LINE;
		}
		
		if (isCRLF(currentChar)){
			status = ErrorStatus.BAD_HEADER_NAME_FORMAT;
		}
		return null;
	}
	
	/**
	 * True if the current parser position points to a space char
	 * @return
	 */
	public boolean isWhiteSpace(){
		return isWhiteSpace(currentChar);
	}
	
	/**
	 * Skips all end of line characters.
	 * @return
	 */
	public boolean skipEndOfLine(){
		boolean res =false;
		int i = 0;
		while(buffer.hasRemaining() && i < 3){
			currentChar = buffer.get();
			if (!isCRLF(currentChar)){
				break;
			}

			i++;
		}

		if (i>0 ){
			currentLineLength = 0;
			lineCount++;
		}

		if (i >= 3){
			res = true;
		}

		return res;
	}
	
	
	/**
	 * Parse the request line
	 * @param c
	 * @return
	 */
	public String[] readRequestLine (){
		String [] res = new String [3];
		int i = 0;
		int position = buffer.position() -1;
		while (buffer.hasRemaining() && currentLineLength < HttpServerDescriptor.REQUEST_LINE_MAX_SIZE){
			currentChar = buffer.get();
			currentLineLength++;
			if (isCRLF(currentChar)){
				break;
			}
			if (isWhiteSpace(currentChar)){
				res [i] = new String (buffer.array(), position, buffer.position() - position-1);
				position = buffer.position();
				i++;
			}
		}
		
		if (i == 2 && isCRLF(currentChar)){
			res[i] = new String (buffer.array(), position, buffer.position() - position-1);
			return res;
		}
		
		status = ErrorStatus.BAD_REQUEST;
		return null;
	}
	
	
	public ByteBuffer getBuffer() {
		return buffer;
	}
	
	/**
	 * True if underlying buffer {@link ByteBuffer#hasRemaining()} and 
	 * the current status is {@link ErrorStatus#OK}.
	 * @return
	 */
	public boolean hasRemaining(){
		return ErrorStatus.OK == status && buffer.hasRemaining(); 
	}

	/**
	 * True if 
	 * the current status is {@link ErrorStatus#OK}.
	 * @return
	 */
	public boolean hasErrors(){
		return ErrorStatus.OK == status; 
	}
	
	/**
	 * '\r' and '\n' are considered as CRLF
	 * @param c
	 * @return
	 */
	private boolean isCRLF(byte c){
		return ((c == '\r' )||(c == '\n'));
	}
	
	/**
	 * Htab and space chars are considered as white-space
	 * @param c
	 * @return
	 */
	private boolean isWhiteSpace(byte c){
		return (c == ' ') || (c == '\t');
	}
	
}
