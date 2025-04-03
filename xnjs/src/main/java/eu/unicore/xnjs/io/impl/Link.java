package eu.unicore.xnjs.io.impl;

import java.io.Serializable;
import java.util.Map;

import eu.unicore.persist.util.UUID;
import eu.unicore.security.Client;
import eu.unicore.util.Log;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.io.IFileTransfer;
import eu.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.xnjs.io.TransferInfo;
import eu.unicore.xnjs.io.TransferInfo.Status;
import eu.unicore.xnjs.tsi.TSI;

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
	private String preferredLoginNode;

	public Link(XNJS configuration, Client client, String workingDirectory, String source, String target) {
		this.configuration=configuration;
		this.client=client;
		this.workingDirectory=workingDirectory;
		this.info = new TransferInfo(UUID.newUniqueID(), source, target);
		info.setProtocol("link");
	}
	
	@Override
	public TransferInfo getInfo(){
		return info;
	}

	@Override
	public void run() {
		try{
			TSI tsi=configuration.getTargetSystemInterface(client, preferredLoginNode);
			tsi.setStorageRoot("/");
			String target = info.getSource();
			String linkName = workingDirectory+tsi.getFileSeparator()+info.getTarget();
			tsi.link(target,linkName);
			info.setStatus(Status.DONE);
		}catch(Exception ex){
			info.setStatus(Status.FAILED,Log.createFaultMessage("File transfer failed", ex));
		}
	}

	@Override
	public void setPreferredLoginNode(String loginNode) {
		this.preferredLoginNode = loginNode;
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
