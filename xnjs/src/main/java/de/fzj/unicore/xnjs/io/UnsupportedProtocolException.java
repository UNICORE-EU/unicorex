package de.fzj.unicore.xnjs.io;

import java.io.IOException;

/**
 * Thrown when a file transfer requests a protocol 
 * that is not supported by the loaded services
 * 
 * @author schuller
 */
public class UnsupportedProtocolException extends IOException {
	
	private static final long serialVersionUID=1L;

	public UnsupportedProtocolException(String message) {
		super(message);
	}

}
