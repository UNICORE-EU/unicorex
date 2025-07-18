package eu.unicore.uas.fts;

import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import eu.unicore.services.InitParameters;
import eu.unicore.services.messaging.ResourceDeletedMessage;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.uas.json.JSONUtil;
import eu.unicore.uas.util.LogUtil;
import eu.unicore.uas.xnjs.UFileTransferCreator;
import eu.unicore.util.Pair;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.ActionResult;
import eu.unicore.xnjs.ems.ActionStatus;
import eu.unicore.xnjs.fts.FTSInfo;
import eu.unicore.xnjs.fts.FTSTransferInfo;
import eu.unicore.xnjs.io.IFileTransferEngine;
import eu.unicore.xnjs.io.TransferInfo.Status;


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
		super.initialise(map);
		ServerToServerTransferModel m = getModel();
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

	/**
	 * returns <code>true</code> if the file transfer is not yet finished
	 */
	protected boolean isFinished() throws Exception {
		Status s = getStatus();
		return s==Status.DONE || s==Status.FAILED || s==Status.ABORTED;
	}
	
	@Override
	public void destroy() {
		ServerToServerTransferModel model = getModel();
		try{
			if(!isFinished()){
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
			getKernel().getMessaging().getChannel(model.serviceSpec).publish(m);
		}
		catch(Exception ex){}
		super.destroy();
	}

	public Status getStatus() throws Exception {
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

	public String getFiletransferStatusMessage() throws Exception {
		ActionResult result = getXNJSAction().getResult();
		if(result!=null)
			return result.getStatusString()
			+(result.getErrorMessage()!=null ? " "+result.getErrorMessage():"");
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

	public synchronized Action getXNJSAction() throws Exception {
		if(xnjsAction == null){
			xnjsAction = getXNJSFacade().getAction(getUniqueID());
		}
		return xnjsAction;
	}

	/**
	 * create, but not yet start file transfer action
	 */
	protected Action createTransfer()throws Exception{
		ServerToServerTransferModel model = getModel();
		String protocol = "BFT";
		String remote = null;
		JSONObject j = new JSONObject();
		j.put("workdir", model.workdir);
		j.put("extraParameters", JSONUtil.asJSON(model.getExtraParameters()));
		if(model.getIsExport()){
			j.put("file", model.source);
			j.put("target", model.target);
			remote = model.target;
		}
		else{		
			j.put("file", model.target);
			j.put("source", model.source);
			remote = model.source;
		}
		if(UFileTransferCreator.isREST(remote)) {
			try {
				Pair<String,String>urlInfo = UFileTransferCreator.extractUrlInfo(remote);
				protocol = urlInfo.getM1();
			}catch(Exception e) {}
		}
		else {
			// not a UNICORE storage URL - use the plain protocol
			protocol = new URI(remote).getScheme();
		}
		logger.debug("FTS action = {}", ()->j.toString(2));
		Action action = getXNJSFacade().getXNJS().makeAction(j, "FTS", model.getUniqueID());
		if(model.scheduledStartTime>0){
			action.setNotBefore(model.scheduledStartTime);
		}
		//set the actual protocol
		model.protocol = protocol;
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
