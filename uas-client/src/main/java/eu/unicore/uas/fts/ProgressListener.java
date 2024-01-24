package eu.unicore.uas.fts;

import java.io.IOException;

/**
 * A progress listener can be registered with filetransfers, in order
 * to receive notifications when data chunks have been written
 * 
 * @param <T> - the type of the value that is being monitored
 *
 * @author schuller
 */
public interface ProgressListener<T>{

	/**
	 * Notify the listener that some bytes have been written
	 * 
	 * @param amount - the number of bytes written since the last call to notifyProgress
	 */
	public void notifyProgress(T amount);
	
	
	/**
	 * check if the process monitoring this filetransfer has requested to cancel it.
	 * The file transfer code will try to honor this request, and throw a {@link CancelledException}
	 * 
	 * @return <code>true</code> if transfer should be cancelled
	 */
	public boolean isCancelled();
	
	
	/**
	 * this exception is thrown by file transfer code when a "cancel" request
	 * has been received
	 */
	public static class CancelledException extends IOException {

		private static final long serialVersionUID = 1L;

		public CancelledException() {
			super();
		}

		public CancelledException(String message, Throwable cause) {
			super(message + "; caused by: " + cause.toString());
		}

		public CancelledException(String message) {
			super(message);
		}

		public CancelledException(Throwable cause) {
			super("Caused by: " + cause.toString());
		}
		
	} 
	
}
