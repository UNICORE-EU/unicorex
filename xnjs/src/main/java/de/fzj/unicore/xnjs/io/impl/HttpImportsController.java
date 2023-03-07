package de.fzj.unicore.xnjs.io.impl;

import java.util.List;
import java.util.Map;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.fts.FTSTransferInfo;
import de.fzj.unicore.xnjs.fts.IFTSController;
import de.fzj.unicore.xnjs.fts.SourceFileInfo;
import de.fzj.unicore.xnjs.io.DataStageInInfo;
import de.fzj.unicore.xnjs.io.FileSet;
import de.fzj.unicore.xnjs.io.IFileTransfer;
import de.fzj.unicore.xnjs.io.IFileTransfer.ImportPolicy;
import de.fzj.unicore.xnjs.io.IFileTransfer.OverwritePolicy;
import de.fzj.unicore.xnjs.io.IFileTransferEngine;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import de.fzj.unicore.xnjs.io.OptionNotSupportedException;
import eu.unicore.security.Client;

public class HttpImportsController implements IFTSController {

	protected IStorageAdapter localStorage;

	protected final Client client;
	
	protected final XNJS xnjs;

	protected DataStageInInfo dsi;

	protected OverwritePolicy overwritePolicy;
	
	protected ImportPolicy importPolicy;
	
	protected Map<String,String> extraParameters;

	protected final String source;
	
	protected FileSet sourceFileSet;

	protected final String workingDirectory;

	protected String protocol;

	public HttpImportsController(XNJS xnjs, Client client, DataStageInInfo dsi, String workingDirectory) {
		this.xnjs = xnjs;
		this.client = client;
		this.dsi = dsi;
		this.source = dsi.getSources()[0].toString();
		this.workingDirectory = workingDirectory;
	}

	@Override
	public void setStorageAdapter(IStorageAdapter storageAdapter) {
		this.localStorage = storageAdapter;
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

	@Override
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	@Override
	public long collectFilesForTransfer(List<FTSTransferInfo> fileList) throws Exception {
		SourceFileInfo sfi = new SourceFileInfo();
		sfi.setPath(source);
		sfi.setSize(1024); // arbitrary since we can't get the real value from a plain http server
		fileList.add(new FTSTransferInfo(sfi, dsi.getFileName(), false));
		return sfi.getSize();
	}
	
	@Override
	public IFileTransfer createTransfer(SourceFileInfo from, String to) throws Exception {
		DataStageInInfo info = dsi.clone();
		return xnjs.get(IFileTransferEngine.class).createFileImport(client, workingDirectory, info);
	}

}
