package eu.unicore.uas.jclouds.s3;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import eu.unicore.security.Client;
import eu.unicore.services.Kernel;
import eu.unicore.uas.impl.sms.SMSBaseImpl;
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
import eu.unicore.xnjs.io.XnjsFile;
import eu.unicore.xnjs.util.IOUtils;

/**
 * handles imports from a remote UNICORE storage to local storage
 *
 * @author schuller
 */
public class S3ImportsController implements IFTSController {

	private final Client client;

	private final XNJS xnjs;

	private final Kernel kernel;

	private DataStageInInfo dsi;

	private IStorageAdapter s3adapter;

	private final String source;

	private FileSet sourceFileSet;

	private XnjsFile remoteBaseInfo;

	private final String workingDirectory;
	
	private final Map<String,String> s3Params;
	

	public S3ImportsController(XNJS xnjs, Client client, Map<String,String> s3Params, DataStageInInfo dsi, String workingDirectory) {
		this.xnjs = xnjs;
		this.kernel = xnjs.get(Kernel.class);
		this.client = client;
		this.dsi = dsi;
		this.s3Params =  s3Params;
		this.source = this.s3Params.get("file");
		this.workingDirectory = workingDirectory;
	}

	@Override
	public void setStorageAdapter(IStorageAdapter storageAdapter) {}

	@Override
	public void setOverwritePolicy(OverwritePolicy overwrite) {}

	@Override
	public void setImportPolicy(ImportPolicy importPolicy) {}

	@Override
	public void setExtraParameters(Map<String,String>extraParameters) {}

	@Override
	public void setProtocol(String protocol) {}

	private void setup() throws Exception {
		if(s3adapter==null) {
			s3adapter = createS3Adapter();
		}
	}

	private void getRemoteFileInfo(String source) throws Exception {
		if(!FileSet.hasWildcards(source)){
			remoteBaseInfo = s3adapter.getProperties(source);
			if(remoteBaseInfo.isDirectory()){
				sourceFileSet = new FileSet(source, true);
			}
			else{
				sourceFileSet = new FileSet(source);
			}
		}
		else{
			sourceFileSet = new FileSet(source);
			remoteBaseInfo = s3adapter.getProperties(sourceFileSet.getBase());
		}
		if(remoteBaseInfo == null){
			throw new FileNotFoundException("No files found for: "+source);
		}
	}

	@Override
	public long collectFilesForTransfer(List<FTSTransferInfo> fileList) throws Exception {
		setup();
		getRemoteFileInfo(source);
		if(remoteBaseInfo.isDirectory()) {
			return doCollectFiles(fileList, remoteBaseInfo, dsi.getFileName(), remoteBaseInfo.getPath());
		}
		else{
			SourceFileInfo sfi = new SourceFileInfo();
			sfi.setPath(remoteBaseInfo.getPath());
			sfi.setSize(remoteBaseInfo.getSize());
			fileList.add(new FTSTransferInfo(sfi, dsi.getFileName(), false));
			return remoteBaseInfo.getSize();
		}
	}

	private long doCollectFiles(List<FTSTransferInfo> fileList, XnjsFile sourceFolder,
			String targetFolder, String baseDirectory) throws Exception
	{
		long result = 0;
		for (XnjsFile child : s3adapter.ls(sourceFolder.getPath(), 0, SMSBaseImpl.MAX_LS_RESULTS, false)) {
			String relative = IOUtils.getRelativePath(child.getPath(), sourceFolder.getPath());
			String target = targetFolder+relative;
			while(target.startsWith("//"))target=target.substring(1);
			if(child.isDirectory() && sourceFileSet.isRecurse())
			{
				result += doCollectFiles(fileList, child, target, baseDirectory);
			}
			else 
			{
				if(remoteBaseInfo.isDirectory() && sourceFileSet.matches(child.getPath())){
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

	protected IStorageAdapter createS3Adapter() throws IOException {
		String accessKey = s3Params.get("accessKey");
		String secretKey = s3Params.get("secretKey");
		String endpoint = s3Params.get("endpoint");
		String bucket = s3Params.get("bucket");
		String provider = s3Params.get("provider");
		boolean sslValidate = Boolean.parseBoolean(s3Params.get("validate"));
		if(provider==null)provider="aws-s3";
		if(bucket==null)throw new IllegalArgumentException("Parameter 'bucket' is required");
		if(endpoint==null)throw new IllegalArgumentException("Parameter 'endpoint' is required");
		return new S3StorageAdapterFactory().createStorageAdapter(kernel, accessKey, secretKey, 
				endpoint, provider, bucket, null, sslValidate);
	}
	
	@Override
	public IFileTransfer createTransfer(SourceFileInfo from, String to) throws Exception {
		setup();
		String f = from.getPath();
		if(!f.startsWith("/"))f="/"+f;
		String source = "S3:"+s3Params.get("endpoint")+"/"+s3Params.get("bucket")+f;
		DataStageInInfo info = dsi.clone();
		info.setSources(new URI[]{new URI(source)});
		info.setFileName(to);
		info.getExtraParameters().putAll(s3Params);
		info.getExtraParameters().remove("file");
		return xnjs.get(IFileTransferEngine.class).createFileImport(client, workingDirectory, info);
	}
}