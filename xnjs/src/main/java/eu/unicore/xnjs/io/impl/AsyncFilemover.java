package eu.unicore.xnjs.io.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;

import eu.unicore.persist.util.UUID;
import eu.unicore.security.Client;
import eu.unicore.util.Log;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.io.ChangePermissions;
import eu.unicore.xnjs.io.IFileTransfer;
import eu.unicore.xnjs.io.IFileTransferEngine;
import eu.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.xnjs.io.TransferInfo;
import eu.unicore.xnjs.io.TransferInfo.Status;
import eu.unicore.xnjs.io.XnjsFile;
import eu.unicore.xnjs.tsi.TSI;
import eu.unicore.xnjs.util.AsyncCommandHelper;
import eu.unicore.xnjs.util.FileMonitor;
import eu.unicore.xnjs.util.IOUtils;
import eu.unicore.xnjs.util.LogUtil;
import eu.unicore.xnjs.util.Observer;
import eu.unicore.xnjs.util.ResultHolder;

/**
 * common base class for data movement using some external executable (wget, curl, globus-url-copy etc)
 * 
 * @author schuller
 */
public abstract class AsyncFilemover implements IFileTransfer,Observer<XnjsFile> {

	protected static final Logger logger=LogUtil.getLogger(LogUtil.IO,AsyncFilemover.class);

	//metrics/usage logger
	protected static final Logger usageLogger = Log.getLogger(Log.SERVICES+".datatransfer.USAGE", AsyncFilemover.class);

	protected final String workingDirectory;

	protected final Client client;

	protected final XNJS xnjs;

	protected AsyncCommandHelper ach;

	protected FileMonitor monitor=null;
	
	protected IStorageAdapter storageAdapter;

	protected OverwritePolicy overwrite;

	private String permissions = null;

	protected volatile boolean abort=false;

	protected long startTime;

	protected String preferredLoginNode = null;

	protected final TransferInfo info;

	public AsyncFilemover(Client client, String workingDirectory, String source, String target, XNJS xnjs){
		this.xnjs = xnjs;
		this.workingDirectory = workingDirectory;
		this.client = client;
		this.info = new TransferInfo(UUID.newUniqueID(), source, target);
	}

	public TransferInfo getInfo(){
		return info;
	}

	/**
	 * generate the commandline to execute
	 */
	protected abstract String makeCommandline()throws Exception;

	/**
	 * invoked immediately before submitting the async command.<br/>
	 * It can be used to customize, e.g. set environment variables.<br/>
	 * (The default implementation does nothing)
	 */
	protected void preSubmit()throws Exception{
	}

	protected abstract boolean isImport();

	@Override
	public void run() {
		startTime=System.currentTimeMillis();

		if(storageAdapter==null){
			TSI tsi = xnjs.getTargetSystemInterface(client, preferredLoginNode);
			tsi.setStorageRoot(workingDirectory);
			storageAdapter = tsi;
		}

		if(abort){
			info.setStatus(Status.ABORTED, "Aborted");
			return;
		}
		info.setStatus(Status.RUNNING, "Running");
		logger.debug("Submitting "+this);

		try{
			if(isImport()) {
				createParentDirectories();
				//register a listener on the target file
				monitor = new FileMonitor(workingDirectory, info.getTarget(), client, xnjs,
						preferredLoginNode, 3, TimeUnit.SECONDS);
				monitor.registerObserver(this);
			}
			doRun();
			try {
				if(permissions!=null && isImport()) {
					storageAdapter.chmod2(info.getTarget(), ChangePermissions.getChangePermissions(permissions), false);
				}
			}catch(Exception ex) {}
			//force a last update on the file info
			if(monitor!=null)monitor.run();
			reportUsage();
			xnjs.get(IFileTransferEngine.class).updateInfo(info);
		}catch(Exception ex){
			reportFailure("Could not do transfer", ex);
			LogUtil.logException("Could not do transfer",ex,logger);
		}
		finally{
			if(monitor!=null)monitor.dispose();
		}
	}

	protected void doRun() throws Exception {
		String cmd = makeCommandline();
		ach = new AsyncCommandHelper(xnjs,cmd,info.getUniqueId(),info.getParentActionID(),client);
		ach.setPreferredExecutionHost(preferredLoginNode);
		preSubmit();
		ach.submit();
		while(!ach.isDone()){
			Thread.sleep(2000);
		}
		ResultHolder res=ach.getResult();
		if(res.getExitCode()!=null && res.getExitCode()==0){
			logger.debug("Async transfer {} -> {} is DONE.", info.getSource(), info.getTarget());
			info.setStatus(Status.DONE);
		}
		else{
			String statusMessage="Transfer failed.";
			try{
				if(res!=null && res.getResult().getErrorMessage()!=null){
					statusMessage+=" Error message: "+res.getResult().getErrorMessage();
				}
				String error=res.getErrorMessage();
				if(error!=null && error.trim().length()>0){
					statusMessage+=" Error details: "+error;
				}
			}catch(IOException ex){
				LogUtil.logException("Could not read stderr",ex,logger);
			}
			info.setStatus(Status.FAILED, statusMessage);
		}
	}

