package eu.unicore.uas.fts.http;

import java.util.Map;

import eu.unicore.services.ContainerProperties;
import eu.unicore.services.InitParameters;
import eu.unicore.services.messaging.PullPoint;
import eu.unicore.uas.fts.FileTransferImpl;
import eu.unicore.uas.fts.FileTransferModel;

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
		HttpFileTransferModel m = getModel();
		m.setClient(getClient());
		m.accessURL = makeAccessURL(getUniqueID());
		setOK();
	}

	public String getPath(){
		HttpFileTransferModel m = getModel();
		return m.getIsExport()? m.getSource(): m.getTarget();
	}

	protected String makeAccessURL(String uniqueID){
		ContainerProperties mainProps = getKernel().getContainerProperties(); 
		return mainProps.getContainerURL()+ "/rest/files/" + uniqueID;
	}

	@Override
	public void destroy() {
		kernel.getAttribute(FileAccessStatus.class).cleanup(getUniqueID());
		super.destroy();
	}

	@Override
	public Long getTransferredBytes() {
		Long t = kernel.getAttribute(FileAccessStatus.class).getTransferredBytes(getUniqueID());
		long tv = t!=null? t : 0l;
		getModel().setTransferredBytes(tv);
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
