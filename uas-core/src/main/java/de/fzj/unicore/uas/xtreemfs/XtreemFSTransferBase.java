package de.fzj.unicore.uas.xtreemfs;

import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.client.StorageClient;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.security.ETDAssertionForwarding;
import de.fzj.unicore.wsrflite.utils.Utilities;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.io.IFileTransfer;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import de.fzj.unicore.xnjs.io.OptionNotSupportedException;
import de.fzj.unicore.xnjs.io.TransferInfo;
import de.fzj.unicore.xnjs.tsi.TSI;
import eu.unicore.security.Client;
import eu.unicore.util.httpclient.IClientConfiguration;

public abstract class XtreemFSTransferBase implements IFileTransfer, Constants{
	
	protected final XNJS configuration;
	protected final Client client;
	protected OverwritePolicy overwrite;
	protected volatile boolean aborted=false;
	protected final Kernel kernel;
	protected final XtreemProperties xtreemProperties;
	
	protected final TransferInfo info;
	
	public XtreemFSTransferBase(XNJS configuration, Client client){
		this.configuration=configuration;
		this.client=client;
		this.kernel = configuration.get(Kernel.class);
		xtreemProperties = kernel.getAttribute(XtreemProperties.class);
		this.info = new TransferInfo(Utilities.newUniqueID(), null, null);
		this.info.setProtocol("xtreemfs");
	}
	
	@Override
	public TransferInfo getInfo(){
		return info;
	}
	
	@Override
	public void abort(){
		aborted=true;
	}

	protected void ensureDirectoriesExist(String path)throws Exception{
		String dir=path.substring(0,path.lastIndexOf("/"));
		TSI tsi=configuration.getTargetSystemInterface(client);
		tsi.mkdir(dir);
	}
	
	protected StorageClient createStorageClient()throws Exception{
		IClientConfiguration sec=kernel.getClientConfiguration().clone();
		ETDAssertionForwarding.configureETD(client, sec);
		String address=xtreemProperties.getValue(XtreemProperties.XTREEMFS_REMOTE_URL);
		if(address==null){
			//TODO check for an XtreemFS SMS in the external registry
			throw new IllegalStateException("No SMS for remote XtreemFS access is defined!");
		}
		EndpointReferenceType epr=EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue(address);
		StorageClient sms=new StorageClient(epr,sec);
		return sms;
	}
	
	@Override
	public void setOverwritePolicy(OverwritePolicy overwrite)
			throws OptionNotSupportedException {
		this.overwrite=overwrite;
	}

	@Override
	public void setStorageAdapter(IStorageAdapter adapter) {

	}

	@Override
	public void setImportPolicy(ImportPolicy policy) {
		// NOP
	}
	
	
}
