package eu.unicore.uas.fts;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import eu.unicore.security.Client;
import eu.unicore.services.InitParameters;
import eu.unicore.uas.UAS;
import eu.unicore.uas.impl.BaseResourceImpl;
import eu.unicore.uas.util.LogUtil;
import eu.unicore.uas.xnjs.StorageAdapterFactory;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.xnjs.io.XnjsFile;

/**
 * A file transfer resource <br/>
 *
 * this class must be extended to support a specific protocol
 *
 * @author schuller
 */
public abstract class FileTransferImpl extends BaseResourceImpl implements DataResource {

	private static final Logger logger = LogUtil.getLogger(LogUtil.DATA,FileTransferImpl.class);

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
		String[] tags = map.initialTags;
		if(tags!=null && tags.length>0){
			m.getTags().addAll(Arrays.asList(tags));
		}
		logger.info("New file transfer: {}", toString());
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
		sb.append("<").append(getUniqueID()).append(">");
		sb.append(m.isExport? " export" : " import");
		if(m.getSource()!=null){
			sb.append(" from '").append(m.source).append("'");
		}
		if(m.getTarget()!=null){
			sb.append(" to '").append(m.target).append("'");
		}
		Client cl=getClient();
		if(cl!=null && cl.getDistinguishedName()!=null){
			sb.append(" for <").append(cl.getDistinguishedName()).append(">");
		}
		sb.append(" protocol=").append(m.protocol);
		sb.append(" overwrite=").append(m.overWrite);
		sb.append(" workdir=<").append(m.workdir).append(">");
		return sb.toString();
	}

}
