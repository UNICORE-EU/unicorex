package de.fzj.unicore.uas.util;

import java.io.ByteArrayOutputStream;

public class LimitedByteArrayOutputStream extends ByteArrayOutputStream {

	private final int maxBytes;

	public LimitedByteArrayOutputStream(int maxBytes) {
		super();
		this.maxBytes = maxBytes;
	}

	public synchronized void write(byte b[], int off, int len) {
		if(buf.length+len>maxBytes)throw new RuntimeException("Max length exceeded!");
		super.write(b, off, len);
	}

	public synchronized void write(byte b) {
		if(buf.length+1>maxBytes)throw new RuntimeException("Max length exceeded!");
		super.write(b);
	}
}
