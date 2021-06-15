package de.fzj.unicore.uas.xnjs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.unigrids.services.atomic.types.GridFileType;
import org.unigrids.services.atomic.types.ProtocolType;
import org.unigrids.x2006.x04.services.sms.ImportFileDocument;
import org.unigrids.x2006.x04.services.sms.ImportFileResponseDocument;

import de.fzj.unicore.uas.UASProperties;
import de.fzj.unicore.uas.client.StorageClient;
import de.fzj.unicore.uas.fts.FiletransferOptions.IMonitorable;
import de.fzj.unicore.uas.fts.ProgressListener;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.uas.util.Pair;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.io.FileSet;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import de.fzj.unicore.xnjs.io.Permissions;
import de.fzj.unicore.xnjs.io.TransferInfo.Status;
import de.fzj.unicore.xnjs.io.XnjsFile;

/**
 * Base class for UNICORE file exports<br/>
 * Sub classes need only provide an appropriate client class and 
 * the protocol that is used.
 *  
 * @author demuth
 * @author schuller
 */
public abstract class U6FileExportBase extends U6FileTransferBase {

	/**
	 * storage client for accessing the remote storage
	 */
	protected StorageClient sms;

	/**
	 * files to transfer: stores pairs of (local_source + size) and remote filename
	 */
	protected final List<Pair<Pair<String,Long>,String>> filesToTransfer = new ArrayList<Pair<Pair<String,Long>,String>>();

	private FileSet fileSet;

	public U6FileExportBase(XNJS configuration){
		super(configuration);
	}

	public final void run(){
		try{
			setup();
			collectFilesToTransfer();
			info.setStatus(Status.RUNNING);
			runTransfers();
			info.setStatus(Status.DONE);
			computeMetrics();
		}catch(ProgressListener.CancelledException ce){
			cancelled();
		}
		catch(Exception ex){
			String msg="Error executing filetransfer";
			LogUtil.logException(msg,ex,logger);
			failed(LogUtil.createFaultMessage(msg, ex));
		}
		destroyFileTransferResource();
		onFinishCleanup();
	}

	/**
	 * sets up security, creates the SMS client and retrieves the
	 * info about the local file/directory
	 */
	protected void setup() throws Exception {
		checkCancelled();

		initSecurityProperties();

		sms=new StorageClient(smsEPR,sec);
		info.setSource(cleanLocalFilePath(info.getSource()));

		startTime=System.currentTimeMillis();
	}

	@Override
	protected void onFinishCleanup(){
		super.onFinishCleanup();
		sms=null;
	}

	/**
	 * collect all the files that need to be transferred, and store them
	 * in {@link #filesToTransfer}
	 * @throws Exception
	 */
	protected void collectFilesToTransfer() throws Exception {
		String source = info.getSource();
		String target = info.getTarget();
		if(!FileSet.hasWildcards(source)){
			boolean dir = isDirectory(source);
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
		}
		long dataSize;
		if(fileSet.isMultifile())
		{
			dataSize = doCollectFiles(filesToTransfer, fileSet.getBase(), sms, target);
		}
		else
		{
			dataSize=computeFileSize(source);
			Pair<String,Long>s=new Pair<String,Long>(source,dataSize);
			filesToTransfer.add(new Pair<Pair<String,Long>,String>(s,target));
		}
		info.setDataSize(dataSize);
	}

	/**
	 * transfer all files that have been previously collected
	 */
	protected void runTransfers() throws Exception {
		for (Pair<Pair<String,Long>,String> pair : filesToTransfer) {
			checkCancelled();
			Pair<String,Long>sourceDesc=pair.getM1();
			String currentSource = sourceDesc.getM1();
			String currentTarget = pair.getM2();
			if(isDirectory(currentSource))
			{
				checkOverwriteAllowed(sms,currentTarget);
				sms.createDirectory(currentTarget);
				info.setTransferredBytes(info.getTransferredBytes()+1);
			}
			else transferFile(sourceDesc, currentTarget);
		}
	}

	/**
	 * check if we can replace the remote file transfer by a local copy
	 */
	protected boolean checkPossibilityForLocalCopy()throws Exception {
		String remoteFSID=sms.getFileSystem().getDescription();
		String localFSID=getStorageAdapter().getFileSystemIdentifier();
		UASProperties conf = kernel.getAttribute(UASProperties.class);
		return remoteFSID!=null && 
				localFSID!=null && 
				remoteFSID.equals(localFSID) && 
				!conf.getBooleanValue(UASProperties.SMS_TRANSFER_FORCEREMOTE);
	}

	protected void transferFile(Pair<String,Long>sourceDesc, String target) throws Exception
	{
		checkCancelled();
		String source=sourceDesc.getM1();
		if(checkPossibilityForLocalCopy()){
			transferFileLocal(sourceDesc, target);
		}
		else{
			transferFileFromRemote(sourceDesc, target);
		}
		copyPermissions(source, target);
	}