	@Override
	public void update(XnjsFile xinfo){
		if(xinfo!=null){
			info.setTransferredBytes(xinfo.getSize());
		}
		if(info.getDataSize()<0){
			try{
				String f = isImport() ? info.getTarget() : info.getSource();
				if(!isImport() || Status.DONE==info.getStatus()) {
					TSI tsi=xnjs.getTargetSystemInterface(client, preferredLoginNode);
					tsi.setStorageRoot(workingDirectory);
					info.setDataSize(tsi.getProperties(f).getSize());
				}
			}catch(Exception ex){}
		}
	}

	@Override
	public void setStorageAdapter(IStorageAdapter adapter) {
		this.storageAdapter=adapter;
	}

	@Override
	public void setOverwritePolicy(OverwritePolicy overwrite) {
		this.overwrite=overwrite;
	}

	@Override
	public void setPermissions(String permissions) {
		this.permissions = permissions;
	}

	@Override
	public void setPreferredLoginNode(String loginNode) {
		this.preferredLoginNode = loginNode;
	}

	@Override
	public void setImportPolicy(ImportPolicy policy){
		// NOP
	}

	@Override
	public void abort() {	
		abort=true;
		if(ach!=null){
			try{
				ach.abort();
			}
			catch(Exception ee){
				LogUtil.logException("Can't abort file transfer program", ee ,logger);
			}
		}
	}

	public ResultHolder getResult(){
		return ach!=null ? ach.getResult() : null;
	}

	//copy all data from an input stream to an output stream, while tracking progress via
	//the transferredBytes field
	protected void copyTrackingTransferedBytes(InputStream in, OutputStream out)throws Exception{
		int bufferSize=65536;
		byte[] buffer = new byte[bufferSize];
		int len=0;
		long transferredBytes=0;
		while (!abort) {
			len=in.read(buffer,0,bufferSize);
			if(len<0)break;
			out.write(buffer,0,len);
			transferredBytes+=len;
			info.setTransferredBytes(transferredBytes);
		}
	}

	protected void createParentDirectories()throws Exception{
		String s = getParentOfLocalFilePath(info.getTarget());
		XnjsFile parent = storageAdapter.getProperties(s);
		if(parent==null){
			storageAdapter.mkdir(s);
		}
		else if(!parent.isDirectory()){
			throw new IOException("Parent <"+s+"> is not a directory");
		}
	}

	private String getParentOfLocalFilePath(String file){
		String result = file.replaceAll("/+","/").
				replace("\\/","/").
				replace("/", storageAdapter.getFileSeparator());
		int i=result.lastIndexOf(storageAdapter.getFileSeparator());
		return i>0 ? result.substring(0,i) : "/" ;
	}

	@Override
	public String toString(){
		return getDescription();
	}

	protected String getDescription(){
		StringBuilder sb=new StringBuilder();
		sb.append("Filetransfer ").append(info.getUniqueId());
		sb.append(" '").append(info.getSource()).append("' -> '").append(info.getTarget());
		sb.append("' workdir='").append(workingDirectory).append("'");
		if(client!=null){
			sb.append(" client='").append(client.getDistinguishedName()+"'");
		}
		return sb.toString();
	}

	/**
	 * sets the status to FAILED, and the statusMessage to the failure cause
	 * (created using {@link Log#createFaultMessage(String, Throwable)}
	 * @param message - error message
	 * @param cause - the error cause
	 */
	protected void reportFailure(String message, Throwable cause){
		info.setStatus(Status.FAILED, Log.createFaultMessage(message, cause));
	}

	/**
	 * Computes transfer rates, bytes transferred and
	 * logs it to the USAGE logger at INFO level<br/>
	 * (If needed, Log4j can be configured to send these log messages to a 
	 * specific file instead of the standard logfile)
	 * <p>
	 * The format is:
	 * [clientDN] [Sent|Received] [bytes] [kb/sec] [source] [target] [protocol] [parentJobID]   
	 * <p>
	 * NOTE: if you override the #run() method, you should call this method after your transfer finishes
	 */
	protected void reportUsage(){
		if(Status.DONE!=info.getStatus()){
			return;
		}
		long finishTime=System.currentTimeMillis();
		long dataSize = info.getDataSize();
		long consumedMillis=finishTime-startTime;
		String r=IOUtils.format(consumedMillis>0?(float)dataSize/(float)consumedMillis:0,2);
		String what=isImport()?"received":"sent";
		String dn=client!=null?client.getDistinguishedName():"anonymous";
		usageLogger.info("[{}] [{}] [{}] [{} kB/s] [{}] [{}] [{}] [{}]",
					dn, what, dataSize, r, info.getSource(), info.getTarget(),
					info.getProtocol(), info.getParentActionID());
	}

}
