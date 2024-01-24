package eu.unicore.uas.fts;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface FiletransferOptions {

	/**
	 * file transfers that use chunks can implement this interface
	 * to allow setting the chunksize
	 */
	public static interface IChunked{
		public void setChunksize(int chunksize);
	}
	
	/**
	 * file transfers that support partial reads chunks should 
	 * implement this interface
	 */
	public static interface SupportsPartialRead{
		
		/**
		 * @param offset - where to start reading
		 * @param length - number of bytes to read
		 * @param os - output stream to write to
		 * @return the number of bytes actually read (always >= 0)
		 * @throws IOException
		 */
		public long readPartial(long offset, long length, OutputStream os)throws IOException;
		
	}
	
	/**
	 * file transfers can implement this interface
	 * to allow progress monitoring
	 */
	public static interface IMonitorable{
		public void setProgressListener(ProgressListener<Long> listener);
	}
	
	/**
	 * file transfers can implement this interface
	 * to show they support append
	 */
	public static interface IAppendable{
		public void setAppend();
	}
	
	/**
	 * file transfers supporting reads implement this interface
	 */
	public static interface Read{
		
		/**
		 * convenience method that reads all data from the remote location
		 * and writes it to an output stream
		 * 
		 * @param sink - a local OutputStream to write the data to
		 */
		public void readAllData(OutputStream sink)throws Exception;
	}
	
	/**
	 * file transfers supporting writes implement this interface
	 */
	public static interface Write{
		
		/**
		 * Writes all data from <code>source</code> to the remote location<br/>
		 * In case the remote file exists, it is overwritten.
		 *
		 * @param source - an InputStream supplying local data
		 * @throws Exception
		 */
		public void writeAllData(InputStream source)throws Exception;
		
		/**
		 * Writes <code>numBytes</code> bytes of data from <code>source</code> to the 
		 * remote location. If <code>numBytes</code> is negative, all bytes from the input
		 * stream (until EOF) are read and written to the remote location.<br/>
		 * In case the remote file exists, it is overwritten.
		 *
		 * @param source
		 * @param numBytes - how many bytes to read from the source, or -1 if all data should be read
		 * @throws Exception
		 * @since 1.4.0
		 */
		public void writeAllData(InputStream source, long numBytes)throws Exception;

	}

	/**
	 * file transfers that support getting a stream for reading data implement this interface
	 */
	public static interface ReadStream {
		
		/**
		 * get an input stream for reading from the remote source
		 */
		public InputStream getInputStream()throws Exception;
	}
	
	/**
	 * file transfers that support getting a stream for writing data implement this interface
	 */
	public static interface WriteStream {
		
		/**
		 * get an output stream for writing to the remote target
		 */
		public OutputStream getOutputStream()throws Exception;
	}
}
