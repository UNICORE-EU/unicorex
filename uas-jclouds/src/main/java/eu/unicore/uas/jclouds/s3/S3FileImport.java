package eu.unicore.uas.jclouds.s3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import eu.unicore.uas.fts.ProgressListener;
import eu.unicore.uas.impl.sms.SMSBaseImpl;
import eu.unicore.uas.impl.sms.SMSUtils;
import eu.unicore.uas.util.LogUtil;
import eu.unicore.util.Log;
import eu.unicore.util.Pair;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.io.FileSet;
import eu.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.xnjs.io.TransferInfo.Status;
import eu.unicore.xnjs.io.XnjsFile;
import eu.unicore.xnjs.io.XnjsFileWithACL;
import eu.unicore.xnjs.tsi.BatchMode;
import eu.unicore.xnjs.util.IOUtils;

/**
 * File imports from S3
 * 
 * @author schuller
 */
public class S3FileImport extends S3FileTransferBase {

	private XnjsFile remote;

	private FileSet fileSet;

	/**
	 * files to transfer: pairs of remote source and local target
	 */
	protected final List<Pair<XnjsFile, String>> filesToTransfer = new ArrayList<>();

	public S3FileImport(XNJS xnjs){
		super(xnjs);
	}	

	@Override
	public final void run(){
		try{
			setup();
			getRemoteInfo();
			collectFilesToTransfer();
			info.setStatus(Status.RUNNING);
			preTransferSanityChecks();
			runTransfers();
			setFilePermissions();
			info.setStatus(Status.DONE);
			computeMetrics();
		}
		catch(ProgressListener.CancelledException ce){
			cancelled();
		}
		catch(Exception ex){
			String msg = "Error executing filetransfer";
			LogUtil.logException(msg, ex, logger);
			failed(LogUtil.createFaultMessage(msg, ex));
		}
		if(statusTracker!=null)statusTracker.update(info);
		onFinishCleanup();
	}

	/**
	 * collect all the files that need to be transferred, and store them
	 * in {@link #filesToTransfer}
	 * @throws Exception
	 */
	protected void collectFilesToTransfer() throws Exception {
		long dataSize;
		if(remote.isDirectory())
		{
			dataSize = doCollectFiles(filesToTransfer, remote, info.getTarget(), remote.getPath());
		}
		else
		{
			dataSize = remote.getSize();
			filesToTransfer.add(new Pair<>(remote, info.getTarget()));
		}
		info.setDataSize(dataSize);
	}

	/**
	 * perform any sanity checks before starting the actual filetransfers
	 * @throws Exception
	 */
	protected void preTransferSanityChecks() throws Exception {
		XnjsFileWithACL localInfo = getLocalStorage().getProperties(info.getTarget());
		// if we have more than one file to transfer, target must be a directory
		if(filesToTransfer.size()>1){
			if(localInfo!=null && !localInfo.isDirectory()){
				throw new IOException("Target <"+info.getTarget()+"> is not a directory.");
			}
		}
		if(filesToTransfer.size()==1){
			Pair<XnjsFile,String> file = filesToTransfer.get(0);
			createParentDirectories(file.getM2());
			if(localInfo!=null && localInfo.isDirectory()){
				// we must append the filename to the target
				String name = new File(file.getM1().getPath()).getName();
				info.setTarget(info.getTarget()+"/"+name);
				file.setM2(info.getTarget());
			}
		}
	}

	/**
	 * transfer all previously collected files
	 */
	protected void runTransfers() throws Exception {
		for (var pair : filesToTransfer) {
			checkCancelled();
			XnjsFile currentSource = pair.getM1();
			String currentTarget = pair.getM2();
			if(currentSource.isDirectory())
			{
				createParentDirectories(currentTarget+"/file");
				info.setTransferredBytes(info.getTransferredBytes()+1);
			}
			else {
				transferFile(currentSource, currentTarget);
			}
		}
	}

