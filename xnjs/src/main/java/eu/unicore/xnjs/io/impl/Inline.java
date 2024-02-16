package eu.unicore.xnjs.io.impl;

import java.io.OutputStreamWriter;

import eu.unicore.persist.util.UUID;
import eu.unicore.security.Client;
import eu.unicore.util.Log;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.io.ChangePermissions;
import eu.unicore.xnjs.io.IFileTransfer;
import eu.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.xnjs.io.TransferInfo;
import eu.unicore.xnjs.io.TransferInfo.Status;
import eu.unicore.xnjs.io.XnjsFile;

/**
 * file import from inline data in the incoming XML
 *  
 * @author schuller
 */
public class Inline implements IFileTransfer {
	
	private final String workingDirectory;
	private final Client client;
	private final XNJS configuration;
	private OverwritePolicy overwrite;
	private final String inlineData;
	private String umask = null;
	private String permissions = null;

	private final TransferInfo info;

	public Inline(XNJS configuration, Client client, String workingDirectory, String target, String data) {
		this.configuration=configuration;
		this.client=client;
		this.workingDirectory=workingDirectory;
		this.info = new TransferInfo(UUID.newUniqueID(), "inline://", target);
		info.setProtocol("inline");
		this.inlineData = data;
		if(data!=null)info.setDataSize(inlineData.length());
	}

	@Override
	public TransferInfo getInfo(){
		return info;
	}

	@Override
	public void setUmask(String umask) {
		this.umask = umask;
	}

	/**
	 * uses TSI link to write inline data to the target
	 */
	public void run() {
		info.setStatus(Status.RUNNING);
		if(tsi == null){
			tsi=configuration.getTargetSystemInterface(client);
			if(umask!=null)tsi.setUmask(umask);
		}
		tsi.setStorageRoot(workingDirectory);
		boolean append = OverwritePolicy.APPEND.equals(overwrite);
		try(OutputStreamWriter os = getTarget(info.getTarget(),append)){
			os.write(inlineData);
			os.flush(); // flush here to avoid possible race condition
			info.setTransferredBytes(inlineData.length());
			info.setStatus(Status.DONE);
		}catch(Exception ex){
			info.setStatus(Status.FAILED, Log.createFaultMessage("Inline import to '"
					+ workingDirectory+"/"+info.getTarget() + "' failed", ex));
		}
		if(permissions!=null) {
			try{
				tsi.chmod2(info.getTarget(), ChangePermissions.getChangePermissions(permissions), false);
			}catch(Exception ex) {
				info.setStatus(Status.FAILED, Log.createFaultMessage("Setting permissions of '"
						+ workingDirectory+"/"+info.getTarget() + "' to '"+permissions+"' failed", ex));
			}
		}
	}
	
	@Override
	public void abort(){}
	
	@Override
	public void setOverwritePolicy(OverwritePolicy overwrite) {
		this.overwrite = overwrite;
	}
	
	@Override
	public void setImportPolicy(ImportPolicy policy){
		// NOP
	}

	private IStorageAdapter tsi;
	
	@Override
	public void setStorageAdapter(IStorageAdapter adapter) {
		this.tsi = adapter;
	}

	@Override
	public void setPermissions(String permissions) {
		this.permissions = permissions;
	}

	private OutputStreamWriter getTarget(String target, boolean append)throws Exception{
		String s = getParentOfLocalFilePath(target);
		XnjsFile parent=tsi.getProperties(s);
		if(parent==null){
			tsi.mkdir(s);
		}
		else if(!parent.isDirectory()){
			throw new ExecutionException("Parent <"+s+"> is not a directory");
		}
		return new OutputStreamWriter(tsi.getOutputStream(info.getTarget(),append),"UTF-8");
	}

	private String getParentOfLocalFilePath(String file){
		String result = file.replaceAll("/+","/").
				replace("\\/","/").
				replace("/", tsi.getFileSeparator());
		int i=result.lastIndexOf(tsi.getFileSeparator());
		return i>0 ? result.substring(0,i) : "/" ;
	}

}
