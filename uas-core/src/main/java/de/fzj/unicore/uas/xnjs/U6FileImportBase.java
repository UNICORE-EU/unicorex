package de.fzj.unicore.uas.xnjs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.unigrids.services.atomic.types.GridFileType;
import org.unigrids.services.atomic.types.PermissionsType;
import org.unigrids.services.atomic.types.ProtocolType;
import org.unigrids.x2006.x04.services.sms.ExportFileDocument;
import org.unigrids.x2006.x04.services.sms.ExportFileResponseDocument;

import de.fzj.unicore.uas.UASProperties;
import de.fzj.unicore.uas.client.StorageClient;
import de.fzj.unicore.uas.fts.FiletransferOptions.IMonitorable;
import de.fzj.unicore.uas.fts.ProgressListener;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.uas.util.Pair;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.io.FileSet;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import de.fzj.unicore.xnjs.io.Permissions;
import de.fzj.unicore.xnjs.io.TransferInfo.Status;
import de.fzj.unicore.xnjs.io.XnjsFile;
import de.fzj.unicore.xnjs.io.XnjsFileWithACL;
import de.fzj.unicore.xnjs.tsi.BatchMode;
import de.fzj.unicore.xnjs.util.IOUtils;
import eu.unicore.util.Log;

/**
 * Base class for UNICORE file imports.<br/>
 * 
 * Sub classes must provide an appropriate client class and 
 * the protocol that is used.
 *  
 * @author demuth
 * @author schuller
 */
public abstract class U6FileImportBase extends U6FileTransferBase {

	private GridFileType remote;

	private FileSet fileSet;

	/**
	 * Storage client used to access the remote storage
	 */
	protected StorageClient sms;

	/**
	 * files to transfer: pairs of remote source and local target
	 */
	protected final List<Pair<GridFileType,String>> filesToTransfer = new ArrayList<Pair<GridFileType,String>>();

	public U6FileImportBase(XNJS configuration){
		super(configuration);
	}

