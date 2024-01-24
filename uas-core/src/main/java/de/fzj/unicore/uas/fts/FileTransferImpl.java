package de.fzj.unicore.uas.fts;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.impl.BaseResourceImpl;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.uas.xnjs.StorageAdapterFactory;
import eu.unicore.security.Client;
import eu.unicore.services.InitParameters;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.xnjs.io.XnjsFile;

/**
 * a WS-Resource representing a File transfer<br/>
 * 
 * this class must be extended to support a specific protocol
 *  
 * @author schuller
 */
public abstract class FileTransferImpl extends BaseResourceImpl implements DataResource {

	private static final Logger logger = LogUtil.getLogger(LogUtil.DATA,FileTransferImpl.class);

	/**
	 * Configuration key: maps protocol to implementation class.
	 * For example, the config entry 
	 * "uas.filetransfer.protocol.HTTP=my.class.for.http" 
	 * will map the given class to the HTTP protocol.
	 * The class must extend {@link FileTransferImpl}
	 */
	public static final String CONFIG_PROTOCOL_KEY="uas.filetransfer.protocol.";

	public FileTransferImpl(){
		super();
	}

	@Override 
	public FileTransferModel getModel(){
		return (FileTransferModel)model;
	}
	
	@Override
	public void initialise(InitParameters initP) throws Exception{
		if(model==null){
			setModel(new FileTransferModel());
		}
		FiletransferInitParameters map = (FiletransferInitParameters)initP;
		
		FileTransferModel m = getModel();
		
		super.initialise(map);
		String rawsource=map.source;
		String rawtarget=map.target;
		m.protocol = map.protocol;
		String parentID = map.smsUUID;
		m.setParentUID(parentID);
		m.setParentServiceName(UAS.SMS);
		m.serviceSpec = parentID;
		m.workdir = map.workdir;
		m.isExport = map.isExport;
		m.overWrite = map.overwrite;
		m.umask = map.umask;
		m.numberOfBytes = map.numbytes;
		m.extraParameters = map.extraParameters;
		initialiseSourceAndTarget(rawsource, rawtarget);
		m.setStorageAdapterFactory(map.storageAdapterFactory);
		logger.info("New file transfer: "+toString());
	}

	/**
	 * setup the source and target of this transfer<br/>
	 * 
	 * @param rawsource
	 * @param rawtarget
	 */
	protected void initialiseSourceAndTarget(String rawsource, String rawtarget){
		if(rawsource!=null){
			getModel().source = rawsource;
		}
		if(rawtarget!=null){
			getModel().target = rawtarget;
		}
	}

	public IStorageAdapter getStorageAdapter()throws IOException{
		FileTransferModel m = getModel();
		StorageAdapterFactory factory=m.getStorageAdapterFactory();
		if(factory!=null){
			IStorageAdapter adapter = factory.createStorageAdapter(this);
			adapter.setUmask(m.getUmask());
			adapter.setStorageRoot(m.getWorkdir());
			return adapter;
		}
		else return null;
	}

	public long getDataSize(){
		String source = getModel().getSource();
		String filename=source!=null?source:getModel().getTarget();
		try{
			XnjsFile f=getStorageAdapter().getProperties(filename);
			if(f!=null){
				return f.getSize();
			}
		}catch(Exception e){
			LogUtil.logException("Could not determine file size for <"+filename+">",e,logger);
		}
		return 0L;
	}

	public Long getTransferredBytes() {
		return getModel().transferredBytes;
	}
	
	/**
	 * collect protocol dependent parameters (e.g. HTTP access URL)
	 * for (RESTful) rendering and returning to the client. The base implementation
	 * returns an empty map
	 */
	public Map<String,String>getProtocolDependentParameters(){
		Map<String,String> params = new HashMap<String, String>();
		return params;
	}
	
	/**
	 * create an input stream for reading from the backend storage
	 * @throws ExecutionException
	 */
	protected InputStream createNewInputStream()throws IOException, ExecutionException{
		return getStorageAdapter().getInputStream(getModel().getSource());
	}

	/**
	 * create an output stream for writing to the backend storage
	 * @throws IOException, ExecutionException
	 */
	protected OutputStream createNewOutputStream(boolean append)throws IOException, ExecutionException{
		return getStorageAdapter().getOutputStream(getModel().getTarget(),append);
	}

	/**
	 * encode some characters that are illegal in URIs
	 * @param orig
	 * @return encoded string
	 */
	public static String urlEncode(String orig){
		try{
			return orig.replaceAll(" ", "%20");
		}catch(Exception e){
			logger.error(e);
			return orig;
		}
	}

	protected URI toURI(String path)throws URISyntaxException{
		return new URI(path);
	}

	protected void setStatus(int status, String description){
		FileTransferModel m = getModel();
		m.status = status;
		m.description = description;
	}

	protected void setReady(){
		setStatus(FileTransferModel.STATUS_READY, "Ready.");;
	}

	protected void setOK(){
		setStatus(FileTransferModel.STATUS_RUNNING,"OK.");
	}

	@Override
	public String toString(){
		FileTransferModel m = getModel();
		StringBuilder sb=new StringBuilder();
		if(m.getSource()!=null){
			sb.append(" from '").append(m.source).append("'");
		}
		if(m.getTarget()!=null){
			sb.append(" to '").append(m.target).append("'");
		}

		Client cl=getClient();
		
		if(cl!=null && cl.getDistinguishedName()!=null){
			sb.append(" for ").append(cl.getDistinguishedName());
		}
		sb.append(" protocol=").append(m.protocol);
		sb.append(" isExport=").append(m.isExport);
		sb.append(" overwrite=").append(m.overWrite);
		sb.append(" workdir=").append(m.workdir);
		sb.append(" myID=").append(getUniqueID());

		return sb.toString();
	}

}
