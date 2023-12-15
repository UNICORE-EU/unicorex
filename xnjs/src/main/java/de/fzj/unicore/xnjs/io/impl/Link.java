package de.fzj.unicore.xnjs.io.impl;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.io.IFileTransfer;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import de.fzj.unicore.xnjs.io.TransferInfo;
import de.fzj.unicore.xnjs.io.TransferInfo.Status;
import de.fzj.unicore.xnjs.tsi.TSI;
import eu.unicore.security.Client;
import eu.unicore.util.Log;

/**
 * file "import" using a symbolic link
 *  
 * @author schuller
 */
public class Link implements IFileTransfer {
	
	private final String workingDirectory;
	private final Client client;
	private final XNJS configuration;
	private final TransferInfo info;
	
	public Link(XNJS configuration, Client client, String workingDirectory, String source, String target) {
		this.configuration=configuration;
		this.client=client;
		this.workingDirectory=workingDirectory;
		this.info = new TransferInfo(UUID.randomUUID().toString(), source, target);
		info.setProtocol("link");
	}
	
	public TransferInfo getInfo(){return info;}
	
	public long getDataSize() {
		return -1;
	}

	/**
	 * uses TSI link to create the link
	 */
	public void run() {
		try{
			TSI tsi=configuration.getTargetSystemInterface(client);
			tsi.setStorageRoot("/");
			String target = info.getSource();
			String linkName = workingDirectory+tsi.getFileSeparator()+info.getTarget();
			tsi.link(target,linkName);
			info.setStatus(Status.DONE);
		}catch(Exception ex){
			info.setStatus(Status.FAILED,Log.createFaultMessage("File transfer failed", ex));
		}
	}

	public void abort() {}

	public Map<String,Serializable>pause() {
		return null;
	}

	public void resume(Map<String,Serializable>state) {	
	}

	public void setOverwritePolicy(OverwritePolicy overwrite) {
	}
	
	public void setImportPolicy(ImportPolicy policy){
	}
	
	public void setStorageAdapter(IStorageAdapter adapter) {
	}
	
}