	public final void run(){
		try{
			setup();
			getRemoteInfo();
			collectFilesToTransfer();
			preTransferSanityChecks();
			info.setStatus(Status.RUNNING);
			runTransfers();
			setPermissions();
			info.setStatus(Status.DONE);
			computeMetrics();
		}
		catch(ProgressListener.CancelledException ce){
			cancelled();
		}
		catch(Exception ex){
			String msg="Error executing filetransfer";
			LogUtil.logException(msg,ex,logger);
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
		if(remote.getIsDirectory())
		{
			dataSize = doCollectFiles(filesToTransfer, remote, info.getTarget(), remote.getPath());
		}
		else
		{
			dataSize=remote.getSize();
			filesToTransfer.add(new Pair<GridFileType,String>(remote, info.getTarget()));
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
			Pair<GridFileType,String> file = filesToTransfer.get(0);
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
		for (Pair<GridFileType,String> pair : filesToTransfer) {
			checkCancelled();
			GridFileType currentSource = pair.getM1();
			String currentTarget = pair.getM2();
			if(currentSource.getIsDirectory())
			{
				createParentDirectories(currentTarget+"/file");
				info.setTransferredBytes(info.getTransferredBytes()+1);
			}
			else {
				transferFile(currentSource, currentTarget);
			}
		}
	}

	/**
	 * transfer all previously collected files
	 */
	protected void setPermissions() {
		try{
			IStorageAdapter tsi=getStorageAdapter();
			boolean supportsBatch=tsi instanceof BatchMode;

			if(supportsBatch){
				((BatchMode)tsi).startBatch();
			}
			try{
				for (Pair<GridFileType,String> pair : filesToTransfer) {
					GridFileType currentSource = pair.getM1();
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
		sms=new StorageClient(smsEPR,sec);
		startTime=System.currentTimeMillis();
	}

	protected boolean supportsResume(){
		return false;
	}
	
	/**
	 * retrieve information about the remote source
	 */
	protected void getRemoteInfo() throws Exception {
		String source = info.getSource();
		if(!FileSet.hasWildcards(source)){
			remote = sms.listProperties(source);
			boolean dir = remote.getIsDirectory();
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
			remote = sms.listProperties(fileSet.getBase());
		}
		if(remote == null){
			throw new FileNotFoundException("No files found for: "+source);
		}
	}

	@Override
	protected void onFinishCleanup(){
		super.onFinishCleanup();
		remote=null;
		sms=null;
	}

	/**
	 * Import a single file.<br/> 
	 * Will check if the remote file system is in fact the local one, and
	 * will invoke the transferFileByCopy() in that case
	 * @param source
	 * @param target
	 * @throws Exception
	 */
	protected void transferFile(GridFileType source, String target) throws Exception
	{
		checkCancelled();
		//handle directory as target
		String localFile = target;
		IStorageAdapter adapter=getStorageAdapter();
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

		if(checkPossibilityForLocalCopy()){
			transferFileLocal(source, sms, target);
		}
		else{
			transferFileFromRemote(source, sms, localFile);
		}
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
		String mountPoint=sms.getFileSystem().getMountPoint();
		String remoteFSID=sms.getFileSystem().getDescription();
		String localFSID=getStorageAdapter().getFileSystemIdentifier();
		UASProperties conf = kernel.getAttribute(UASProperties.class);

		return mountPoint!=null &&
				OverwritePolicy.APPEND!=overwrite &&
				remoteFSID!=null && 
				localFSID!=null && 
				remoteFSID.equals(localFSID) && 
				importPolicy != ImportPolicy.FORCE_COPY &&
				!conf.getBooleanValue(UASProperties.SMS_TRANSFER_FORCEREMOTE);
	}


	protected void transferFileLocal(GridFileType source, StorageClient sms, String target)throws Exception{
		String base=sms.getFileSystem().getMountPoint();
		String src=base+source.getPath();
		IStorageAdapter s=getStorageAdapter();
		String sRoot=s.getStorageRoot();
		try{
			s.setStorageRoot("/");
			String tgt=workdir+"/"+target;
			checkOverwriteAllowed(s, tgt);
			boolean linked = false;
			if(importPolicy == ImportPolicy.PREFER_LINK){
				logger.info("Optimization enabled: importing file by symlinking <"+src+"> to <"+target+">");
				try{
					s.link(src, tgt);
					linked = true;
				}catch(Exception ex){
					Log.logException("Could not symlink, will try copy!",ex,logger);
					linked=false;
				}
			}
			if(!linked){
				logger.info("Optimization enabled: importing file by local copy of <"+src+"> to <"+target+">");
				s.cp(src, tgt);	
			}
			info.setTransferredBytes(info.getTransferredBytes()+source.getSize());
		}
		finally{
			s.setStorageRoot(sRoot);
		}
	}


	protected void transferFileFromRemote(GridFileType source, StorageClient sms, String localFile)throws Exception{
		checkCancelled();
		createNewExport(source, sms);
		if(ftc instanceof IMonitorable){
			((IMonitorable)ftc).setProgressListener(this);
		}
		doRun(localFile);
	}

	protected void createNewExport(GridFileType source, StorageClient sms)throws Exception{
		ExportFileDocument req=ExportFileDocument.Factory.newInstance();
		req.addNewExportFile().setSource(source.getPath());
		req.getExportFile().setProtocol(ProtocolType.Enum.forString(info.getProtocol()));
		Map<String,String>ep=getExtraParameters();
		if(ep!=null && ep.size()>0){
			req.getExportFile().setExtraParameters(convert(ep));
		}
		ExportFileResponseDocument res=sms.ExportFile(req);
		fileTransferInstanceEpr=res.getExportFileResponse().getExportEPR();
		ftc=getFTClient();
	}

	/**
	 * Recursively gather all files and directories that need to be copied
	 * @return total file size, including one extra byte for each directory
	 */
	protected long doCollectFiles(List<Pair<GridFileType,String>> collection, GridFileType sourceFolder, 
			String targetFolder, String baseDirectory) throws Exception
	{
		long result = 1;
		collection.add(new Pair<GridFileType,String>(sourceFolder, targetFolder));
		for (GridFileType child : sms.listDirectory(sourceFolder.getPath())) {
			String relative = IOUtils.getRelativePath(child.getPath(), sourceFolder.getPath());
			String target = targetFolder+relative;
			if(child.getIsDirectory() && fileSet.isRecurse())
			{
				result += doCollectFiles(collection, child, target, baseDirectory);
			}
			else 
			{
				if(remote.getIsDirectory() && fileSet.matches(child.getPath())){
					collection.add(new Pair<GridFileType,String>(child,target));
					result+= child.getSize();
				}
			}
		}
		return result;
	}

	protected void doRun(String localFile)throws Exception{
		OutputStream os=null;
		boolean append=OverwritePolicy.APPEND.equals(overwrite);
		os=getStorageAdapter().getOutputStream(localFile, append);
		try{
			checkCancelled();
			if(ftc instanceof IMonitorable){
				((IMonitorable)ftc).setProgressListener(this);
			}
			ftc.readAllData(os);
		}finally{
			if(os!=null)os.close();
		}
	}	

	protected void copyPermissions(GridFileType remote, String localFile)throws Exception{
		PermissionsType p=remote.getPermissions();
		Permissions perm=XNJSFacade.getXNJSPermissions(p);
		getStorageAdapter().chmod(localFile, perm);
		if(logger.isDebugEnabled()){
			logger.debug("Copied remote permissions to :"+perm);
		}
	}

}
