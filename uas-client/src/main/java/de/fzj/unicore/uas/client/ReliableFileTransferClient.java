package de.fzj.unicore.uas.client;

import java.io.OutputStream;
import java.util.List;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.uas.fts.FiletransferOptions;
import de.fzj.unicore.uas.fts.FiletransferOptions.SupportsPartialRead;
import de.fzj.unicore.uas.fts.ProgressListener;
import de.fzj.unicore.uas.fts.ProgressListener.CancelledException;
import eu.unicore.util.Log;

/**
 * uses a normal file transfer client (which has to provide a few extras) 
 * to provide reliable download features. The underlying file transfer client
 * has to support partial read (i.e. byte ranges). For example the BFT client and service 
 * (6.5.0 and later) support this feature. <br/>
 * The data is initially stored as chunks in a directory, which are then merged into the target 
 * file as soon as all chunks have been downloaded. 
 * <br/>
 * NOTE : Not all errors are caught with this client. For example, if the underlying file transfer
 * instance is deleted, this client will fail. To get even more reliability, the same transfer
 * can be retried with a new underlying file transfer<br/>
 * 
 * @author schuller
 * @since 6.5.0
 */
public class ReliableFileTransferClient implements FiletransferOptions.IMonitorable{

	private final static Logger logger=Log.getLogger(Log.SERVICES, ReliableFileTransferClient.class);

	private final SupportsPartialRead ftc;
	private final Store storage;
	private volatile boolean aborted=false;
	private int errorCount=0;
	private int maxErrors=42;
	private boolean ok=true;
	private String statusMessage="OK.";
	private ProgressListener<Long> listener;
	
	/**
	 * Create a new client for downloading data from the given data source
	 * 
	 * @param ftc - a data transfer client supporting partial reads
	 * @param storage - the storage
	 */
	public ReliableFileTransferClient(SupportsPartialRead ftc, Store storage){
		this.ftc=ftc;
		this.storage=storage;
	}
	
	@Override
	public void setProgressListener(ProgressListener<Long> listener) {
		this.listener=listener;
	}
	
	/**
	 * check if the "parent" transfer was cancelled
	 */
	protected void checkCancelled() throws ProgressListener.CancelledException{
		if(listener!=null && listener.isCancelled()){
			throw new ProgressListener.CancelledException();
		}
	}
	
	public void run()throws CancelledException{
		SupportsPartialRead srr=((SupportsPartialRead)ftc);
		statusMessage="RUNNING";
		while(!aborted){
			try{
				List<Chunk>chunks=storage.getChunks();
				if(chunks.size()==0){
					storage.finish();
					statusMessage="FINISHED";
					ok=true;
					break;
				}
				for(Chunk chunk: chunks){
					checkCancelled();
					if(logger.isDebugEnabled()){
						logger.debug("Downloading chunk : "+chunk);
					}
					OutputStream os=chunk.newOutputStream();
					try{
						long read=srr.readPartial(chunk.getOffset(), chunk.getLength(), os);
						storage.ack(chunk);
						if(listener!=null){
							listener.notifyProgress(read);
						}
					}
					finally{
						os.close();
					}
				}
				
			}catch(CancelledException ce){
				throw ce;
			
			}catch(Exception ex){
				//TODO need to detect non-recoverable errors, like permission
				//problems on the filesystem
				if(logger.isDebugEnabled()){
					logger.debug(Log.createFaultMessage("Error",ex));
				}
				errorCount++;
				if(errorCount>maxErrors){
					statusMessage="FAILED: max error count of <"+maxErrors+"> exceeded";
					ok=false;
					break;
				}
			}
		}
	}

	public void abort(){
		this.aborted=true;
	}

	public boolean ok(){
		return ok;
	}

	public String getStatusMessage(){
		return statusMessage;
	}
	
	/**
	 * highlevel interface for describing and writing a complete file
	 * as a set of chunks
	 * 
	 * @author schuller
	 */
	public static interface Store{

		/**
		 * get the non-acknowledged chunks to be downloaded
		 */
		List<Chunk>getChunks() throws Exception;

		/**
		 * call when a chunk was written successfully.
		 */
		void ack(Chunk chunk) throws Exception;

		/**
		 * call to finalize the whole download and clean up
		 * @throws Exception
		 */
		void finish() throws Exception;
	}

	/**
	 * A chunk is a part of the final file. Writing a chunk is
	 * atomic, i.e. it is either completely downloaded, or broken, 
	 * in the last case it has to be re-written.
	 * 
	 * @author schuller
	 */
	public static interface Chunk{
		
		/**
		 * create a new output stream for writing the chunk
		 */
		OutputStream newOutputStream() throws Exception;

		/**
		 * the offset of this chunk (i.e. the position in the
		 * real target file)
		 */
		long getOffset();

		/**
		 * the length of the chunk
		 */
		long getLength();
		
		/**
		 * the index of the chunk
		 */
		int getIndex();
		
		/**
		 * set the offset
		 * @param offset
		 */
		public void setOffset(long offset);
		
		/**
		 * set the length
		 * @param length
		 */
		public void setLength(long length);
		
		/**
		 * get the file path of this chunk (relative to working dir)
		 */
		public String getPath();
		
		/**
		 * append to existing file (which was already partially downloaded)
		 */
		public void setAppend(boolean append);
	}

}
