package de.fzj.unicore.uas.fts.http;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import de.fzj.unicore.uas.xnjs.UFileTransferCreator;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.fts.FTSTransferInfo;
import de.fzj.unicore.xnjs.fts.IFTSController;
import de.fzj.unicore.xnjs.fts.SourceFileInfo;
import de.fzj.unicore.xnjs.io.DataStageOutInfo;
import de.fzj.unicore.xnjs.io.FileSet;
import de.fzj.unicore.xnjs.io.IFileTransfer;
import de.fzj.unicore.xnjs.io.IFileTransfer.ImportPolicy;
import de.fzj.unicore.xnjs.io.IFileTransfer.OverwritePolicy;
import de.fzj.unicore.xnjs.io.IFileTransferEngine;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import de.fzj.unicore.xnjs.io.OptionNotSupportedException;
import de.fzj.unicore.xnjs.io.XnjsFile;
import de.fzj.unicore.xnjs.util.IOUtils;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.StorageClient;
import eu.unicore.security.Client;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.services.rest.jwt.JWTDelegation;
import eu.unicore.services.rest.jwt.JWTServerProperties;

public class BFTExportsController implements IFTSController {

	protected IStorageAdapter localStorage;

	protected final Client client;
	
	protected final XNJS xnjs;
	
	protected final Kernel kernel;

	protected DataStageOutInfo dso;

	protected OverwritePolicy overwritePolicy;
	
	protected ImportPolicy importPolicy;
	
	protected Map<String,String> extraParameters;
	
	protected StorageClient remoteStorage;
	
	protected final Endpoint remoteEndpoint;

	protected final String target;
	
	protected FileSet sourceFileSet;

	protected final String workingDirectory;
	
	public BFTExportsController(XNJS xnjs, Client client, Endpoint remoteEndpoint, DataStageOutInfo dso, String workingDirectory) {
		this.xnjs = xnjs;
		this.kernel = xnjs.get(Kernel.class);
		this.client = client;
		this.remoteEndpoint = remoteEndpoint;
		this.dso = dso;
		this.target = UFileTransferCreator.getFileSpec(dso.getTarget().toString());
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
	public void setOverwritePolicy(OverwritePolicy overwrite) throws OptionNotSupportedException {
		this.overwritePolicy = overwrite;
	}

	@Override
	public void setImportPolicy(ImportPolicy importPolicy) {
		this.importPolicy = importPolicy;
	}

	@Override
	public void setExtraParameters(Map<String,String>extraParameters) {
		this.extraParameters = extraParameters;
	}

	protected void setup() throws Exception {
		if(remoteStorage==null) {
			String user = client.getDistinguishedName();
			IAuthCallback auth = new JWTDelegation(kernel.getContainerSecurityConfiguration(), 
					new JWTServerProperties(kernel.getContainerProperties().getRawProperties()), user);
			remoteStorage = new StorageClient(remoteEndpoint, kernel.getClientConfiguration().clone(), auth);
		}
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
		setup();
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
			fileList.add(new FTSTransferInfo(sfi, dso.getTarget().toString(), false));
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
		setup();
		String target = remoteEndpoint.getUrl()+"/files/"+to;
		DataStageOutInfo info = dso.clone();
		info.setTarget(new URI(target));
		info.setFileName(from.getPath());
		IFileTransfer ft = xnjs.get(IFileTransferEngine.class).createFileExport(client, workingDirectory, info);
		return ft;
	}

}
