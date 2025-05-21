package eu.unicore.uas.jclouds;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.inject.Singleton;

import eu.unicore.security.Client;
import eu.unicore.uas.jclouds.s3.S3ExportsController;
import eu.unicore.uas.jclouds.s3.S3FileExport;
import eu.unicore.uas.jclouds.s3.S3FileImport;
import eu.unicore.uas.jclouds.s3.S3ImportsController;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.fts.IFTSController;
import eu.unicore.xnjs.io.DataStageInInfo;
import eu.unicore.xnjs.io.DataStageOutInfo;
import eu.unicore.xnjs.io.IFileTransfer;
import eu.unicore.xnjs.io.IFileTransferCreator;

/**
 * creates {@link IFileTransfer} instances that use a jclouds-supported protocol
 * such as S3
 *
 * @author schuller
 */
@Singleton
public class CloudFileTransferCreator implements IFileTransferCreator{

	private final XNJS xnjs;

	public CloudFileTransferCreator(XNJS config) {
		this.xnjs = config;
	}

	@Override
	public int getPriority() {
		return 1;
	}
	
	@Override
	public IFileTransfer createFileExport(Client client, String workdir, DataStageOutInfo info) {
		URI target = info.getTarget();
		if(isS3(target.toASCIIString())) {
			Map<String,String>urlInfo = extractUrlInfo(target);
			if(info.getExtraParameters()!=null) {
				urlInfo.putAll(info.getExtraParameters());
			}
			return createExportS3(client, workdir, urlInfo, info);
		}
		return null;
	}

	@Override
	public IFileTransfer createFileImport(Client client, String workdir, DataStageInInfo info){
		URI source = info.getSources()[0];
		if(isS3(source.toASCIIString())) {
			Map<String,String>urlInfo = extractUrlInfo(source);
			if(info.getExtraParameters()!=null) {
				urlInfo.putAll(info.getExtraParameters());
			}
			return createImportS3(client, workdir, urlInfo, info);
		}
		return null;
	}

	@Override
	public String getProtocol() {
		return "S3";
	}

	@Override
	public String getStageOutProtocol() {
		return getProtocol();
	}

	@Override
	public String getStageInProtocol() {
		return getProtocol();
	}

	/**
	 * create a transfer FROM a S3 storage to a local file
	 *
	 * the assumed URI format is
	 *   
	 *   S3:http(s)://s3endpoint/bucket/filespec
	 *
	 * @return IFileTransfer instance
	 */
	private IFileTransfer createImportS3(Client client, String workdir, 
			Map<String,String> s3Params, DataStageInInfo info){
		S3FileImport ft = new S3FileImport(xnjs);
		ft.setClient(client);
		ft.setWorkdir(workdir);
		ft.getInfo().setSource(s3Params.get("file"));
		ft.getInfo().setTarget(info.getFileName());
		ft.setS3Params(s3Params);
		return ft;
	} 

	/**
	 * create a transfer TO a S3 storage from a local file
	 *
	 * the assumed URI format is
	 *   
	 *   S3:http(s)://s3endpoint/bucket/filespec
	 *
	 * @return IFileTransfer instance
	 */
	private IFileTransfer createExportS3(Client client, String workdir, 
			Map<String,String> s3Params, DataStageOutInfo info){
		S3FileExport ft = new S3FileExport(xnjs);
		ft.setClient(client);
		ft.setWorkdir(workdir);
		ft.getInfo().setTarget(s3Params.get("file"));
		ft.getInfo().setSource(info.getFileName());
		ft.setS3Params(s3Params);
		return ft;
	}

	private static final Pattern  s3URLPattern = Pattern.compile("S3:(.*)://([^/]*)/([^/]*)/(.*)");

	public static boolean isS3(String url) {
		return s3URLPattern.matcher(url).matches();
	}

	/**
	 * returns S3 details
	 */
	public static Map<String,String> extractUrlInfo(URI url){
		return extractUrlInfo(url.toString());
	}

	/**
	 * returns UNICORE protocol and Storage URL
	 */
	public static Map<String,String>extractUrlInfo(String url){
		Matcher m = s3URLPattern.matcher(url);
		if(!m.matches())throw new IllegalArgumentException("Not a valid S3 URL <"+url+">");
		Map<String,String> info = new HashMap<>();
		info.put("endpoint", m.group(1)+"://"+m.group(2));
		info.put("bucket", m.group(3));
		info.put("file", m.group(4));
		return info;
	}

	@Override
	public IFTSController createFTSImport(Client client, String workingDirectory, DataStageInInfo info)
			throws IOException {
		URI source = info.getSources()[0];
		Matcher m = s3URLPattern.matcher(source.toASCIIString());
		if(m.matches()) {
			Map<String,String>urlInfo = extractUrlInfo(source);
			if(info.getExtraParameters()!=null) {
				urlInfo.putAll(info.getExtraParameters());
			}
			return new S3ImportsController(xnjs, client, urlInfo, info, workingDirectory);
		}
		return null;
	}

	@Override
	public IFTSController createFTSExport(Client client, String workingDirectory, DataStageOutInfo info)
			throws IOException {
		URI target = info.getTarget();
		Matcher m = s3URLPattern.matcher(target.toASCIIString());
		if(m.matches()) {
			Map<String,String>urlInfo = extractUrlInfo(target);
			if(info.getExtraParameters()!=null) {
				urlInfo.putAll(info.getExtraParameters());
			}
			return new S3ExportsController(xnjs, client, urlInfo, info, workingDirectory);
		}
		return null;
	}

}
