package eu.unicore.uas.xnjs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import eu.unicore.client.core.FileList.FileListEntry;
import eu.unicore.client.core.StorageClient;
import eu.unicore.uas.UASProperties;
import eu.unicore.uas.fts.FiletransferOptions;
import eu.unicore.uas.fts.FiletransferOptions.IMonitorable;
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
 * Base class for UNICORE RESTful file imports with support for BFT transfer
 * 
 * @author schuller
 */
public class RESTFileImportBase extends RESTFileTransferBase {

	private FileListEntry remote;

	private FileSet fileSet;

	/**
	 * Storage client used to access the remote storage
	 */
	protected StorageClient storage;

	private String permissions = null;

	/**
	 * files to transfer: pairs of remote source and local target
	 */
	protected final List<Pair<FileListEntry,String>> filesToTransfer = new ArrayList<>();

	public RESTFileImportBase(XNJS configuration){
		super(configuration);
	}	

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
		destroyFileTransferResource();
		onFinishCleanup();
	}

	/**
	 * collect all the files that need to be transferred, and store them
	 * in {@link #filesToTransfer}
	 * @throws Exception
	 */
	protected void collectFilesToTransfer() throws Exception {
		long dataSize;
		if(remote.isDirectory)
		{
			dataSize = doCollectFiles(filesToTransfer, remote, info.getTarget(), remote.path);
		}
		else
		{
			dataSize = remote.size;
			filesToTransfer.add(new Pair<FileListEntry,String>(remote, info.getTarget()));
		}
		info.setDataSize(dataSize);
	}

	/**
	 * perform any sanity checks before starting the actual filetransfers
	 * @throws Exception
	 */
	protected void preTransferSanityChecks() throws Exception {
		XnjsFileWithACL localInfo=getStorageAdapter().getProperties(info.getTarget());
		// if we have more than one file to transfer, target must be a directory
		if(filesToTransfer.size()>1){
			if(localInfo!=null && !localInfo.isDirectory()){
				throw new IOException("Target <"+info.getTarget()+"> is not a directory.");
			}
		}

		if(filesToTransfer.size()==1){
			Pair<FileListEntry,String> file = filesToTransfer.get(0);
			createParentDirectories(file.getM2());
			if(localInfo!=null && localInfo.isDirectory()){
				// we must append the filename to the target
				String name = new File(file.getM1().path).getName();
				info.setTarget(info.getTarget()+"/"+name);
				file.setM2(info.getTarget());
			}
		}
	}

	/**
	 * transfer all previously collected files
	 */
	protected void runTransfers() throws Exception {
		for (Pair<FileListEntry,String> pair : filesToTransfer) {
			checkCancelled();
			FileListEntry currentSource = pair.getM1();
			String currentTarget = pair.getM2();
			if(currentSource.isDirectory)
			{
				createParentDirectories(currentTarget+"/file");
				info.setTransferredBytes(info.getTransferredBytes()+1);
			}
			else {
				transferFile(currentSource, currentTarget);
			}
		}
	}

	@Override
	public void setPermissions(String permissions) {
		this.permissions = permissions;
	}

	protected void setFilePermissions() {
		try{
			IStorageAdapter tsi=getStorageAdapter();
			boolean supportsBatch=tsi instanceof BatchMode;

			if(supportsBatch){
				((BatchMode)tsi).startBatch();
			}
			try{
				for (Pair<FileListEntry,String> pair : filesToTransfer) {
					FileListEntry currentSource = pair.getM1();
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
		if(OverwritePolicy.RESUME_FAILED_TRANSFER.equals(overwrite)
				&&!supportsResume()){
			throw new Exception("Resuming a transfer is not supported!");
		}
		checkCancelled();
		initSecurityProperties();
		storage = new StorageClient(storageEndpoint, sec, auth);
		startTime=System.currentTimeMillis();
	}

	protected boolean supportsResume(){
		return false;
	}
	
	/**
	 * retrieve information about the remote source
	 * 
	 * @throws Exception
	 */
	protected void getRemoteInfo() throws Exception {
		String source = info.getSource();
		if(!FileSet.hasWildcards(source)){
			remote = storage.stat(source);
			boolean dir = remote.isDirectory;
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
			remote = storage.stat(fileSet.getBase());
		}
		if(remote == null){
			throw new FileNotFoundException("No files found for: "+source);
		}
	}

	@Override
	protected void onFinishCleanup(){
		super.onFinishCleanup();
		remote = null;
		storage = null;
	}

	/**
	 * Import a single file.<br/> 
	 * Will check if the remote file system is in fact the local one, and
	 * will invoke the transferFileByCopy() in that case
	 * @param source
	 * @param target
	 * @throws Exception
	 */
	protected void transferFile(FileListEntry source, String target) throws Exception
	{
		checkCancelled();
		//handle directory as target
		String localFile = target;
		IStorageAdapter adapter=getStorageAdapter();
		XnjsFile xFile=adapter.getProperties(localFile);

		if(xFile!=null){
			if(xFile.isDirectory()){
				String name=getFileName(source.path);
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

		if(checkPossibilityForLocalCopy()){
			transferFileLocal(source, target);
		}
		else{
			transferFileFromRemote(source, localFile);
		}
		if(statusTracker!=null)statusTracker.update(info);
	}

	/**
	 * checks if the remote file transfer can be replaced by a local copy. 
	 * This is usually possible if
	 * <ul>
	 * <li>the "mount point" of the remote SMS is defined</li>
	 * <li>the writing mode is NOT set to append</li>
	 * <li>the file system IDs of local and remote storage are non-null and match</li>
	 * <li>the transfer is not "forced" to be remote via configuration</li>
	 * <li>the transfer is not "forced" to be remote via user request</li>
	 * </ul>
	 * @return <code>true</code> if local cp is possible
	 */
	protected boolean checkPossibilityForLocalCopy()throws Exception{
		String mountPoint = storage.getProperties().getString("mountPoint");
		String remoteFSID = storage.getProperties().optString("filesystemDescription", null);
		String localFSID = getStorageAdapter().getFileSystemIdentifier();
		UASProperties conf = kernel.getAttribute(UASProperties.class);

		return mountPoint!=null &&
				OverwritePolicy.APPEND!=overwrite &&
				remoteFSID!=null && 
				localFSID!=null && 
				remoteFSID.equals(localFSID) && 
				importPolicy != ImportPolicy.FORCE_COPY &&
				!conf.getBooleanValue(UASProperties.SMS_TRANSFER_FORCEREMOTE);
	}


	protected void transferFileLocal(FileListEntry source, String target)throws Exception{
		String base = storage.getMountPoint();
		String src=base+source.path;
		IStorageAdapter s=getStorageAdapter();
		String sRoot=s.getStorageRoot();
		try{
			s.setStorageRoot("/");
			String tgt=workdir+"/"+target;
			checkOverwriteAllowed(s, tgt);
			boolean linked = false;
			if(importPolicy == ImportPolicy.PREFER_LINK){
				logger.info("Optimization enabled: importing file by symlinking <{}> to <{}>", src, target);
				try{
					s.link(src, tgt);
					linked = true;
				}catch(Exception ex){
					Log.logException("Could not symlink, will try copy!",ex,logger);
					linked=false;
				}
			}
			if(!linked){
				logger.info("Optimization enabled: importing file by local copy of <{}> to <{}>", src, target);
				s.cp(src, tgt);	
			}
			info.setTransferredBytes(info.getTransferredBytes()+source.size);
		}
		finally{
			s.setStorageRoot(sRoot);
		}
	}


	protected void transferFileFromRemote(FileListEntry source, String localFile)throws Exception{
		checkCancelled();
		createNewExport(source);
		if(ftc instanceof IMonitorable){
			((IMonitorable)ftc).setProgressListener(this);
		}
		doRun(localFile);
	}

	protected void createNewExport(FileListEntry source)throws Exception{
		ftc = storage.createExport(source.path, info.getProtocol(), new HashMap<String, String>());
	}

	/**
	 * Recursively gather all files and directories that need to be copied
	 * @return total file size, including one extra byte for each directory
	 */
	protected long doCollectFiles(List<Pair<FileListEntry,String>> collection, FileListEntry sourceFolder, 
			String targetFolder, String baseDirectory) throws Exception
	{
		long result = 1;
		collection.add(new Pair<FileListEntry,String>(sourceFolder, targetFolder));
		for (FileListEntry child : storage.ls(sourceFolder.path).list(0, SMSBaseImpl.MAX_LS_RESULTS)) {
			String relative = IOUtils.getRelativePath(child.path, sourceFolder.path);
			String target = targetFolder+relative;
			if(child.isDirectory && fileSet.isRecurse())
			{
				result += doCollectFiles(collection, child, target, baseDirectory);
			}
			else 
			{
				if(remote.isDirectory && fileSet.matches(child.path)){
					collection.add(new Pair<FileListEntry,String>(child,target));
					result += child.size;
				}
			}
		}
		return result;
	}

	protected void doRun(String localFile)throws Exception{
		boolean append=OverwritePolicy.APPEND.equals(overwrite);
		try(OutputStream os = getStorageAdapter().getOutputStream(localFile, append)){
			checkCancelled();
			if(ftc instanceof IMonitorable){
				((IMonitorable)ftc).setProgressListener(this);
			}
			((FiletransferOptions.Read)ftc).readFully(os);
		}
	}	

	protected void copyPermissions(FileListEntry remote, String localFile) {
		try{
			String perm = permissions!=null ? permissions : remote.permissions;
			getStorageAdapter().chmod2(localFile, SMSUtils.getChangePermissions(perm), false);
		}catch(Exception ex) {
			Log.logException("Can't set permissions on local file <"+localFile+">", ex, logger);
		}
	}

}