	protected void setFilePermissions() {
		try{
			IStorageAdapter tsi = getLocalStorage();
			boolean supportsBatch = tsi instanceof BatchMode;
			if(supportsBatch){
				((BatchMode)tsi).startBatch();
			}
			try{
				for (Pair<XnjsFile,String> pair : filesToTransfer) {
					XnjsFile currentSource = pair.getM1();
					String currentTarget = pair.getM2();
					copyPermissions(currentSource, currentTarget);
					if(supportsBatch){
						((BatchMode)tsi).commitBatch();
					}
				}
			}finally{
				if(supportsBatch){
					((BatchMode)tsi).cleanupBatch();
				}
			}
		}
		catch(Exception ex){
			String msg="Error setting file permissions: "+ex.getMessage();
			LogUtil.logException(msg,ex,logger);
		}
	}

	/**
	 * sets up security and creates the SMS client
	 */
	protected void setup() throws Exception{
		if(OverwritePolicy.RESUME_FAILED_TRANSFER.equals(overwrite)){
			throw new Exception("Resuming a transfer is not supported!");
		}
		checkCancelled();
		initSecurityProperties();
		s3Adapter = createS3Adapter();
		startTime=System.currentTimeMillis();
	}

	/**
	 * retrieve information about the remote source
	 */
	protected void getRemoteInfo() throws Exception {
		String source = info.getSource();
		if(!FileSet.hasWildcards(source)){
			remote = s3Adapter.getProperties(source);
			boolean dir = remote.isDirectory();
			if(dir){
				fileSet = new FileSet(source,true);
			}
			else{
				fileSet = new FileSet(source);
			}
		}
		else{
			// have wildcards
			fileSet = new FileSet(source);
			remote = s3Adapter.getProperties(fileSet.getBase());
		}
		if(remote == null){
			throw new FileNotFoundException("No files found for: "+source);
		}
	}

	/**
	 * Import a single file.<br/> 
	 * Will check if the remote file system is in fact the local one, and
	 * will invoke the transferFileByCopy() in that case
	 * @param source
	 * @param target
	 * @throws Exception
	 */
	protected void transferFile(XnjsFile source, String target) throws Exception
	{
		checkCancelled();
		//handle directory as target
		String localFile = target;
		IStorageAdapter adapter = getLocalStorage();
		XnjsFile xFile=adapter.getProperties(localFile);
		if(xFile!=null){
			if(xFile.isDirectory()){
				String name=getFileName(source.getPath());
				if(!target.endsWith(adapter.getFileSeparator())){
					localFile=target+adapter.getFileSeparator()+name;	
				}
				else localFile=target+name;
			}
			// file exists, check whether we are allowed to overwrite
			else if(OverwritePolicy.DONT_OVERWRITE.equals(overwrite)){
				// nothing to do, file exists and we are not overwriting it
				return;
			}
			if(!xFile.getPermissions().isWritable()){
				throw new IOException("Local target file is not writeable");
			}
		}
		checkCancelled();
		boolean append=OverwritePolicy.APPEND.equals(overwrite);
		try(OutputStream os = getLocalStorage().getOutputStream(localFile, append);
				InputStream is = s3Adapter.getInputStream(source.getPath())){
			checkCancelled();
			copy(is, os);
		}
		if(statusTracker!=null)statusTracker.update(info);
	}

	/**
	 * Recursively gather all files and directories that need to be copied
	 * @return total file size, including one extra byte for each directory
	 */
	protected long doCollectFiles(List<Pair<XnjsFile, String>> collection, XnjsFile sourceFolder, 
			String targetFolder, String baseDirectory) throws Exception
	{
		long result = 1;
		collection.add(new Pair<>(sourceFolder, targetFolder));
		for (XnjsFile child : s3Adapter.ls(sourceFolder.getPath(),0, SMSBaseImpl.MAX_LS_RESULTS, false)) {
			String relative = IOUtils.getRelativePath(child.getPath(), sourceFolder.getPath());
			String target = targetFolder+relative;
			if(child.isDirectory() && fileSet.isRecurse())
			{
				result += doCollectFiles(collection, child, target, baseDirectory);
			}
			else 
			{
				if(remote.isDirectory() && fileSet.matches(child.getPath())){
					collection.add(new Pair<>(child,target));
					result += child.getSize();
				}
			}
		}
		return result;
	}

	protected void copyPermissions(XnjsFile remote, String localFile) {
		try{
			String perm = permissions!=null ? permissions : remote.getPermissions().toString();
			getLocalStorage().chmod2(localFile, SMSUtils.getChangePermissions(perm), false);
		}catch(Exception ex) {
			Log.logException("Can't set permissions on local file <"+localFile+">", ex, logger);
		}
	}

}