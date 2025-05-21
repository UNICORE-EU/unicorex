package eu.unicore.uas.jclouds.s3;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import eu.unicore.uas.fts.ProgressListener;
import eu.unicore.uas.impl.sms.SMSUtils;
import eu.unicore.uas.util.LogUtil;
import eu.unicore.util.Log;
import eu.unicore.util.Pair;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.io.FileSet;
import eu.unicore.xnjs.io.Permissions;
import eu.unicore.xnjs.io.TransferInfo.Status;
import eu.unicore.xnjs.io.XnjsFile;

/**
 * File exports to S3
 * 
 * @author schuller
 */
public class S3FileExport extends S3FileTransferBase {

	private FileSet fileSet;

	private String permissions = null;

	/**
	 * files to transfer: stores pairs of (local_source + size) and remote filename
	 */
	protected final List<Pair<Pair<String,Long>,String>> filesToTransfer = new ArrayList<>();

	public S3FileExport(XNJS configuration){
		super(configuration);
		this.export = true;
	}	

	public final void run(){
		try{
			setup();
			collectFilesToTransfer();
			info.setStatus(Status.RUNNING);
			runTransfers();
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
			dataSize = computeFileSize(source);
			Pair<String,Long> s = new Pair<>(source,dataSize);
			filesToTransfer.add(new Pair<>(s,target));
		}
		info.setDataSize(dataSize);
	}

	protected boolean isDirectory(String file) throws ExecutionException
	{
		XnjsFile f = getLocalStorage().getProperties(file);
		if(f==null)throw new ExecutionException("The file <"+file+"> does not exist or can not be accessed.");
		return f.isDirectory();
	}

	/**
	 * @param file - the file on the target system to check
	 * @throws IOException if the file does not exist
	 * @throws ExecutionException if the file properties can not be checked
	 */
	protected synchronized long computeFileSize(String file)throws ExecutionException {
		XnjsFile f = getLocalStorage().getProperties(file);
		if(f!=null){
			return f.getSize();
		}
		else throw new ExecutionException("The file <"+file+"> does not exist or can not be accessed.");
	}

	/**
	 * transfer all previously collected files
	 */
	protected void runTransfers() throws Exception {
		for (Pair<Pair<String,Long>,String> pair: filesToTransfer) {
			checkCancelled();
			Pair<String,Long>sourceDesc=pair.getM1();
			String currentSource = sourceDesc.getM1();
			String currentTarget = pair.getM2();
			if(isDirectory(currentSource))
			{
				checkOverwriteAllowed(currentTarget);
				s3Adapter.mkdir(currentTarget);
				info.setTransferredBytes(info.getTransferredBytes()+1);
			}
			else transferFile(sourceDesc, currentTarget);
		}
	}

	/**
	 * checks if we may overwrite the given file (in case it exists)
	 * 
	 * @param fileName -  the file to check
	 * @throws IOException - if the file exists and DONT_OVERWRITE is requested
	 */
	protected void checkOverwriteAllowed(String fileName)throws Exception{
		if(OverwritePolicy.DONT_OVERWRITE.equals(overwrite)){
			XnjsFile g=null;
			try{
				//will throw IOexception if file does not exist
				g = s3Adapter.getProperties(fileName);
			}catch(Exception ioe){}
			if(g!=null){
				throw new IOException("File <"+g.getPath()+"> exists, and we have been asked to not overwrite.");
			}
		}
	}
	
	@Override
	public void setPermissions(String permissions) {
		this.permissions = permissions;
	}

	/**
	 * copies the permissions of the local file to the remote file
	 */
	protected void copyPermissions(String source, String target) {
		try{
			Permissions p = getLocalStorage().getProperties(source).getPermissions();
			s3Adapter.chmod(target, p);
		}catch(Exception ex) {
			Log.logException("Could not set permissions of remote file <"+target+">", ex, logger);
		}	
	}

	/**
	 * sets up security and creates the SMS client
	 */
	protected void setup() throws Exception{
		checkCancelled();
		initSecurityProperties();
		s3Adapter = createS3Adapter();
		info.setSource(cleanLocalFilePath(info.getSource()));
		startTime=System.currentTimeMillis();
	}

	protected boolean supportsResume(){
		return false;
	}

	/**
	 * Import a single file.<br/> 
	 * Will check if the remote file system is in fact the local one, and
	 * will invoke the transferFileByCopy() in that case
	 * @param source
	 * @param target
	 * @throws Exception
	 */
	protected void transferFile(Pair<String,Long> sourceDesc, String target) throws Exception
	{
		checkCancelled();
		String localFile = sourceDesc.getM1();
		boolean append=OverwritePolicy.APPEND.equals(overwrite);
		try(InputStream is = getLocalStorage().getInputStream(localFile);
				OutputStream os= s3Adapter.getOutputStream(target,append,sourceDesc.getM2())){
			copy(is, os);
		}
		if(statusTracker!=null)statusTracker.update(info);
	}

	/**
	 * Recursively gather all files and directories that need to be copied
	 * @return total file size, including one extra byte for each directory
	 */
	protected long doCollectFiles(List<Pair<Pair<String,Long>,String>> collection, String sourceFolder, String targetFolder) throws Exception
	{
		long result = 1;
		collection.add(new Pair<Pair<String,Long>,String>(new Pair<String,Long>(sourceFolder,1l), targetFolder));
		for (XnjsFile child : getLocalStorage().ls(sourceFolder)) {
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

	protected void doRun(String localFile)throws Exception{
		
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
