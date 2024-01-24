package eu.unicore.xnjs.fts.impl;

import java.util.List;

import eu.unicore.security.Client;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.fts.FTSTransferInfo;
import eu.unicore.xnjs.fts.IFTSController;
import eu.unicore.xnjs.fts.SourceFileInfo;
import eu.unicore.xnjs.io.IFileTransfer;
import eu.unicore.xnjs.io.IFileTransfer.ImportPolicy;
import eu.unicore.xnjs.io.IFileTransfer.OverwritePolicy;
import eu.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.xnjs.io.impl.Inline;

/**
 * file import from inline data in the incoming job description
 *  
 * @author schuller
 */
public class InlineFTS implements IFTSController {
	
	private final String workingDirectory;
	private final Client client;
	private final XNJS xnjs;
	private final String target;
	private final String inlineData;
	private OverwritePolicy overwrite;
	
	private IStorageAdapter tsi;
		
	public InlineFTS(XNJS xnjs, Client client, String workingDirectory, String target, String data) {
		this.xnjs = xnjs;
		this.client = client;
		this.workingDirectory = workingDirectory;
		this.target = target;
		this.inlineData = data;
	}

	@Override
	public void setOverwritePolicy(OverwritePolicy overwrite) {
		this.overwrite = overwrite;
	}
	
	@Override
	public void setImportPolicy(ImportPolicy policy){
		// NOP
	}

	@Override
	public void setProtocol(String protocol){
		// NOP
	}

	@Override
	public void setStorageAdapter(IStorageAdapter adapter) {
		this.tsi = adapter;
	}

	@Override
	public long collectFilesForTransfer(List<FTSTransferInfo> fileList) throws Exception {
		SourceFileInfo sfi = new SourceFileInfo();
		sfi.setPath("inline://");
		sfi.setSize(inlineData.length());
		fileList.add(new FTSTransferInfo(sfi, target, false));
		return inlineData.length();
	}

	@Override
	public IFileTransfer createTransfer(SourceFileInfo from, String to) throws Exception {
		Inline inline = new Inline(xnjs,client, workingDirectory, target, inlineData);
		inline.setOverwritePolicy(overwrite);
		inline.setStorageAdapter(tsi);
		return inline;
	}

}
