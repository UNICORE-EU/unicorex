package eu.unicore.uas.rest;

import java.util.Date;
import java.util.Map;

import org.json.JSONObject;

import eu.unicore.services.rest.Link;
import eu.unicore.services.rest.RESTUtils;
import eu.unicore.services.rest.USEResource;
import eu.unicore.services.rest.impl.ServicesBase;
import eu.unicore.uas.UAS;
import eu.unicore.uas.fts.ServerToServerFileTransferImpl;
import eu.unicore.uas.fts.ServerToServerTransferModel;
import eu.unicore.xnjs.fts.FTSTransferInfo;
import eu.unicore.xnjs.io.TransferInfo.Status;
import jakarta.ws.rs.Path;

/**
 * REST interface to server-to-server transfers
 *
 * @author schuller
 */
@Path("/transfers")
@USEResource(home=UAS.SERVER_FTS)
public class Transfers extends ServicesBase {

	@Override
	protected Map<String,Object>getProperties() throws Exception {
		Map<String,Object> props = super.getProperties();
		ServerToServerFileTransferImpl resource  = getResource();
		ServerToServerTransferModel model = getModel();
		if(model.getScheduledStartTime()>0){
			Date d = new Date(model.getScheduledStartTime());
			props.put("scheduledStartTime", getISODateFormatter().format(d));
		}
		props.put("protocol",model.getProtocol());
		props.put("isExport",model.getIsExport());
		props.put("source",model.getSource());
		props.put("target",model.getTarget());
		JSONObject fileList = new JSONObject();
		try{ 
			for(FTSTransferInfo i: resource.getFTSInfo().getTransfers()) {
				fileList.put(i.getSource().getPath(), i.getStatus());
			}
		}catch(Exception e) {}
		props.put("files", fileList);
		renderStatus(props);
		return props;
	}

	protected void renderStatus(Map<String,Object> o) throws Exception{
		ServerToServerFileTransferImpl resource  = getResource();
		o.put("status",resource.getStatus());
		o.put("statusMessage", resource.getFiletransferStatusMessage());
		o.put("transferredBytes",resource.getTransferredBytes());
		if(Status.CREATED==resource.getStatus()) {
			o.put("size", -1);
		}
		else {
			o.put("size", resource.getDataSize());
		}
	}
	
	@Override
	public ServerToServerTransferModel getModel(){
		return (ServerToServerTransferModel)model;
	}
	
	@Override
	public ServerToServerFileTransferImpl getResource(){
		return (ServerToServerFileTransferImpl)resource;
	}

	@Override
	protected void updateLinks() {
		super.updateLinks();
		ServerToServerTransferModel model = getModel();
		links.add(new Link("storage",RESTUtils.makeHref(kernel, "core/storages", model.getParentUID()),"Parent Storage"));
	}
}
