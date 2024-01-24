package eu.unicore.uas.fts.http;

import java.util.Map;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.ContainerProperties;
import eu.unicore.services.InitParameters;
import eu.unicore.services.messaging.PullPoint;
import eu.unicore.uas.UASProperties;
import eu.unicore.uas.fts.FileTransferImpl;
import eu.unicore.uas.fts.FileTransferModel;
import eu.unicore.uas.fts.FiletransferInitParameters;
import eu.unicore.uas.util.LogUtil;

/**
 * "Baseline" file transfer, which exposes a file using HTTP(s) via Jetty<br/>
 * 
 * The URI for download is exposed using a WSRF resource property.
 * 
 * Based on an idea by Roger Menday. <br/>
 * 
 * @author schuller
 * @since 1.0.1
 */
public class HttpFileTransferImpl extends FileTransferImpl {
	
	private static final Logger logger = LogUtil.getLogger(LogUtil.DATA,HttpFileTransferImpl.class);

	@Override
	public HttpFileTransferModel getModel(){
		return (HttpFileTransferModel)model;
	}
	
	@Override
	public void initialise(InitParameters map)throws Exception{
		if(model == null){
			model=new HttpFileTransferModel();
		}
		super.initialise(map);
		try{
			HttpFileTransferModel m = getModel();
			m.setClient(getClient());
			m.accessURL = makeAccessURL(getUniqueID());
			m.contentType = ((FiletransferInitParameters)map).mimetype;
			setOK();
		}catch(Exception e){
			LogUtil.logException("Error initialising BFT filetransfer",e,logger);
			setStatus(FileTransferModel.STATUS_FAILED, "Error initialising BFT filetransfer");
			throw e;
		}
	}

	public String getPath(){
		HttpFileTransferModel m = getModel();
		return m.getIsExport()? m.getSource(): m.getTarget();
	}

	protected String makeAccessURL(String uniqueID){
		ContainerProperties mainProps = getKernel().getContainerProperties(); 
		String base=mainProps.getBaseUrl();
		String add=base.replace("services", "files")+"/"+uniqueID;
		if(logger.isDebugEnabled())logger.debug("Enabling HTTP access on URL: "+add);
		if(uasProperties.getBooleanValue(UASProperties.FTS_HTTP_PREFER_POST)){
			add=add+"?method=POST";
		}
		return add;
	}
	
	@Override
	public void destroy() {
		FileServlet fs=kernel.getAttribute(FileServlet.class);
		fs.cleanup(getUniqueID());
		super.destroy();
	}
	
	@Override
	public Long getTransferredBytes() {
		HttpFileTransferModel model = getModel();
		String uid = getUniqueID();
		FileServlet fs=kernel.getAttribute(FileServlet.class);
		Long t=fs.getTransferredBytes(uid);
		if(t!=null){
			model.setTransferredBytes(t);
		}
		else{
			logger.debug("Can't get transferred bytes for transfer <"+uid+">");
			return 0l;
		}
		return t;
	}

	public Map<String,String>getProtocolDependentParameters(){
		Map<String,String> params = super.getProtocolDependentParameters();
		params.put("accessURL", getModel().getAccessURL());
		return params;
	}

	/**
	 * handle (error) messages
	 */
	@Override
	public void processMessages(PullPoint messageIterator) {
		while(messageIterator.hasNext()){
			getModel().setDescription(messageIterator.next().getBody());
			getModel().setStatus(FileTransferModel.STATUS_FAILED);
		}
	}

}
