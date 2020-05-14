package de.fzj.unicore.uas.xtreemfs;

import java.net.URI;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.io.DataStageInInfo;
import de.fzj.unicore.xnjs.io.DataStageOutInfo;
import de.fzj.unicore.xnjs.io.IFileTransfer;
import de.fzj.unicore.xnjs.io.IFileTransferCreator;
import eu.unicore.security.Client;

public class XtreemFSFileTransferCreator implements IFileTransferCreator {

	private final XNJS configuration;
	
	public XtreemFSFileTransferCreator(XNJS configuration){
		this.configuration=configuration;
	}
	
	@Override
	public IFileTransfer createFileExport(Client client, String workdir, DataStageOutInfo info) {
		URI target = info.getTarget();
		String source = info.getFileName();
		if("xtreemfs".equalsIgnoreCase(target.getScheme())){
			return new XtreemFSUpload(configuration,client,workdir,source,target);
		}
		return null;
	}

	@Override
	public IFileTransfer createFileImport(Client client, String workdir, DataStageInInfo info) {
		URI source = info.getSources()[0];
		String target = info.getFileName();
		if("xtreemfs".equalsIgnoreCase(source.getScheme())){
			return new XtreemFSDownload(configuration,client,workdir,source,target);
		}
		return null;
	}

	@Override
	public String getProtocol() {
		return "xtreemfs";
	}
	
	@Override
	public String getStageOutProtocol() {
		return getProtocol();
	}
	
	@Override
	public String getStageInProtocol() {
		return getProtocol();
	}

}
