package eu.unicore.uas.fts.http;

import java.util.Map;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.ContainerProperties;
import eu.unicore.services.InitParameters;
import eu.unicore.services.messaging.PullPoint;
import eu.unicore.uas.UASProperties;
import eu.unicore.uas.fts.FileTransferImpl;
import eu.unicore.uas.fts.FileTransferModel;
import eu.unicore.uas.util.LogUtil;

/**
 * "Baseline" file transfer, which provides access to a file via the HTTP(s) transport<br/>
 * 
 * The URI for download is accessible as a resource property.
 * 
 * Based on an idea by Roger Menday. <br/>
 *
 * @author schuller
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
		String ep = mainProps.getContainerURL()+ "/files/" + uniqueID;
		if(uasProperties.getBooleanValue(UASProperties.FTS_HTTP_PREFER_POST)){
			ep = ep + "?method=POST";
		}
		return ep;
	}

	@Override
	public void destroy() {
		kernel.getAttribute(FileServlet.class).cleanup(getUniqueID());
		super.destroy();
	}

	@Override
	public Long getTransferredBytes() {
		HttpFileTransferModel model = getModel();
		String uid = getUniqueID();
		Long t = kernel.getAttribute(FileServlet.class).getTransferredBytes(uid);
		long tv = t!=null? t : 0l;
		model.setTransferredBytes(tv);
		return tv;
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
			getModel().setDescription(messageIterator.next().toString());
			getModel().setStatus(FileTransferModel.STATUS_FAILED);
		}
	}

}
