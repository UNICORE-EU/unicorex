package de.fzj.unicore.uas.rest;

import java.util.Date;
import java.util.Map;

import javax.ws.rs.Path;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.fts.ServerToServerFileTransferImpl;
import de.fzj.unicore.uas.fts.ServerToServerTransferModel;
import eu.unicore.services.rest.Link;
import eu.unicore.services.rest.RESTUtils;
import eu.unicore.services.rest.USEResource;
import eu.unicore.services.rest.impl.ServicesBase;

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
		ServerToServerTransferModel model = getModel();
		if(model.getScheduledStartTime()>0){
			Date d = new Date(model.getScheduledStartTime());
			props.put("scheduledStartTime", getISODateFormatter().format(d));
		}
		props.put("protocol",model.getProtocol());
		props.put("isExport",model.getIsExport());
		props.put("source",model.getSource());
		props.put("target",model.getTarget());
		renderStatus(props);
		return props;
	}

	protected void renderStatus(Map<String,Object> o) throws Exception{
		ServerToServerFileTransferImpl resource  = getResource();
		o.put("status",resource.getStatus());
		o.put("statusMessage", resource.getFiletransferStatusMessage());
		o.put("transferredBytes",resource.getTransferredBytes());
		o.put("size", resource.getDataSize());
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
