package eu.unicore.xnjs.io.impl;

import java.net.URI;

import javax.inject.Inject;

import eu.unicore.security.Client;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.fts.IFTSController;
import eu.unicore.xnjs.fts.impl.InlineFTS;
import eu.unicore.xnjs.io.DataStageInInfo;
import eu.unicore.xnjs.io.DataStageOutInfo;
import eu.unicore.xnjs.io.DataStagingCredentials;
import eu.unicore.xnjs.io.IFileTransfer;
import eu.unicore.xnjs.io.IFileTransferCreator;
import eu.unicore.xnjs.io.git.GitStageIn;

/**
 * Creates transfers for the following mechanisms
 * FTP, GSIFTP, SCP, HTTP(s), file
 * 
 * @author schuller
 */
public class DefaultTransferCreator implements IFileTransferCreator {
	
	private final XNJS configuration;
	
	@Inject
	public DefaultTransferCreator (XNJS config){
		this.configuration=config;
	}

	@Override
	public String getProtocol() {
		return "ftp, gsiftp, scp, http, https, file, link, inline, git";
	}
	
	@Override
	public String getStageInProtocol() {
		return "ftp, gsiftp, scp, http, https, file, link, inline, git";
	}

	@Override
	public String getStageOutProtocol() {
		return "ftp, gsiftp, scp, http, https, file";
	}

	@Override
	public IFileTransfer createFileExport(Client client, String workingDirectory, DataStageOutInfo info) {
		URI target = info.getTarget();
		String scheme = target.getScheme();
		String source = info.getFileName();
		DataStagingCredentials credentials = info.getCredentials();
		IFileTransfer f = null;
		if("ftp".equalsIgnoreCase(scheme)){
			f = new FTPUpload(client,workingDirectory,source,target,configuration,credentials);
		}
		if("gsiftp".equalsIgnoreCase(scheme)){
			f = new GSIFTPUpload(client,workingDirectory,source,target,configuration);
		}
		if("scp".equalsIgnoreCase(scheme)){
			f = new ScpUpload(client,workingDirectory,source,target,configuration,credentials);
		}
		if("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)){
			f = new HTTPFileUpload(client, workingDirectory, source, target.toString(), configuration,credentials);
		}
		if("file".equalsIgnoreCase(scheme)){
			f = new FileCopy(configuration, client, workingDirectory, source,target.getRawPath(), false);
		}
		if(f!=null){
			f.setOverwritePolicy(info.getOverwritePolicy());
			if(info.getExtraParameters()!=null){
				f.setExtraParameters(info.getExtraParameters());
			}
		}
		return f;
	}
	
	@Override
	public IFileTransfer createFileImport(Client client, String workingDirectory,  DataStageInInfo info) {
		URI source = info.getSources()[0]; // TODO
		String scheme = source.getScheme();
		String target = info.getFileName();
		DataStagingCredentials credentials = info.getCredentials();
		IFileTransfer f = null;
		if("ftp".equalsIgnoreCase(scheme)){
			f = new FTPDownload(client,workingDirectory,source,target,configuration,credentials);
		}
		if("gsiftp".equalsIgnoreCase(scheme)){
			f = new GSIFTPDownload(client,workingDirectory,source,target,configuration);
		}
		if("scp".equalsIgnoreCase(scheme)){
			f = new ScpDownload(client,workingDirectory,source,target,configuration,credentials);
		}
		if("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)){
			f = new HTTPFileDownload(client, workingDirectory, source.toString(), target, configuration,credentials);
		}
		if("file".equalsIgnoreCase(scheme)){
			return new FileCopy(configuration, client, workingDirectory, source.getRawPath(),target, true);
		}
		if("link".equalsIgnoreCase(scheme)){
			f = new Link(configuration, client, workingDirectory, source.getSchemeSpecificPart(),target);
		}
		if("inline".equalsIgnoreCase(scheme)){
			f= new Inline(configuration, client, workingDirectory, target, info.getInlineData());
		}
		if("git".equalsIgnoreCase(scheme)) {
			f = new GitStageIn(configuration, client,workingDirectory, source.getSchemeSpecificPart(),
					target, credentials);
		}
		if(f!=null) {
			f.setOverwritePolicy(info.getOverwritePolicy());
			f.setImportPolicy(info.getImportPolicy());
			if(info.getExtraParameters()!=null){
				f.setExtraParameters(info.getExtraParameters());
			}
		}
		return f;
	}
	
	@Override
	public IFTSController createFTSImport(Client client, String workingDirectory,  DataStageInInfo info) {
		URI source = info.getSources()[0]; // TODO
		String scheme = source.getScheme();
		String target = info.getFileName();
		IFTSController f = null;
		if("inline".equalsIgnoreCase(scheme)){
			f = new InlineFTS(configuration, client, workingDirectory, target, info.getInlineData());
		}
		else if(scheme.toLowerCase().startsWith("http")) {
			f = new HttpImportsController(configuration, client, info, workingDirectory);
		}
		if(f!=null){
			f.setOverwritePolicy(info.getOverwritePolicy());
			f.setImportPolicy(info.getImportPolicy());
			if(info.getExtraParameters()!=null) {
				f.setExtraParameters(info.getExtraParameters());
			}
		}
		return f;
	}
	
	@Override
	public IFTSController createFTSExport(Client client, String workingDirectory,  DataStageOutInfo info) {
		URI target = info.getTarget();
		String scheme = target.getScheme();
		IFTSController f = null;
		if(scheme.toLowerCase().startsWith("http")) {
			f = new HttpExportsController(configuration, client, info, workingDirectory);
		}
		if(f!=null){
			f.setOverwritePolicy(info.getOverwritePolicy());
			if(info.getExtraParameters()!=null) {
				f.setExtraParameters(info.getExtraParameters());
			}
		}
		return f;
	}
}
