package eu.unicore.uas.jclouds.s3;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import eu.unicore.security.Client;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.fts.FTSTransferInfo;
import eu.unicore.xnjs.fts.IFTSController;
import eu.unicore.xnjs.fts.SourceFileInfo;
import eu.unicore.xnjs.io.DataStageOutInfo;
import eu.unicore.xnjs.io.FileSet;
import eu.unicore.xnjs.io.IFileTransfer;
import eu.unicore.xnjs.io.IFileTransfer.ImportPolicy;
import eu.unicore.xnjs.io.IFileTransfer.OverwritePolicy;
import eu.unicore.xnjs.io.IFileTransferEngine;
import eu.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.xnjs.io.XnjsFile;
import eu.unicore.xnjs.util.IOUtils;


/**
 * handles exports from local storage to a remote UNICORE storage
 *
 * @author schuller
 */
public class S3ExportsController implements IFTSController {

	private IStorageAdapter localStorage;

	private final Client client;

	private final XNJS xnjs;

	private DataStageOutInfo dso;

	private final Map<String,String> s3Params;

	private final String target;

	private FileSet sourceFileSet;

	private final String workingDirectory;

	public S3ExportsController(XNJS xnjs, Client client, Map<String,String> s3Params, DataStageOutInfo dso, String workingDirectory) {
		this.xnjs = xnjs;
		this.client = client;
		this.dso = dso;
		this.s3Params =  s3Params;
		this.target = this.s3Params.get("file");
		this.workingDirectory = workingDirectory;
	}

	@Override
	public void setStorageAdapter(IStorageAdapter storageAdapter) {
		this.localStorage = storageAdapter;
	}

	public synchronized IStorageAdapter getStorageAdapter() {
		if(localStorage==null) {
			localStorage = xnjs.getTargetSystemInterface(client, dso.getPreferredLoginNode());
		}
		localStorage.setStorageRoot(workingDirectory);
		return localStorage;
	}

	@Override
	public void setOverwritePolicy(OverwritePolicy overwrite) {}

	@Override
	public void setImportPolicy(ImportPolicy importPolicy) {}

	@Override
	public void setExtraParameters(Map<String,String>extraParameters) {}

	@Override
	public void setProtocol(String protocol) {}

	private boolean isDirectory(String file) throws ExecutionException, IOException
	{
		XnjsFile f = getStorageAdapter().getProperties(file);
		if(f!=null) {
			return f.isDirectory();			
		}
		else throw new IOException("The file <"+file+"> does not exist or can not be accessed.");
	}

	private synchronized long getFileSize(String file)throws ExecutionException, IOException {
		XnjsFile f = getStorageAdapter().getProperties(file);
		if(f!=null){
			return f.getSize();
		}
		else throw new IOException("The file <"+file+"> does not exist or can not be accessed.");
	}

	@Override
	public long collectFilesForTransfer(List<FTSTransferInfo> fileList) throws Exception {
		String source = dso.getFileName();
		if(FileSet.hasWildcards(source)){
			sourceFileSet = new FileSet(source);
		}
		else{
			sourceFileSet = new FileSet(source, isDirectory(source));
		}
		long dataSize;
		if(sourceFileSet.isMultifile())
		{
			XnjsFile sourceDir = getStorageAdapter().getProperties(sourceFileSet.getBase());
			if(sourceDir==null) {
				throw new IOException("The directory <"+sourceFileSet.getBase()+
						"> does not exist or cannot be accessed!");
			}
			dataSize = doCollectFiles(fileList, sourceDir, target);
		}
		else
		{
			dataSize = getFileSize(source);
			SourceFileInfo sfi = new SourceFileInfo();
			sfi.setPath(source);
			sfi.setSize(dataSize);
			fileList.add(new FTSTransferInfo(sfi, target, true));
		}
		return dataSize;
	}

	private long doCollectFiles(List<FTSTransferInfo> fileList, XnjsFile sourceFolder,
			String targetFolder) throws Exception
	{
		long result = 0;
		IStorageAdapter sms = getStorageAdapter();
		for (XnjsFile child : sms.ls(sourceFolder.getPath())) {
			String relative = IOUtils.getRelativePath(child.getPath(), sourceFolder.getPath());
			String target = targetFolder + relative;
			if(child.isDirectory() && sourceFileSet.isRecurse())
			{
				result += doCollectFiles(fileList, child, target);
			}
			else 
			{
				if(sourceFileSet.isMultifile() && sourceFileSet.matches(child.getPath())){
					SourceFileInfo sfi = new SourceFileInfo();
					sfi.setPath(child.getPath());
					sfi.setSize(child.getSize());
					fileList.add(new FTSTransferInfo(sfi, target, false));
					result += child.getSize();
				}
			}
		}
		return result;
	}

	@Override
	public IFileTransfer createTransfer(SourceFileInfo from, String to) throws Exception {
		String f = to;
		if(!f.startsWith("/"))f="/"+f;
		String target = "S3:"+s3Params.get("endpoint")+"/"+s3Params.get("bucket")+f;
		DataStageOutInfo info = dso.clone();
		info.setTarget(new URI(target));
		info.setFileName(from.getPath());
		info.getExtraParameters().putAll(s3Params);
		info.getExtraParameters().remove("file");
		return xnjs.get(IFileTransferEngine.class).createFileExport(client, workingDirectory, info);
	}
}