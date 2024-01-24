package de.fzj.unicore.uas.xnjs;

import java.io.IOException;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;

import com.google.inject.Singleton;

import de.fzj.unicore.uas.fts.FileTransferCapabilities;
import de.fzj.unicore.uas.fts.FileTransferCapability;
import de.fzj.unicore.uas.util.LogUtil;
import eu.unicore.client.Endpoint;
import eu.unicore.security.Client;
import eu.unicore.services.Kernel;
import eu.unicore.util.Pair;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.fts.IFTSController;
import eu.unicore.xnjs.io.DataStageInInfo;
import eu.unicore.xnjs.io.DataStageOutInfo;
import eu.unicore.xnjs.io.DataStagingCredentials;
import eu.unicore.xnjs.io.IFileTransfer;
import eu.unicore.xnjs.io.IFileTransferCreator;

/**
 * creates {@link IFileTransfer} instances that use a UNICORE protocol such as BFT 
 *
 * @author schuller
 */
@Singleton
public class UFileTransferCreator implements IFileTransferCreator{
	
	protected static final Logger logger = LogUtil.getLogger(LogUtil.DATA,UFileTransferCreator.class);
	
	private final XNJS xnjs;
	
	private final Kernel kernel;
	
	public UFileTransferCreator(XNJS config) {
		super();
		this.xnjs = config;
		kernel = config.get(Kernel.class);
	}

	@Override
	public int getPriority() {
		return 1;
	}
	

	public IFileTransfer createFileExport(Client client, String workdir, DataStageOutInfo info) {
		String source = info.getFileName();
		URI target = info.getTarget();
		DataStagingCredentials creds = info.getCredentials();
		if(isREST(target)) {
			Pair<String,String>urlInfo = extractUrlInfo(target);
			String protocol = urlInfo.getM1();
			FileTransferCapability fc=FileTransferCapabilities.getCapability(protocol, kernel);
			if(fc!=null){
				if(fc.isAvailable()){
					Endpoint ep = new Endpoint(urlInfo.getM2());
					IFileTransfer f = createExportREST(ep, fc.getExporter(),client,workdir,source,target,creds);
					f.setOverwritePolicy(info.getOverwritePolicy());
					if(info.getExtraParameters()!=null){
						f.setExtraParameters(info.getExtraParameters());
					}
					return f;
				}
				else{
					throw new RuntimeException("File transfer for protocol <"+protocol+"> is not available!");
				}
			}
		}
		return null;
	}

	@Override
	public IFileTransfer createFileImport(Client client, String workdir, DataStageInInfo in){
		URI source = in.getSources()[0];
		String target = in.getFileName();
		DataStagingCredentials creds = in.getCredentials();
		if(isREST(source)) {
			Pair<String,String>urlInfo = extractUrlInfo(source);
			String protocol = urlInfo.getM1();
			FileTransferCapability fc=FileTransferCapabilities.getCapability(protocol, kernel);
			if(fc!=null){
				if(fc.isAvailable()){
					Endpoint ep = new Endpoint(urlInfo.getM2());
					IFileTransfer f = createImportREST(ep, fc.getImporter(),client,workdir,source,target,creds);
					f.setOverwritePolicy(in.getOverwritePolicy());
					if(in.getExtraParameters()!=null){
						f.setExtraParameters(in.getExtraParameters());
					}
					return f;
				}
				else{
					throw new RuntimeException("File transfer for protocol <"+protocol+"> is not available!");
				}
			}
		}
		return null;
	}

	public static boolean isREST(URI url) {
		return isREST(url.toString());
	}
	
	public static boolean isREST(String url) {
		return url.contains("/rest/core/storages/") && url.contains("/files");
	}
	
	public String getProtocol() {
		return String.valueOf(FileTransferCapabilities.getProtocols(kernel));
	}
	
	public String getStageOutProtocol() {
		return getProtocol();
	}
	
	public String getStageInProtocol() {
		return getProtocol();
	}
	
	/**
	 * create a transfer FROM a RESTful storage to a local file
	 * 
	 * the assumed URI format is
	 *   
	 *   unicore_protocol:http(s)://host:port/rest/core/storages/resourceID/files/filespec
	 * 
	 * @param clazz 
	 * @param client
	 * @param workdir
	 * @param source - remote file
	 * @param targetFile - local file
	 * @param creds - ignored
	 * @return IFileTransfer instance
	 */
	public IFileTransfer createImportREST(Endpoint ep, Class<? extends IFileTransfer> clazz, Client client, String workdir, URI source, String targetFile, DataStagingCredentials creds){
		String sourceFile = getFileSpec(source.toString());
		try{
			RESTFileTransferBase ft=(RESTFileTransferBase)clazz.getConstructor(XNJS.class).newInstance(xnjs);
			ft.setClient(client);
			ft.setWorkdir(workdir);
			ft.getInfo().setSource(sourceFile);
			ft.getInfo().setTarget(targetFile);
			ft.setStorageEndpoint(ep);
			ft.setExport(false);
			return ft;
		}catch(Exception e){
			logger.warn("Unable to instantiate file transfer", e);
			return null;
		}
	} 
	
