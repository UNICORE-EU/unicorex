package de.fzj.unicore.uas.xtreemfs;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.unigrids.services.atomic.types.ProtocolType;

import de.fzj.unicore.uas.client.FileTransferClient;
import de.fzj.unicore.uas.client.StorageClient;
import de.fzj.unicore.uas.fts.ProgressListener;
import de.fzj.unicore.uas.impl.sms.SMSUtils;
import de.fzj.unicore.uas.util.Pair;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.io.TransferInfo.Status;
import de.fzj.unicore.xnjs.io.XnjsFile;
import de.fzj.unicore.xnjs.tsi.TSI;
import eu.unicore.security.Client;
import eu.unicore.util.Log;

/**
 * upload data to XtreemFS
 * 
 * @author schuller
 */
public class XtreemFSUpload extends XtreemFSTransferBase{

	private static final Logger logger=Log.getLogger(Log.SERVICES+".xtreemfs", XtreemFSUpload.class);
	
	private final String workdir;
	private final String source;
	private final URI target;
	
	public XtreemFSUpload(XNJS configuration, Client client, String workdir, String source, URI target){
		super(configuration,client);
		this.workdir=workdir;
		this.target=target;
		this.source=source;
		info.setTarget(target.toString());
		info.setSource(source);
	}

	@Override
	public void run() {
		info.setStatus(Status.RUNNING);
		try{
			String baseDir=configuration.getProperty(XTREEMFS_LOCAL_MOUNT);
			if(baseDir==null){
				baseDir=xtreemProperties.getValue(XtreemProperties.XTREEMFS_LOCAL_MOUNT);
			}
			if(baseDir!=null){
				exportLocally(baseDir);
			}
			else{
				uploadToRemoteSMS();
			}
			info.setStatus(Status.DONE);
		}catch(Exception ex){
			info.setStatus(Status.FAILED,Log.createFaultMessage("File export failed.", ex));
		}
	}

	protected void exportLocally(String baseDir)throws Exception{
		if(baseDir==null)throw new IllegalStateException("No local XtreemFS mountpoint defined.");
		String realTarget=baseDir+SMSUtils.urlDecode(makeTarget());
		String realSource=workdir+"/"+source;
		ensureDirectoriesExist(realTarget);
		//just copy file to remote ...
		TSI tsi=configuration.getTargetSystemInterface(client);
		tsi.cp(realSource, realTarget);
		logger.info("Copied: "+realSource+" to "+realTarget);
	}
	
	String makeTarget(){
		return target.getRawSchemeSpecificPart();
	}


	protected void uploadToRemoteSMS()throws Exception{
		StorageClient sms=createStorageClient();
		logger.info("Uploading to remote SMS "+sms.getUrl());
		String remoteBase = makeTarget();
		if(isDirectory(source)){
			transferFolder(source, sms, remoteBase);
		}
		else{
			transferFile(source, sms, remoteBase);
		}
	}
	
	protected void transferFolder(String source, StorageClient sms, String target) throws Exception
	{
		checkCancelled();
		List<Pair<String,String>> collection = new ArrayList<Pair<String,String>>();
		info.setDataSize(collectFiles(collection, source, sms, target));

		for (Pair<String,String> pair : collection) {

			String currentSource = pair.getM1();
			String currentTarget = pair.getM2();
			if(isDirectory(currentSource))
			{
				sms.createDirectory(currentTarget);
				info.setTransferredBytes(info.getTransferredBytes()+1);
			}
			else transferFile(currentSource, sms, currentTarget);
		}
	}
	
	protected boolean isDirectory(String file) throws ExecutionException
	{
		XnjsFile f=getStorageAdapter().getProperties(file);
		if(f==null)throw new ExecutionException("The file <"+file+"> does not exist or can not be accessed.");
		return f.isDirectory();
	}
	
	protected void transferFile(String localFile, StorageClient sms, String remoteFile)throws Exception {
		checkCancelled();
		FileTransferClient ftc=sms.getImport(remoteFile, ProtocolType.BFT);
		TSI tsi=getStorageAdapter();
		InputStream is=tsi.getInputStream(localFile);
		try{
			ftc.writeAllData(is);
		}
		finally{
			try{
				ftc.destroy();
			}catch(Exception ex){}
			try{
				is.close();
			}catch(Exception ex){}
		}
	}
	
	/**
	 * Recursively gather all files and directories that need to be copied
	 */
	protected long collectFiles(List<Pair<String,String>> collection, String sourceFolder, StorageClient sms, String targetFolder) throws Exception
	{
		long result = 1;
		collection.add(new Pair<String,String>(sourceFolder, targetFolder));
		for (XnjsFile child : getStorageAdapter().ls(sourceFolder)) {
			int relativeIndex=sourceFolder.lastIndexOf("/");
			if(relativeIndex<0)relativeIndex=0;
			String relative = child.getPath().substring(relativeIndex);
			String target = targetFolder+"/"+relative;

			if(child.isDirectory())
			{
				result += collectFiles(collection, child.getPath(), sms, target);
			}
			else 
			{
				collection.add(new Pair<String,String>(child.getPath(),target));
				result+= child.getSize();
			}
		}
		return result;
	}
	
	private TSI storageAdapter;
	
	protected TSI getStorageAdapter(){
		if(storageAdapter==null){
			storageAdapter=configuration.getTargetSystemInterface(client);
			storageAdapter.setStorageRoot(workdir);
		}
		return storageAdapter;
	}

	/**
	 * if the transfer has been cancelled, this method will 
	 * throw a CancelledException
	 */
	public void checkCancelled()throws ProgressListener.CancelledException{
		if(aborted)throw new ProgressListener.CancelledException();
	}
}
