package de.fzj.unicore.xnjs.io;

import java.io.IOException;

/**
 * Thrown when a file transfer option is requested that is not supported
 * 
 * @author schuller
 */
public class OptionNotSupportedException extends IOException {
	
	private static final long serialVersionUID=1L;

	public OptionNotSupportedException(String message) {
		super(message);
	}

}
