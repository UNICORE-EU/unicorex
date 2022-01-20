package de.fzj.unicore.uas.fts;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.xnjs.io.TransferInfo;
import de.fzj.unicore.xnjs.io.TransferInfo.Status;
import de.fzj.unicore.xnjs.util.Observer;
import eu.unicore.services.Home;
import eu.unicore.services.Resource;
import eu.unicore.util.Log;

/**
 * track status changes of server-server file transfers
 *  
 * @author schuller
 */
public class StatusTracker implements Observer<TransferInfo>{
	
	private static final Logger logger = Log.getLogger(Log.SERVICES, StatusTracker.class);
	
	private final Home home;
	
	private final String resourceID;
	
	public StatusTracker(Home home, String resourceID){
		this.home = home;
		this.resourceID = resourceID;
	}
	
	public void update(TransferInfo info){
		Resource r = null;
		try{
			r = home.getForUpdate(resourceID);
			ServerToServerTransferModel model = (ServerToServerTransferModel)r.getModel();
			Status s = info.getStatus();
			int newStatus = FileTransferModel.STATUS_RUNNING;
			String msg = "OK.";
			if(s.equals(Status.FAILED)){
				newStatus = FileTransferModel.STATUS_FAILED;
				msg = info.getStatusMessage();
			}
			else if(s.equals(Status.DONE)){
				newStatus = FileTransferModel.STATUS_DONE;
				msg = "File transfer done.";
			}
			model.status = newStatus;
			model.description = msg;
			model.transferredBytes = info.getTransferredBytes();
			model.setNumberOfBytes(info.getDataSize());
		}catch(Exception ex){
			Log.logException("Cannot update file transfer info", ex, logger);
		}
		finally{
			if(r!=null)try{
				home.persist(r);
			}catch(Exception ex){}
		}
	}

}
