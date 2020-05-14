package de.fzj.unicore.uas.fts;

import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.unigrids.services.atomic.types.ProtocolType;
import org.unigrids.x2006.x04.services.fts.ScheduledStartTimeDocument;

import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.uas.xnjs.U6FileTransferBase;
import de.fzj.unicore.wsrflite.InitParameters;
import de.fzj.unicore.wsrflite.messaging.ResourceDeletedMessage;
import de.fzj.unicore.wsrflite.security.util.AuthZAttributeStore;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.ValueRenderer;
import de.fzj.unicore.xnjs.io.DataStageInInfo;
import de.fzj.unicore.xnjs.io.DataStageOutInfo;
import de.fzj.unicore.xnjs.io.IFileTransfer;
import de.fzj.unicore.xnjs.io.IFileTransfer.OverwritePolicy;
import de.fzj.unicore.xnjs.io.IFileTransferEngine;
import de.fzj.unicore.xnjs.io.TransferInfo;
import de.fzj.unicore.xnjs.io.TransferInfo.Status;
import eu.unicore.security.Client;
import eu.unicore.services.ws.utils.WSServerUtilities;
import eu.unicore.util.Log;


/**
 * WS-Resource for initiating and monitoring
 * a server-to-server file transfer
 * 
 * the source parameter is a UNICORE6 URI 
 * the target is the local file (relative to storage root)
 * 
 * @author schuller
 */
public class ServerToServerFileTransferImpl extends FileTransferImpl {

	private static final Logger logger = LogUtil.getLogger(LogUtil.DATA,ServerToServerFileTransferImpl.class);

	public static final String PARAM_SCHEDULED_START="scheduledStartTime";

	public ServerToServerFileTransferImpl(){
		super();
		addRenderer(new TransferRateResourceProperty(this));
		addRenderer(new ValueRenderer(this, ScheduledStartTimeDocument.type.getDocumentElementName()) {
			@Override
			protected ScheduledStartTimeDocument getValue() throws Exception {
				long scheduledStartTime = getModel().scheduledStartTime;
				if(scheduledStartTime==0)return null;
				ScheduledStartTimeDocument res=ScheduledStartTimeDocument.Factory.newInstance();
				Calendar cal=Calendar.getInstance();
				cal.setTimeInMillis(scheduledStartTime);
				res.setScheduledStartTime(cal);
				return res;
			}
		});
	}
	

	@Override 
	public ServerToServerTransferModel getModel(){
		return (ServerToServerTransferModel)super.getModel();
	}
	
	@Override
	public void initialise(InitParameters map) throws Exception {
		if(model==null){
			setModel(new ServerToServerTransferModel());
		}
		ServerToServerTransferModel m = getModel();
		
		super.initialise(map);
		Map<String,String>extraParameters = m.extraParameters;
		String startTime=extraParameters!=null ? extraParameters.get(PARAM_SCHEDULED_START) : null;
		if(startTime!=null){
			try{
				m.scheduledStartTime = parseDate(startTime);
				logger.info("Filetransfer scheduled to start at "+startTime);
			}catch(IllegalArgumentException ex){
				throw new Exception("Could not parse supplied scheduledStartTime as a date",ex);
			}
		}
		m.client = AuthZAttributeStore.getClient();
		m.overWrite = true;
		IFileTransfer ft = createTransfer();
		startFileTransfer(ft, false);
	}
	

	@Override
	public void customPostActivate() {
		updateStatus();
	}

	protected void updateStatus() {
		TransferInfo ft = getInfo();
		if(ft!=null){
			Status s=ft.getStatus();
			if(s.equals(Status.FAILED)){
				setStatus(STATUS_FAILED,ft.getStatusMessage());
			}
			else if(s.equals(Status.DONE)){
				setStatus(STATUS_DONE,"File transfer done.");
			}
			else { //still running
				setOK();
			}
		}
	}

	/**
	 * check if the filetransfer should be restarted, and tries to restart
	 * 
	 * TODO
	 */
	protected void checkRestart(){
		updateStatus();
		ServerToServerTransferModel m = getModel();
		
		if(!m.isFinished()){
			Client oldClient = AuthZAttributeStore.getClient();
			Client client = m.client;
			logger.info("Attempting to resume file transfer "+toString()+
					(client!=null?" for "+client.getDistinguishedName():""));
			try{
				//on restart, must use stored client
				AuthZAttributeStore.setClient(client);
				IFileTransfer ft = createTransfer();
				startFileTransfer(ft, true);
			}catch(Exception ex){
				setStatus(STATUS_FAILED, "Failed (during restart, error message: "+ex.getMessage()+")");
				Log.logException("Attempt to restart server-to-server file transfer "+getUniqueID()+" failed", ex);
			}
			finally{
				AuthZAttributeStore.setClient(oldClient);
			}
		}
	}

