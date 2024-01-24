package eu.unicore.xnjs.io.impl;

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

public class HttpExportsController implements IFTSController {

	protected IStorageAdapter localStorage;

	protected final Client client;
	
	protected final XNJS xnjs;

	protected DataStageOutInfo dso;
	
	protected Map<String,String> extraParameters;
	
	protected final String remoteStorageURL;

	protected FileSet sourceFileSet;

	protected final String workingDirectory;
	
	protected String protocol;
	
	protected String target = "/";

	public HttpExportsController(XNJS xnjs, Client client, DataStageOutInfo dso, String workingDirectory) {
		this.xnjs = xnjs;
		this.client = client;
		this.dso = dso;
		this.remoteStorageURL = dso.getTarget().toString();
		this.workingDirectory = workingDirectory;
	}

	@Override
	public void setStorageAdapter(IStorageAdapter storageAdapter) {
		this.localStorage = storageAdapter;
	}
	
	public synchronized IStorageAdapter getStorageAdapter() {
		if(localStorage==null) {
			localStorage = xnjs.getTargetSystemInterface(client);
		}
		localStorage.setStorageRoot(workingDirectory);
		return localStorage;
	}

	
	@Override
	public void setOverwritePolicy(OverwritePolicy overwrite) {
		dso.setOverwritePolicy(overwrite);
	}

	@Override
	public void setImportPolicy(ImportPolicy importPolicy) {
		// NOP
	}

	@Override
	public void setExtraParameters(Map<String,String>extraParameters) {
		this.extraParameters = extraParameters;
	}

	@Override
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	protected boolean isDirectory(String file) throws ExecutionException, IOException
	{
		XnjsFile f = getStorageAdapter().getProperties(file);
		if(f!=null) {
			return f.isDirectory();			
		}
		else throw new IOException("The file <"+file+"> does not exist or can not be accessed.");
	}

	protected synchronized long getFileSize(String file)throws ExecutionException, IOException {
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
			fileList.add(new FTSTransferInfo(sfi, getFileSpec(dso.getTarget()), false));
		}
		return dataSize;
	}
	
	protected long doCollectFiles(List<FTSTransferInfo> fileList, XnjsFile sourceFolder, 
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
		DataStageOutInfo info = dso.clone();
		info.setTarget(new URI(remoteStorageURL));
		info.setFileName(from.getPath());
		return xnjs.get(IFileTransferEngine.class).createFileExport(client, workingDirectory, info);
	}

	protected String getFileSpec(URI url) throws Exception {
		return  url.toURL().getFile();
	}
}
