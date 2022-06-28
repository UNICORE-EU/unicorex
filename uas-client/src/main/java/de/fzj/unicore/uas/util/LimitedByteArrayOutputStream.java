package de.fzj.unicore.uas.util;

import java.io.ByteArrayOutputStream;

public class LimitedByteArrayOutputStream extends ByteArrayOutputStream {

	private final int maxBytes;

	/**
	 * @param maxBytes - limit the length of the underlying array
	 */
	public LimitedByteArrayOutputStream(int maxBytes) {
		super();
		this.maxBytes = maxBytes;
	}

	@Override
	public synchronized void write(byte b[], int off, int len) {
		if(buf.length+len>maxBytes)throw new RuntimeException("Max length exceeded!");
		super.write(b, off, len);
	}

	@Override
	public synchronized void write(int b) {
		if(buf.length+1>maxBytes)throw new RuntimeException("Max length exceeded!");
		super.write(b);
	}

}
