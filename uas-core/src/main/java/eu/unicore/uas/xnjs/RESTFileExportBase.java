package eu.unicore.uas.xnjs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eu.unicore.client.core.FileList.FileListEntry;
import eu.unicore.client.core.StorageClient;
import eu.unicore.uas.UASProperties;
import eu.unicore.uas.fts.FiletransferOptions;
import eu.unicore.uas.fts.FiletransferOptions.IMonitorable;
import eu.unicore.uas.util.LogUtil;
import eu.unicore.uas.fts.ProgressListener;
import eu.unicore.util.Log;
import eu.unicore.util.Pair;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.io.FileSet;
import eu.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.xnjs.io.TransferInfo.Status;
import eu.unicore.xnjs.io.XnjsFile;
import eu.unicore.xnjs.tsi.TSI;

/**
 * Base class for UNICORE RESTful file exports with support for BFT transfer
 *
 * @author schuller
 */
public class RESTFileExportBase extends RESTFileTransferBase {

	/**
	 * storage client for accessing the remote storage
	 */
	protected StorageClient storage;

	/**
	 * files to transfer: stores pairs of (local_source + size) and remote filename
	 */
	protected final List<Pair<Pair<String,Long>,String>> filesToTransfer = new ArrayList<Pair<Pair<String,Long>,String>>();

	private FileSet fileSet;

	public RESTFileExportBase(XNJS configuration){
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
		storage = new StorageClient(storageEndpoint, sec, auth);
		info.setSource(cleanLocalFilePath(info.getSource()));
		startTime=System.currentTimeMillis();
	}

	@Override
	protected void onFinishCleanup(){
		super.onFinishCleanup();
		storage = null;
	}

	/**
	 * collect all the files that need to be transferred, and store them
	 * in {@link #filesToTransfer}
	 * @throws Exception
	 */
	protected void collectFilesToTransfer() throws Exception {
		String source = info.getSource();
		String target = info.getTarget();
		if(FileSet.hasWildcards(source)){
			fileSet = new FileSet(source);
		}
		else{
			fileSet = new FileSet(source, isDirectory(source));
		}
		long dataSize;
		if(fileSet.isMultifile())
		{
			dataSize = doCollectFiles(filesToTransfer, fileSet.getBase(), target);
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
				checkOverwriteAllowed(storage, currentTarget);
				storage.mkdir(currentTarget);
				info.setTransferredBytes(info.getTransferredBytes()+1);
			}
			else transferFile(sourceDesc, currentTarget);
		}
	}

	/**
	 * check if we can replace the remote file transfer by a local copy
	 */
	protected boolean checkPossibilityForLocalCopy()throws Exception {
		String remoteFSID = storage.getFileSystemDescription();
		String localFSID = getStorageAdapter().getFileSystemIdentifier();
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
		String base = storage.getMountPoint();
		String targetUmask = storage.getProperties().optString("umask", null);
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
			if(s instanceof TSI && tgt.contains("$")) {
				tgt = ((TSI)s).resolve(tgt);
			}
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
		checkOverwriteAllowed(storage, target);
		Map<String,String>ep=getExtraParameters();
		boolean append=OverwritePolicy.APPEND.equals(overwrite);
		ftc = storage.createImport(target, append, -1, info.getProtocol(), ep);
		if(ftc instanceof IMonitorable){
			((IMonitorable)ftc).setProgressListener(this);
		}
		String localFile = sourceDesc.getM1();
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
			FileListEntry g=null;
			try{
				//will throw IOexception if file does not exist
				g = sms.stat(fileName);
			}catch(IOException ioe){}
			if(g!=null){
				throw new IOException("File <"+g.path+"> exists, and we have been asked to not overwrite.");
			}
		}
	}

	/**
	 * Recursively gather all files and directories that need to be copied
	 */
	protected long doCollectFiles(List<Pair<Pair<String,Long>,String>> collection, String sourceFolder, String targetFolder) throws Exception
	{
		long result = 1;
		collection.add(new Pair<Pair<String,Long>,String>(new Pair<String,Long>(sourceFolder,1l), targetFolder));
		for (XnjsFile child : getStorageAdapter().ls(sourceFolder)) {
			String path = child.getPath();
			String relative = new File(path).getName();
			String target = targetFolder+"/"+relative;

			if(child.isDirectory() && fileSet.isRecurse())
			{
				result += doCollectFiles(collection, path, target);
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
		try(InputStream is = getInputStream(localFile)){
			checkCancelled();
			((FiletransferOptions.Write)ftc).writeAllData(is);
		}
	}


	protected InputStream getInputStream(String localFile)throws ExecutionException, IOException{
		return getStorageAdapter().getInputStream(localFile);
	}

	/**
	 * copies the permissions of the local file to the remote file
	 */
	protected void copyPermissions(String source, String target) {
		try{
			String p = getStorageAdapter().getProperties(source).getUNIXPermissions();
			storage.chmod(target, p);
		}catch(Exception ex) {
			Log.logException("Could not set permissions of remote file <"+target+">", ex, logger);
		}	
	}

}
