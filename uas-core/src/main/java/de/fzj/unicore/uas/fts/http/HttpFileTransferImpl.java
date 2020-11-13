package de.fzj.unicore.uas.fts.http;

import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.uas.UASProperties;
import de.fzj.unicore.uas.ft.http.AccessURLDocument;
import de.fzj.unicore.uas.fts.FileTransferImpl;
import de.fzj.unicore.uas.fts.FiletransferInitParameters;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.wsrflite.ContainerProperties;
import de.fzj.unicore.wsrflite.InitParameters;
import de.fzj.unicore.wsrflite.messaging.PullPoint;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.ValueRenderer;

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
public class HttpFileTransferImpl extends FileTransferImpl{
	
	private static final Logger logger = LogUtil.getLogger(LogUtil.DATA,HttpFileTransferImpl.class);
	
	public final static QName RPAccessURL=AccessURLDocument.type.getDocumentElementName();

	private static final QName portType=new QName("http://unigrids.org/2006/04/services/bfts","BaselineFileTransferService");

	public HttpFileTransferImpl(){
		super();
		addRenderer(new ValueRenderer(this, AccessURLDocument.type.getDocumentElementName()){
			protected AccessURLDocument getValue(){
				AccessURLDocument urlDoc=AccessURLDocument.Factory.newInstance();
				urlDoc.setAccessURL(getModel().accessURL);
				return urlDoc;
			}
		});
	}
	
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
			setStatus(STATUS_FAILED, "Error initialising BFT filetransfer");
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
	
	public QName getPortType()
	{
		return portType;
	}

	/**
	 * handle (error) messages
	 */
	@Override
	public void processMessages(PullPoint messageIterator) {
		while(messageIterator.hasNext()){
			getModel().setDescription(messageIterator.next().getBody());
			getModel().setStatus(STATUS_FAILED);
		}
	}

}
