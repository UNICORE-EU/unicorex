package eu.unicore.xnjs.util;

import java.io.IOException;
import java.io.OutputStream;

/**
 *  output stream for writing to a backend that only supports chunks of data.
 *  Data is buffered internally in a buffer of configurable size, and written
 *  to the backend when flushed, closed, or the buffer is full 
 *  
 *  @author schuller
 */ 
public abstract class BackedOutputStream extends OutputStream {
	
	protected byte[] buffer=null;
	
	/**
	 * number of valid bytes in the buffer
	 */
	protected int pos=0;
	
	/**
	 * is this the first write operation?
	 */
	protected boolean firstWrite=true;
	
	/**
	 * did the user request to append to existing data?
	 */
	protected boolean append;
	
	/**
	 * is the output stream being closed, i.e. is this the last write operation?
	 */
	protected boolean closing = false;
	
	/**
	 * @param append - whether to append to an existing backend target
	 * @param size - buffer size to use
	 */

	public BackedOutputStream(boolean append, int size){
		this.append=append;
		while(buffer==null){
			try{
				buffer=new byte[size];
			}catch(OutOfMemoryError mem){
				size=size/2;
				if(size<8192){
					throw new OutOfMemoryError();
				}
			}
		}
	}


	@Override
	public void write(int b) throws IOException {
		if(pos>=buffer.length) flush();
		buffer[pos]=(byte)b;
		pos++;
	}

	@Override
	public void close() throws IOException {
		closing = true;
		flush();
	}

	@Override
	public void flush() throws IOException {
		writeBuffer();
		firstWrite=false;
		pos=0;
	}

	/**
	 * write the buffer contents (i,e, the bytes buffer[0] to buffer[pos-1]) 
	 * to the backend. The current state must be checked using 
	 * the <code>firstWrite</code>, <code>append</code> and <code>closing</code> flags
	 * 
	 * @throws IOException
	 */
	protected abstract void writeBuffer() throws IOException;
	
}
