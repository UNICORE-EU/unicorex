package de.fzj.unicore.xnjs.io.impl;

import java.net.URI;

import javax.inject.Inject;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.fts.IFTSController;
import de.fzj.unicore.xnjs.fts.impl.InlineFTS;
import de.fzj.unicore.xnjs.io.DataStageInInfo;
import de.fzj.unicore.xnjs.io.DataStageOutInfo;
import de.fzj.unicore.xnjs.io.DataStagingCredentials;
import de.fzj.unicore.xnjs.io.IFileTransfer;
import de.fzj.unicore.xnjs.io.IFileTransferCreator;
import de.fzj.unicore.xnjs.io.git.GitStageIn;
import eu.unicore.security.Client;

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
		
		if("ftp".equalsIgnoreCase(scheme)){
			return new FTPUpload(client,workingDirectory,source,target,configuration,credentials);
		}
		if("gsiftp".equalsIgnoreCase(scheme)){
			return new GSIFTPUpload(client,workingDirectory,source,target,configuration);
		}
		if("scp".equalsIgnoreCase(scheme)){
			return new ScpUpload(client,workingDirectory,source,target,configuration,credentials);
		}
		if("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)){
			return new HTTPFileUpload(client, workingDirectory, source, target.toString(), configuration,credentials);
		}
		if("file".equalsIgnoreCase(scheme)){
			return new FileCopy(configuration, client, workingDirectory, source,target.getRawPath(), false);
		}
		return null;
	}
	
	@Override
	public IFileTransfer createFileImport(Client client, String workingDirectory,  DataStageInInfo info) {
		URI source = info.getSources()[0]; // TODO
		String scheme = source.getScheme();
		String target = info.getFileName();
		DataStagingCredentials credentials = info.getCredentials();
		if("ftp".equalsIgnoreCase(scheme)){
			return new FTPDownload(client,workingDirectory,source,target,configuration,credentials);
		}
		if("gsiftp".equalsIgnoreCase(scheme)){
			return new GSIFTPDownload(client,workingDirectory,source,target,configuration);
		}
		if("scp".equalsIgnoreCase(scheme)){
			return new ScpDownload(client,workingDirectory,source,target,configuration,credentials);
		}
		if("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)){
			return new HTTPFileDownload(client, workingDirectory, source.toString(), target, configuration,credentials);
		}
		if("file".equalsIgnoreCase(scheme)){
			return new FileCopy(configuration, client, workingDirectory, source.getRawPath(),target, true);
		}
		if("link".equalsIgnoreCase(scheme)){
			return new Link(configuration, client, workingDirectory, source.getSchemeSpecificPart(),target);
		}
		if("inline".equalsIgnoreCase(scheme)){
			Inline ft = new Inline(configuration, client, workingDirectory,target);
			ft.setInlineData(info.getInlineData());
			return ft;
		}
		if("git".equalsIgnoreCase(scheme)) {
			return new GitStageIn(configuration, client,workingDirectory, source.getSchemeSpecificPart(),
					target, credentials);
		}
		return null;
	}
	
	@Override
	public IFTSController createFTSImport(Client client, String workingDirectory,  DataStageInInfo info) {
		URI source = info.getSources()[0]; // TODO
		String scheme = source.getScheme();
		String target = info.getFileName();
		if("inline".equalsIgnoreCase(scheme)){
			InlineFTS ft = new InlineFTS(configuration, client, workingDirectory,target);
			ft.setInlineData(info.getInlineData());
			return ft;
		}
		else if(scheme.toLowerCase().startsWith("http")) {
			return new HttpImportsController(configuration, client, info, workingDirectory);
		}
		return null;
	}
	
	@Override
	public IFTSController createFTSExport(Client client, String workingDirectory,  DataStageOutInfo info) {
		URI target = info.getTarget();
		String scheme = target.getScheme();
		if(scheme.toLowerCase().startsWith("http")) {
			return new HttpExportsController(configuration, client, info, workingDirectory);
		}
		return null;
	}
}
