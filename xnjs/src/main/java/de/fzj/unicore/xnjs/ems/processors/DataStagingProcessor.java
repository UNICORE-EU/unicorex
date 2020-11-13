/*********************************************************************************
 * Copyright (c) 2006-2011 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *********************************************************************************/
 

package de.fzj.unicore.xnjs.ems.processors;

import java.io.IOException;
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

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.ActionResult;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.ems.ProcessingException;
import de.fzj.unicore.xnjs.idb.IDB;
import de.fzj.unicore.xnjs.io.DataStageInInfo;
import de.fzj.unicore.xnjs.io.DataStageOutInfo;
import de.fzj.unicore.xnjs.io.DataStagingInfo;
import de.fzj.unicore.xnjs.io.IFileTransfer;
import de.fzj.unicore.xnjs.io.IFileTransferEngine;
import de.fzj.unicore.xnjs.io.IOProperties;
import de.fzj.unicore.xnjs.io.StagingInfo;
import de.fzj.unicore.xnjs.io.TransferInfo;
import de.fzj.unicore.xnjs.io.TransferInfo.Status;
import de.fzj.unicore.xnjs.jsdl.JSDLProcessor;
import de.fzj.unicore.xnjs.util.LogUtil;

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
	
	protected void begin() throws ProcessingException {
	}

	/**
	 * Initiates a filetransfer for each staging element found.
	 * In case of errors (such as wrong protocol), the status of the
	 * whole staging action is set to "FAILED". 
	 */
	protected void handleCreated() throws ProcessingException {
		if(logger.isDebugEnabled()) {
			logger.debug("Adding file transfers for job <"+action.getParentActionID()
				+"> this action is <"+action.getUUID()+">");
		}
		try{
			StagingInfo dstInfo=(StagingInfo)action.getAjd();
			List<String> ftList=new ArrayList<String>();
			List<IFileTransfer> ftInstances=new ArrayList<IFileTransfer>();
			action.getProcessingContext().put(fileTransferKey,ftList);
			String uspace=action.getExecutionContext().getWorkingDirectory();
			if(dstInfo==null){
				setToDoneAndFailed("Internal server error: Data staging expected but not found. File transfers failed.");
			}
			else{
				List<String>filesToDelete=new ArrayList<String>();
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
							DataStageInInfo in=(DataStageInInfo)dst;
							ft=createImport(workingDirectory, in);
						}
						else{
							DataStageOutInfo out=(DataStageOutInfo)dst;
							ft=createExport(workingDirectory, out);
						}
						TransferInfo fti = ft.getInfo();
						fti.setParentActionID(action.getRootActionID());
						fti.setIgnoreFailure(dst.isIgnoreFailure());
						ftInstances.add(ft);
						//handle delete on termination
						boolean deleteOnTermination=dst.isDeleteOnTermination();
						if(deleteOnTermination){
							filesToDelete.add(dst.getFileName());
						}
					}catch(Exception e){
						if(!dst.isIgnoreFailure()){
							String msg=LogUtil.createFaultMessage("Error adding filetransfer",e);
							LogUtil.logException("Error adding filetransfer", e, logger);
							setToDoneAndFailed(msg);
							return;
						}
						else{
							action.addLogTrace("Ignoring failure to setup filetransfer");
						}
					}
				}
				if(filesToDelete.size()>0){
					action.getProcessingContext().put(JSDLProcessor.KEY_DELETEONTERMINATION, filesToDelete);
				}
				
				for(IFileTransfer ft:ftInstances){
					TransferInfo fti = ft.getInfo();
					try{
						getExecutor().execute(ft);
						action.addLogTrace("Started filetransfer "+fti.getSource()+" -> "+fti.getTarget());
						ftList.add(fti.getUniqueId());
					}catch(RejectedExecutionException e){
						LogUtil.logException("Error starting filetransfer: "
								+fti.getSource()+"->"+fti.getTarget(), e, logger);
						setToDoneAndFailed("Error starting filetransfer (internal work queue too full)");
						return;
					}
				}
				action.setStatus(ActionStatus.RUNNING);
			}
		}catch(Exception ex){
			throw new ProcessingException(ex);
		}
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
				if(logger.isDebugEnabled())logger.debug("File transfer "+ft.getUniqueId()+" successful.");
				xnjs.get(IFileTransferEngine.class).cleanup(ftId);
				iter.remove();
				action.setDirty();
			}
			else if(ft.getStatus()==Status.FAILED){
				if(logger.isDebugEnabled())logger.debug("File transfer "+ft.getUniqueId()+" failed.");
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
			if(logger.isDebugEnabled())logger.debug("File transfers <"+action.getUUID()+"> done.");
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