	public IFileTransfer createExportREST(Endpoint ep, Class<? extends IFileTransfer> clazz, Client client, String workdir, String sourceFile, URI target, DataStagingCredentials credentials){
		String targetFile = getFileSpec(target.toString());
		try{
			RESTFileTransferBase ft = (RESTFileTransferBase)clazz.getConstructor(XNJS.class).newInstance(xnjs);
			ft.setClient(client);
			ft.getInfo().setSource(sourceFile);
			ft.getInfo().setTarget(targetFile);
			ft.setWorkdir(workdir);
			ft.setStorageEndpoint(ep);
			ft.setExport(true);
			return ft;
		}catch(Exception e){
			e.printStackTrace();
			logger.warn("Unable to instantiate file transfer", e);
			return null;
		}
	}
	
	
	public static String getFileSpec(String restURL) {
		String[] tokens = urlDecode(restURL.toString()).split("/files",2);
		return tokens.length>1? tokens[1] : "/";
	}
	
	private static final Pattern restURLPattern = Pattern.compile("(.*)://(.*/rest/core/storages/.*)/files/.*");
	
	/**
	 * extracts the storage part from a REST staging URL
	 */
	public static Pair<String,String>extractUrlInfo(URI url){
		return extractUrlInfo(url.toString());
	}

	/**
	 * extracts the storage part from a REST staging URL
	 */
	public static Pair<String,String>extractUrlInfo(String url){
		Matcher m = restURLPattern.matcher(url);
		if(!m.matches())throw new IllegalArgumentException("Not a UNICORE REST storage URL <"+url+">");
		String schemeSpec=m.group(1);
		String protocol, scheme;
		
		if(schemeSpec.equalsIgnoreCase("http")||schemeSpec.equalsIgnoreCase("https")) {
			protocol = "BFT";
			scheme = schemeSpec;;
		}
		else {
			String[] tok = schemeSpec.split(":");
			protocol = tok[0];
			scheme = tok[1];
		}
		String base=m.group(2);
		String rest_url = scheme+"://"+base;
		return new Pair<>(protocol, rest_url);
	}

	/**
	 * replace URI-encoded characters by their unencoded counterparts
	 * @param orig
	 * @return decoded URL
	 */
	public static String urlDecode(String orig){
		try{
			return orig.replaceAll("%20", " ");
		}catch(Exception e){
			return orig;
		}
	}


	@Override
	public IFTSController createFTSImport(Client client, String workingDirectory, DataStageInInfo info)
			throws IOException {
		URI source = info.getSources()[0];
		if(!isREST(source))return null;

		Pair<String,String>urlInfo = extractUrlInfo(source);
		String protocol = urlInfo.getM1();
		Endpoint ep = new Endpoint(urlInfo.getM2());
		FileTransferCapability fc = FileTransferCapabilities.getCapability(protocol, kernel);
		if(fc==null || fc.getFTSImportsController()==null) {
			throw new IOException("Server-to-Server transfer not available for protocol "+protocol);
		}
		try{
			IFTSController fts = fc.getFTSImportsController().getConstructor(
					XNJS.class, Client.class, Endpoint.class, DataStageInInfo.class, String.class).
					newInstance(xnjs, client, ep, info, workingDirectory);
			fts.setProtocol(protocol);
			fts.setOverwritePolicy(info.getOverwritePolicy());
			fts.setImportPolicy(info.getImportPolicy());
			if(info.getExtraParameters()!=null) {
				fts.setExtraParameters(info.getExtraParameters());
			}
			return fts;
		}catch(Exception e) {
			throw new IOException(e);
		}
	}


	@Override
	public IFTSController createFTSExport(Client client, String workingDirectory, DataStageOutInfo info)
			throws IOException {
		URI target = info.getTarget();
		if(!isREST(target))return null;

		Pair<String,String>urlInfo = extractUrlInfo(target);
		String protocol = urlInfo.getM1();
		Endpoint ep = new Endpoint(urlInfo.getM2());
		FileTransferCapability fc = FileTransferCapabilities.getCapability(protocol, kernel);
		if(fc==null || fc.getFTSExportsController()==null) {
			throw new IOException("Server-to-Server transfer not available for protocol "+protocol);
		}
		try{
			IFTSController fts = fc.getFTSExportsController().getConstructor(
					XNJS.class, Client.class, Endpoint.class, DataStageOutInfo.class, String.class).
					newInstance(xnjs, client, ep, info, workingDirectory);
			fts.setProtocol(protocol);
			return fts;
		}catch(Exception e) {
			throw new IOException(e);
		}
	}

}
