package eu.unicore.uas.fts;

import java.io.FileNotFoundException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.FileList.FileListEntry;
import eu.unicore.client.core.StorageClient;
import eu.unicore.security.Client;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.jwt.JWTDelegation;
import eu.unicore.services.rest.jwt.JWTServerProperties;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.uas.impl.sms.SMSBaseImpl;
import eu.unicore.uas.xnjs.UFileTransferCreator;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.fts.FTSTransferInfo;
import eu.unicore.xnjs.fts.IFTSController;
import eu.unicore.xnjs.fts.SourceFileInfo;
import eu.unicore.xnjs.io.DataStageInInfo;
import eu.unicore.xnjs.io.FileSet;
import eu.unicore.xnjs.io.IFileTransfer;
import eu.unicore.xnjs.io.IFileTransfer.ImportPolicy;
import eu.unicore.xnjs.io.IFileTransfer.OverwritePolicy;
import eu.unicore.xnjs.io.IFileTransferEngine;
import eu.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.xnjs.util.IOUtils;

/**
 * handles imports from a remote UNICORE storage to local storage
 *
 * @author schuller
 */
public class ImportsController implements IFTSController {

	private final Client client;

	private final XNJS xnjs;

	private final Kernel kernel;

	private DataStageInInfo dsi;

	private StorageClient remoteStorage;

	private final Endpoint remoteEndpoint;

	private final String source;

	private FileSet sourceFileSet;

	private FileListEntry remoteBaseInfo;

	private final String workingDirectory;

	private String protocol;

	public ImportsController(XNJS xnjs, Client client, Endpoint remoteEndpoint, DataStageInInfo dsi, String workingDirectory) {
		this.xnjs = xnjs;
		this.kernel = xnjs.get(Kernel.class);
		this.client = client;
		this.remoteEndpoint = remoteEndpoint;
		this.dsi = dsi;
		this.source = UFileTransferCreator.getFileSpec(dsi.getSources()[0].toString());
		this.workingDirectory = workingDirectory;
	}

	@Override
	public void setStorageAdapter(IStorageAdapter storageAdapter) {
		// unused
	}

	@Override
	public void setOverwritePolicy(OverwritePolicy overwrite) {
		// unused
	}

	@Override
	public void setImportPolicy(ImportPolicy importPolicy) {
		// unused
	}

	@Override
	public void setExtraParameters(Map<String,String>extraParameters) {
		// unused
	}

	@Override
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	private void setup() throws Exception {
		if(remoteStorage==null) {
			String user = client.getDistinguishedName();
			IAuthCallback auth = new JWTDelegation(kernel.getContainerSecurityConfiguration(), 
					new JWTServerProperties(kernel.getContainerProperties().getRawProperties()), user);
			remoteStorage = new StorageClient(remoteEndpoint, kernel.getClientConfiguration(), auth);
		}
	}

	private void getRemoteFileInfo(String source) throws Exception {
		if(!FileSet.hasWildcards(source)){
			remoteBaseInfo = remoteStorage.stat(source);
			boolean dir = remoteBaseInfo.isDirectory;
			if(dir){
				sourceFileSet = new FileSet(source, true);
			}
			else{
				sourceFileSet = new FileSet(source);
			}
		}
		else{
			sourceFileSet = new FileSet(source);
			remoteBaseInfo = remoteStorage.stat(sourceFileSet.getBase());
		}
		if(remoteBaseInfo == null){
			throw new FileNotFoundException("No files found for: "+source);
		}
	}

	@Override
	public long collectFilesForTransfer(List<FTSTransferInfo> fileList) throws Exception {
		setup();
		getRemoteFileInfo(source);
		if(remoteBaseInfo.isDirectory) {
			return doCollectFiles(fileList, remoteBaseInfo, dsi.getFileName(), remoteBaseInfo.path);
		}
		else{
			SourceFileInfo sfi = new SourceFileInfo();
			sfi.setPath(remoteBaseInfo.path);
			sfi.setSize(remoteBaseInfo.size);
			fileList.add(new FTSTransferInfo(sfi, dsi.getFileName(), false));
			return remoteBaseInfo.size;
		}
	}

	private long doCollectFiles(List<FTSTransferInfo> fileList, FileListEntry sourceFolder,
			String targetFolder, String baseDirectory) throws Exception
	{
		long result = 0;
		for (FileListEntry child : remoteStorage.ls(sourceFolder.path).list(0, SMSBaseImpl.MAX_LS_RESULTS)) {
			String relative = IOUtils.getRelativePath(child.path, sourceFolder.path);
			String target = targetFolder+relative;
			if(child.isDirectory && sourceFileSet.isRecurse())
			{
				result += doCollectFiles(fileList, child, target, baseDirectory);
			}
			else 
			{
				if(remoteBaseInfo.isDirectory && sourceFileSet.matches(child.path)){
					SourceFileInfo sfi = new SourceFileInfo();
					sfi.setPath(child.path);
					sfi.setSize(child.size);
					fileList.add(new FTSTransferInfo(sfi, target, false));
					result += child.size;
				}
			}
		}
		return result;
	}

	@Override
	public IFileTransfer createTransfer(SourceFileInfo from, String to) throws Exception {
		setup();
		String source = remoteEndpoint.getUrl() +
				FilenameUtils.normalize("/files"+from.getPath(), true);
		if(protocol!=null)source=protocol+":"+source;
		DataStageInInfo info = dsi.clone();
		info.setSources(new URI[]{new URI(source)});
		info.setFileName(to);
		return xnjs.get(IFileTransferEngine.class).createFileImport(client, workingDirectory, info);
	}
}