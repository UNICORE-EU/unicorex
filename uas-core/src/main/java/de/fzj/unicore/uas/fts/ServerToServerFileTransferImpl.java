package de.fzj.unicore.uas.fts;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.unigrids.services.atomic.types.ProtocolType;

import de.fzj.unicore.uas.json.JSONUtil;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ActionResult;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.fts.FTSInfo;
import de.fzj.unicore.xnjs.fts.FTSTransferInfo;
import de.fzj.unicore.xnjs.io.IFileTransfer.OverwritePolicy;
import de.fzj.unicore.xnjs.io.IFileTransferEngine;
import de.fzj.unicore.xnjs.io.TransferInfo.Status;
import eu.unicore.services.InitParameters;
import eu.unicore.services.messaging.ResourceDeletedMessage;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.services.ws.utils.WSServerUtilities;


/**
 * WS-Resource for initiating and monitoring
 * a server-to-server file transfer
 * 
 * the source parameter is a UNICORE URI
 * the target is the local file (relative to storage root)
 * 
 * @author schuller
 */
public class ServerToServerFileTransferImpl extends FileTransferImpl {

	private static final Logger logger = LogUtil.getLogger(LogUtil.DATA,ServerToServerFileTransferImpl.class);

	public static final String PARAM_SCHEDULED_START="scheduledStartTime";

	public ServerToServerFileTransferImpl(){
		super();
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
		Action action = createTransfer();
		getXNJSFacade().getManager().add(action, m.client);
		logger.info("Submitted server-to-server transfer with id {} for client {}", action.getUUID(), m.client);
	}

	@Override
	public void destroy() {
		ServerToServerTransferModel model = getModel();
		try{
			if(!model.isFinished()){
				logger.info("Aborting filetransfer {} for client {}", getUniqueID(), getClient().getDistinguishedName());
				String id = model.getFileTransferUID();
				getXNJSFacade().getManager().abort(id, getClient());
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
		catch(Exception ex){}
		super.destroy();
	}

	public Status getStatus(){
		int status = getXNJSAction().getStatus();
		Status ftStatus = Status.CREATED;
		switch (status) {
		case ActionStatus.CREATED:
			ftStatus = Status.CREATED;
			break;
		case ActionStatus.DONE:
			ActionResult result = getXNJSAction().getResult();
			if(result.getStatusCode()==ActionResult.USER_ABORTED) {
				ftStatus = Status.ABORTED;
			}
			else if(result.isSuccessful()) {
				ftStatus = Status.DONE;
			} else {
				ftStatus = Status.FAILED;
			}
			break;
		default:
			ftStatus = Status.RUNNING;
		}
		return ftStatus;
	}

	public String getFiletransferStatusMessage(){
		ActionResult result = getXNJSAction().getResult();
		if(result!=null)
			return result.getStatusString()+" "
			+(result.getErrorMessage()!=null? result.getErrorMessage():"");
		return "OK";
	}

	FTSInfo ftsInfo;
	long transferred = 0l;
	long total = 0l;
	
	public FTSInfo getFTSInfo() throws Exception {
		if(ftsInfo==null) {
			try{
				ftsInfo = getXNJSFacade().getXNJS().get(IFileTransferEngine.class).getFTSStorage().read(model.getUniqueID());
			}catch(Exception e) {}
		}
		transferred = 0;
		total = 0;
		if(ftsInfo!=null) {
			for(FTSTransferInfo i : ftsInfo.getTransfers()) {
				if(Status.DONE==i.getStatus()) {
					transferred += i.getSource().getSize();
				}
				total += i.getSource().getSize();
			}
		}
		return ftsInfo;
	}
	
	@Override
	public Long getTransferredBytes() {
		try{
			getFTSInfo();
		}catch(Exception e) {}
		return transferred;
	}
	
	@Override
	public long getDataSize() {
		try{
			getFTSInfo();
		}catch(Exception e) {}
		return total;
	}

	private Action xnjsAction;

	public synchronized Action getXNJSAction(){
		if(xnjsAction == null){
			xnjsAction = getXNJSFacade().getAction(getUniqueID());
		}
		return xnjsAction;
	}

	/**
	 * create, but not yet start file transfer
	 * @param policy - {@link OverwritePolicy}
	 * @throws Exception
	 */
	protected Action createTransfer()throws Exception{
		ServerToServerTransferModel model = getModel();
		JSONObject j = new JSONObject();
		j.put("workdir", model.workdir);
		j.put("extraParameters", JSONUtil.asJSON(model.getExtraParameters()));
		if(model.getIsExport()){
			j.put("file", model.source);
			j.put("target", model.target);
		}
		else{		
			j.put("file", model.target);
			j.put("source", model.source);
		}
		logger.info("FTS action = {}", j.toString(2));
		Action action = getXNJSFacade().getXNJS().makeAction(j, "FTS", model.getUniqueID());
		if(model.scheduledStartTime>0){
			action.setNotBefore(model.scheduledStartTime);
		}
		//set the actual protocol
		model.protocol = ProtocolType.BFT;
		model.fileTransferUID = action.getUUID();
		return action;
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