	protected void transferFileLocal(Pair<String,Long> sourceDesc, String target) throws Exception
	{
		checkCancelled();
		String base=sms.getFileSystem().getMountPoint();
		String targetUmask = sms.getResourcePropertiesDocument().getStorageProperties().getUmask();
		//if targetUmask is null now then fine - default will be used.

		if(base==null || OverwritePolicy.APPEND==overwrite){
			transferFileFromRemote(sourceDesc, target);
		}
		else{
			String tgt=base+"/"+target;
			String src=workdir+"/"+sourceDesc.getM1();
			IStorageAdapter s=getStorageAdapter();
			String sRoot=s.getStorageRoot();
			String sUmask=s.getUmask();
			try{
				s.setStorageRoot("/");
				s.setUmask(targetUmask);
				checkOverwriteAllowed(s,tgt);
				createParentDirectories(tgt);
				logger.info("Optimization enabled: exporting file by local copy of <"+src+"> to <"+tgt+">");
				s.cp(src, tgt);
				info.setTransferredBytes(info.getTransferredBytes()+s.getProperties(src).getSize());
			}finally{
				s.setStorageRoot(sRoot);
				s.setUmask(sUmask);
			}
		}
	}

	protected void transferFileFromRemote(Pair<String,Long> sourceDesc, String target) throws Exception
	{
		checkCancelled();
		checkOverwriteAllowed(sms,target);
		ImportFileDocument req=ImportFileDocument.Factory.newInstance();
		req.addNewImportFile().setDestination(target);
		req.getImportFile().setProtocol(ProtocolType.Enum.forString(info.getProtocol()));
		boolean append=OverwritePolicy.APPEND.equals(overwrite);
		Map<String,String>ep=getExtraParameters();
		if(ep!=null && ep.size()>0){
			req.getImportFile().setExtraParameters(convert(ep));
		}
		req.getImportFile().setOverwrite(!append);
		ImportFileResponseDocument res=sms.ImportFile(req);
		fileTransferInstanceEpr=res.getImportFileResponse().getImportEPR();	
		String localFile = sourceDesc.getM1();
		ftc=getFTClient();
		ftc.setAppend(append);
		if(ftc instanceof IMonitorable){
			((IMonitorable)ftc).setProgressListener(this);
		}
		doRun(localFile);
	}

	/**
	 * checks if we may overwrite the given file (in case it exists)
	 * 
	 * @param fileName -  the file to check
	 * @throws IOException - if the file exists and DONT_OVERWRITE is requested
	 */
	protected void checkOverwriteAllowed(StorageClient sms, String fileName)throws Exception{
		if(OverwritePolicy.DONT_OVERWRITE.equals(overwrite)){
			GridFileType g=null;
			try{
				//will throw IOexception if file does not exist
				g=sms.listProperties(fileName);
			}catch(IOException ioe){}
			if(g!=null){
				throw new IOException("File <"+g.getPath()+"> exists, and we have been asked to not overwrite.");
			}
		}
	}

	/**
	 * Recursively gather all files and directories that need to be copied
	 */
	protected long doCollectFiles(List<Pair<Pair<String,Long>,String>> collection, String sourceFolder, StorageClient sms, String targetFolder) throws Exception
	{
		long result = 1;
		collection.add(new Pair<Pair<String,Long>,String>(new Pair<String,Long>(sourceFolder,1l), targetFolder));
		for (XnjsFile child : getStorageAdapter().ls(sourceFolder)) {
			String path = child.getPath();
			String relative = new File(path).getName();
			String target = targetFolder+"/"+relative;

			if(child.isDirectory() && fileSet.isRecurse())
			{
				result += doCollectFiles(collection, path, sms, target);
			}
			else 
			{
				if(fileSet.matches(child.getPath())){
					Pair<String,Long> p = new Pair<String,Long>(path,child.getSize());
					collection.add(new Pair<Pair<String,Long> ,String>(p,target));
					result+= child.getSize();
				}
			}
		}
		return result;
	}

	/**
	 * @param file - the file on the target system to check
	 * @throws IOException if the file does not exist
	 * @throws ExecutionException if the file properties can not be checked
	 */
	protected synchronized long computeFileSize(String file)throws ExecutionException,IOException {
		XnjsFile f=getStorageAdapter().getProperties(file);
		if(f!=null){
			return f.getSize();
		}
		else throw new IOException("The file <"+file+"> does not exist or can not be accessed.");
	}

	protected boolean isDirectory(String file) throws ExecutionException
	{
		XnjsFile f=getStorageAdapter().getProperties(file);
		if(f==null)throw new ExecutionException("The file <"+file+"> does not exist or can not be accessed.");
		return f.isDirectory();
	}

	protected void doRun(String localFile) throws Exception {
		InputStream is=getInputStream(localFile);
		try{
			checkCancelled();
			ftc.writeAllData(is);
		}
		finally{
			is.close();
		}
	}


	protected InputStream getInputStream(String localFile)throws ExecutionException, IOException{
		return getStorageAdapter().getInputStream(localFile);
	}

	/**
	 * copies the permissions of the local file to the remote file
	 * @throws Exception
	 */
	protected void copyPermissions(String source, String target)throws Exception{
		Permissions p=getStorageAdapter().getProperties(source).getPermissions();
		sms.changePermissions(target, p.isReadable(), p.isWritable(), p.isExecutable());
	}

}
