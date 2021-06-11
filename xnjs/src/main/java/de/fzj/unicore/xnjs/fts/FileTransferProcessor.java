package de.fzj.unicore.xnjs.fts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.ActionResult;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.ems.ProcessingException;
import de.fzj.unicore.xnjs.ems.processors.DefaultProcessor;
import de.fzj.unicore.xnjs.io.DataStageInInfo;
import de.fzj.unicore.xnjs.io.DataStageOutInfo;
import de.fzj.unicore.xnjs.io.IFileTransfer;
import de.fzj.unicore.xnjs.io.IFileTransferEngine;
import de.fzj.unicore.xnjs.io.IOProperties;
import de.fzj.unicore.xnjs.io.TransferInfo;
import de.fzj.unicore.xnjs.io.TransferInfo.Status;
import de.fzj.unicore.xnjs.util.LogUtil;

/**
 * Processor for reliable, multi-file server-to-server file transfers<br/>
 * 
 * Creates and manages a set of IFileTransfer instances, restarting them if needed
 * 
 * @author schuller
 */
public class FileTransferProcessor extends DefaultProcessor {

	private static final Logger logger=LogUtil.getLogger(LogUtil.JOBS, FileTransferProcessor.class);
	
	private final static String fileTransferKey="FILETRANSFERS";

	public FileTransferProcessor(XNJS xnjs){
		super(xnjs);
	}
	
	
	private JSONObject ftSpec;

	protected JSONObject getTransferSpecification(){
		if(ftSpec==null) {
			try{
				ftSpec = new JSONObject((String)action.getAjd());
			}catch(Exception ex) {}
		}
		return ftSpec;
	}

	/**
	 * Initiates the filetransfer
	 */
	protected void handleCreated() throws ProcessingException {
		try{
			JSONObject ftSpec = getTransferSpecification();
			
			action.setStatus(ActionStatus.DONE);
			action.setResult(new ActionResult(ActionResult.SUCCESSFUL,"OK"));
		}catch(Exception ex){
			throw new ProcessingException(ex);
		}
	}	
	
	protected void getIndividualTransfers() {
		JSONObject transfers = action.getProcessingContext().getAs(fileTransferKey, JSONObject.class);
		
	}
	
	protected IFileTransfer createImport(String workingDirectory, DataStageInInfo info)throws IOException{
		IFileTransfer ft = xnjs.get(IFileTransferEngine.class).
		   createFileImport(action.getClient(), workingDirectory, info);
		return ft;
	}
	
	protected IFileTransfer createExport(String workingDirectory, DataStageOutInfo info)throws IOException{
		IFileTransfer ft = xnjs.get(IFileTransferEngine.class).
		   createFileExport(action.getClient(), workingDirectory, info);
		return ft;
	}
	
	
	@Override
	@SuppressWarnings("unchecked")
	protected void handleAborting()throws ProcessingException{
		ArrayList<String> ftList=(ArrayList<String>)action.getProcessingContext().get(fileTransferKey);
		if(ftList==null)throw new IllegalStateException("Filetransfer list not found in context");
		Iterator<String>iter=ftList.iterator();
		while(iter.hasNext()){
			String ftId=iter.next();
			xnjs.get(IFileTransferEngine.class).abort(ftId);	
		}
		super.handleAborting();
	}
	
	@Override
	protected void handleRemoving()throws ProcessingException{
		//NOP
	}
	
	@Override
	@SuppressWarnings("unchecked")
	protected void handleRunning() throws ProcessingException {
		ArrayList<String> ftList=(ArrayList<String>)action.getProcessingContext().get(fileTransferKey);
		if(ftList==null)throw new IllegalStateException("Filetransfer list not found in context");
		Iterator<String>iter=ftList.iterator();
		while(iter.hasNext()){
			String ftId=iter.next();
			TransferInfo ft = getInfo(ftId);
			if(ft==null){
				//TODO how to deal with re-start of UNICORE/X ?
				throw new IllegalStateException("Internal server error: File transfer '"+ftId+"' not found!");
			}
			
			if(ft.getStatus()==Status.DONE){
				logger.debug("File transfer {} SUCCESSFUL.", ft.getUniqueId());
				xnjs.get(IFileTransferEngine.class).cleanup(ftId);
				iter.remove();
				action.setDirty();
			}
			else if(ft.getStatus()==Status.FAILED){
				logger.debug("File transfer {} FAILED.", ft.getUniqueId());
				if(!ft.isIgnoreFailure()){
					String message="Filetransfer FAILED: "+ft.getSource()+" -> "+ft.getTarget()+
					", error message: "+ft.getStatusMessage();
					action.addLogTrace(message);
					setToDoneAndFailed(message);
					cleanup();
					return;
				}
				else{
					String message="Ignoring FAILED filetransfer: "+ft.getSource()+" -> "+ft.getTarget();
					action.addLogTrace(message);
					iter.remove();
				}
			}
		}
		if(ftList.size()==0){
			action.setStatus(ActionStatus.DONE);
			action.setResult(new ActionResult(ActionResult.SUCCESSFUL));
			logger.debug("File transfers <{}> done.", action.getUUID());
		}
	}

	@SuppressWarnings("unchecked")
	protected void cleanup() throws ProcessingException{
		ArrayList<String> ftList=(ArrayList<String>)action.getProcessingContext().get(fileTransferKey);
		if(ftList==null)throw new IllegalStateException("Filetransfer list not found in context");
		IFileTransferEngine fte = xnjs.get(IFileTransferEngine.class);
		for(String ftId: ftList){
			fte.cleanup(ftId);
		}
	}
	
	protected TransferInfo getInfo(String id){
		return  xnjs.get(IFileTransferEngine.class).getInfo(id);
	}
	
	private static Executor exec=null;
	
	/**
	 * get an {@link Executor} for running filetransfers. 
	 * This default implementation uses a thread pool with a fixed number of threads, as
	 * given by {@link #getNumberOfFiletransferThreads()}
	 * @return {@link Executor}
	 */
	protected synchronized Executor getExecutor(){
		if(exec==null){
			ThreadFactory tf=new ThreadFactory(){
				private final AtomicInteger number=new AtomicInteger(0);
				@Override
				public Thread newThread(Runnable r) {
					Thread t=new Thread(r);
					t.setName("XNJS-Filetransfers-"+number.incrementAndGet());
					return t;
				}
			};
			ThreadPoolExecutor exec1=new ThreadPoolExecutor(getNumberOfFiletransferThreads(), getNumberOfFiletransferThreads(), 
					10000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), tf);
			exec1.allowCoreThreadTimeOut(true);
			exec=exec1;
		}
		return exec;
	}

	/**
	 * get the maximum number of threads in the thread pool to be used for file transfers.
	 * This corresponds to the maximum number of concurrent file transfers
	 * @return maximum number of concurrent filetransfers, the default number is 4
	 */
	protected int getNumberOfFiletransferThreads(){
		return xnjs.getIOProperties().getIntValue(IOProperties.FT_THREADS);
	}

}