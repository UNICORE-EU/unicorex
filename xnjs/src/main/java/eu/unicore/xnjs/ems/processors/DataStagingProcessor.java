package eu.unicore.xnjs.ems.processors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Logger;

import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.ActionResult;
import eu.unicore.xnjs.ems.ActionStatus;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.idb.IDB;
import eu.unicore.xnjs.io.DataStageInInfo;
import eu.unicore.xnjs.io.DataStageOutInfo;
import eu.unicore.xnjs.io.DataStagingInfo;
import eu.unicore.xnjs.io.IFileTransfer;
import eu.unicore.xnjs.io.IFileTransferEngine;
import eu.unicore.xnjs.io.IOProperties;
import eu.unicore.xnjs.io.StagingInfo;
import eu.unicore.xnjs.io.TransferInfo;
import eu.unicore.xnjs.io.TransferInfo.Status;
import eu.unicore.xnjs.util.LogUtil;

/**
 * Processor for data staging actions<br/>
 * 
 * For each data staging element an instance of {@link IFileTransfer} is created and
 * started. These transfers are then monitored. Once they are done (or failed) the
 * data staging action is DONE, and the parent action can continue processing
 * 
 * @see DataStageInInfo
 * @see DataStageOutInfo
 * 
 * @author schuller
 */
public class DataStagingProcessor extends DefaultProcessor {

	private static final Logger logger=LogUtil.getLogger(LogUtil.JOBS,DataStagingProcessor.class);

	private final static String fileTransferKey="FILETRANSFERS";

	public DataStagingProcessor(XNJS xnjs){
		super(xnjs);
	}

	/**
	 * Initiates a filetransfer for each staging element found.
	 * In case of errors (such as wrong protocol), the status of the
	 * whole staging action is set to "FAILED". 
	 */
	protected void handleCreated() throws ExecutionException {
		logger.trace("Adding file transfers for job {}", action.getParentActionID());
		try{
			StagingInfo dstInfo=(StagingInfo)action.getAjd();
			List<String> ftList = new ArrayList<>();
			List<IFileTransfer> ftInstances = new ArrayList<>();
			action.getProcessingContext().put(fileTransferKey,ftList);
			String uspace=action.getExecutionContext().getWorkingDirectory();
			if(dstInfo==null){
				setToDoneAndFailed("Internal server error: Data staging expected but not found. File transfers failed.");
			}
			else{
				List<String>filesToDelete = new ArrayList<>();
				for(DataStagingInfo dst:dstInfo){
					try{
						IFileTransfer ft=null;
						String workingDirectory=uspace;
						String fsName=dst.getFileSystemName();
						if(fsName!=null){
							String fs=xnjs.get(IDB.class).getFilespace(fsName);
							if(fs==null){
								throw new Exception("Requested file system <"+fsName+"> is not available at this site.");
							}
							workingDirectory = xnjs.getTargetSystemInterface(action.getClient()).resolve(fs);
						}
						if(dst instanceof DataStageInInfo){
							ft = createImport(workingDirectory, (DataStageInInfo)dst);
							ft.setUmask(action.getUmask());
						}
						else{
							ft = createExport(workingDirectory, (DataStageOutInfo)dst);
						}
						TransferInfo fti = ft.getInfo();
						fti.setParentActionID(action.getRootActionID());
						fti.setIgnoreFailure(dst.isIgnoreFailure());
						ftInstances.add(ft);
						if(dst.isDeleteOnTermination()){
							filesToDelete.add(dst.getFileName());
						}
					}catch(Exception e){
						if(!dst.isIgnoreFailure()){
							String msg = LogUtil.createFaultMessage("Error adding filetransfer",e);
							setToDoneAndFailed(msg);
							return;
						}
						else{
							action.addLogTrace("Ignoring failure to setup filetransfer");
						}
					}
				}
				if(filesToDelete.size()>0){
					action.getProcessingContext().put(JobProcessor.KEY_DELETEONTERMINATION, filesToDelete);
				}
				for(IFileTransfer ft:ftInstances){
					TransferInfo fti = ft.getInfo();
					try{
						getExecutor().execute(ft);
						action.addLogTrace("Started filetransfer "+fti);
						ftList.add(fti.getUniqueId());
					}catch(RejectedExecutionException e){
						LogUtil.logException("Error starting filetransfer "+fti, e, logger);
						setToDoneAndFailed("Error starting filetransfer (internal work queue too full)");
						return;
					}
				}
				action.setStatus(ActionStatus.RUNNING);
			}
		}catch(Exception ex){
			throw ExecutionException.wrapped(ex);
		}
	}	

	protected IFileTransfer createImport(String workingDirectory, DataStageInInfo info)throws Exception{
		return xnjs.get(IFileTransferEngine.class).createFileImport(action.getClient(), workingDirectory, info);
	}

	protected IFileTransfer createExport(String workingDirectory, DataStageOutInfo info)throws Exception{
		return xnjs.get(IFileTransferEngine.class).createFileExport(action.getClient(), workingDirectory, info);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void handleAborting()throws ExecutionException {
		ArrayList<String> ftList = (ArrayList<String>)action.getProcessingContext().get(fileTransferKey);
		if(ftList==null)throw new IllegalStateException("Filetransfer list not found in context");
		ftList.forEach((ftId)-> xnjs.get(IFileTransferEngine.class).abort(ftId));
		super.handleAborting();
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void handleRunning() throws ExecutionException {
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
				logger.trace("File transfer {} SUCCESSFUL.", ft);
				xnjs.get(IFileTransferEngine.class).cleanup(ftId);
				iter.remove();
				action.setDirty();
			}
			else if(ft.getStatus()==Status.FAILED){
				logger.trace("File transfer {} FAILED.", ft.getUniqueId());
				if(!ft.isIgnoreFailure()){
					String message="Filetransfer FAILED: "+ft+" error message: "+ft.getStatusMessage();
					action.addLogTrace(message);
					setToDoneAndFailed(message);
					cleanup();
					return;
				}
				else{
					action.addLogTrace("Ignoring FAILED filetransfer "+ft);
					iter.remove();
				}
			}
		}
		if(ftList.size()==0){
			action.setStatus(ActionStatus.DONE);
			action.setResult(new ActionResult(ActionResult.SUCCESSFUL));
			logger.trace("File transfers <{}> done.", action.getUUID());
		}
	}

	@SuppressWarnings("unchecked")
	protected void cleanup() throws ExecutionException {
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