	@Override
	public void destroy() {
		ServerToServerTransferModel model = getModel();
		try{
			if(!model.isFinished()){
				logger.info("Aborting filetransfer "+getUniqueID()+" for client "+getClient().getDistinguishedName());
				String id = model.getFileTransferUID();
				IFileTransferEngine fte = getFileTransferEngine();
				fte.abort(id);
				fte.cleanup(id);
			}
		}
		catch(Exception e){
			LogUtil.logException("Problem cleaning up filetransfer.",e,logger);
		}
		//notify parent SMS
		try{
			ResourceDeletedMessage m=new ResourceDeletedMessage("deleted:"+getUniqueID());
			m.setServiceName(getServiceName());
			m.setDeletedResource(getUniqueID());
			getKernel().getMessaging().getChannel(WSServerUtilities.extractResourceID(model.serviceSpec))
				.publish(m);
		}
		catch(Exception ex){
			LogUtil.logException("Problem notifying parent SMS.",ex,logger);
		}
		super.destroy();
	}

	@Override
	public long getDataSize(){
		TransferInfo ft = getInfo();
		return ft!=null ? ft.getDataSize() : -1; 
	}

	/**
	 * get the current transfer rate in bytes per second
	 * TODO
	 */
	public long getTransferRate(){
		TransferInfo ft = getInfo();
		if(ft!=null){
//			long dataSize = getDataSize();
//			long consumedMillis=u6ft.getElapsedTime();
//			if(dataSize>0 && consumedMillis>0){
//				//need bytes per second
//				return 1000*dataSize/consumedMillis;
//			}
		}
		return -1;
	}
	
	public Status getStatus(){
		TransferInfo ft = getInfo();
		if(ft != null){
			return ft.getStatus();
		}
		return Status.DONE;
	}
	
	public String getFiletransferStatusMessage(){
		TransferInfo ft = getInfo();
		if(ft != null){
			return ft.getStatusMessage();
		}
		return "N/A";
	}
	
	
	@Override
	public Long getTransferredBytes() {
		TransferInfo ft = getInfo();
		if(ft != null){
			return ft.getTransferredBytes();
		}
		return super.getTransferredBytes();
	}
	
	IFileTransferEngine fte;
	
	protected IFileTransferEngine getFileTransferEngine(){
		if(fte==null){
			fte = getXNJSFacade().getXNJS().get(IFileTransferEngine.class);
		}
		return fte;
	}
	
	protected TransferInfo getInfo(){
		ServerToServerTransferModel model = getModel();
		String ftUID = model.getFileTransferUID();
		return getFileTransferEngine().getInfo(ftUID);
	}
	
	/**
	 * create, but not yet start file transfer
	 * @param policy - {@link OverwritePolicy}
	 * @throws Exception
	 */
	protected IFileTransfer createTransfer()throws Exception{
		ServerToServerTransferModel model = getModel();
		IFileTransfer ft=null;
		OverwritePolicy policy = OverwritePolicy.OVERWRITE;
		if(!model.getIsExport()){
			DataStageInInfo info = new DataStageInInfo();
			info.setFileName(model.target);
			info.setOverwritePolicy(policy);
			info.setSources(new URI[]{toURI(urlEncode(model.source))});
			ft = getFileTransferEngine().
					createFileImport(
							model.client,
							model.workdir,
							info);
		}
		else{
			DataStageOutInfo info = new DataStageOutInfo();
			info.setFileName(model.source);
			info.setOverwritePolicy(policy);
			info.setTarget(toURI(urlEncode(model.target)));
			ft = getFileTransferEngine().
					createFileExport(
							model.client,
							model.workdir,
							info);
		}
		if(ft instanceof U6FileTransferBase){
			((U6FileTransferBase)ft).setStorageAdapter(getStorageAdapter());
			((U6FileTransferBase)ft).setExtraParameters(model.getExtraParameters());
			((U6FileTransferBase)ft).setStatusTracker(new StatusTracker(home,getUniqueID()));
		}
		//set the actual protocol
		ProtocolType.Enum protocol=ProtocolType.Enum.forString(ft.getInfo().getProtocol());
		if(protocol==null)protocol=ProtocolType.OTHER;
		model.protocol = protocol;
		model.fileTransferUID = ft.getInfo().getUniqueId();
		return ft;
	}

	protected void startFileTransfer(IFileTransfer ft, boolean isRestart){
		long scheduledStartTime = getModel().scheduledStartTime;
		if(scheduledStartTime==0 && isRestart){
			try{
				ft.setOverwritePolicy(OverwritePolicy.RESUME_FAILED_TRANSFER);
			}catch(Exception ex){}
		}
		long delay=scheduledStartTime-System.currentTimeMillis();
		if(delay>20000){
			kernel.getContainerProperties().getThreadingServices().
				getScheduledExecutorService().schedule(ft, delay, TimeUnit.MILLISECONDS);
		}
		else{                     
			kernel.getContainerProperties().getThreadingServices().getExecutorService().execute(ft);
		}
	}

	/**
	 * parses ISO8601 date/time format and returns the corresponding time in millis
	 */
	private static DateFormat format=null;
	private static synchronized long parseDate(String s)throws ParseException{
		if(format==null){
			format=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		}
		return format.parse(s).getTime();
	}
}
