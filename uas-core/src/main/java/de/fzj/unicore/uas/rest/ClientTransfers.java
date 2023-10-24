package de.fzj.unicore.uas.rest;

import java.util.Map;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.fts.FileTransferImpl;
import de.fzj.unicore.uas.fts.FileTransferModel;
import de.fzj.unicore.uas.json.JSONUtil;
import eu.unicore.services.rest.Link;
import eu.unicore.services.rest.RESTUtils;
import eu.unicore.services.rest.USEResource;
import eu.unicore.services.rest.impl.ServicesBase;
import jakarta.ws.rs.Path;

/**
 * REST interface to client-server data transfers
 *
 * @author schuller
 */
@Path("/client-server-transfers")
@USEResource(home=UAS.CLIENT_FTS)
public class ClientTransfers extends ServicesBase {

	@Override
	protected Map<String,Object>getProperties() throws Exception {
		Map<String,Object> props = super.getProperties();
		FileTransferModel model = getModel();
		props.put("protocol",model.getProtocol());
		props.put("isExport",model.getIsExport());
		props.put("source",model.getSource());
		props.put("target",model.getTarget());
		props.put("extraParameters",JSONUtil.asJSON(model.getExtraParameters()));
		props.put("transferredBytes",model.getTransferredBytes());
		return props;
	}
	
	@Override
	public FileTransferModel getModel(){
		return (FileTransferModel)model;
	}
	
	@Override
	public FileTransferImpl getResource(){
		return (FileTransferImpl)resource;
	}
	
	@Override
	protected void updateLinks() {
		super.updateLinks();
		FileTransferModel model = getModel();
		links.add(new Link("storage",RESTUtils.makeHref(kernel, "core/storages", model.getParentUID()),"Parent Storage"));
	}
}
