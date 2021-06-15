package de.fzj.unicore.uas.fts.ws;

import org.unigrids.x2006.x04.services.fts.StatusDocument;
import org.unigrids.x2006.x04.services.fts.StatusType;
import org.unigrids.x2006.x04.services.fts.SummaryType;

import de.fzj.unicore.uas.fts.FileTransferModel;
import eu.unicore.services.Resource;
import eu.unicore.services.ws.renderers.ValueRenderer;

public class StatusResourceProperty extends ValueRenderer {
	
	public StatusResourceProperty(Resource inst){
		super(inst, StatusDocument.type.getDocumentElementName());
	}

	@Override
	protected StatusDocument getValue(){
		StatusDocument res=StatusDocument.Factory.newInstance();
		StatusType ret=res.addNewStatus();
		FileTransferModel m = (FileTransferModel)parent.getModel();
		ret.setSummary(SummaryType.Enum.forInt(m.getStatus()));
		ret.setDescription(m.getDescription());
		return res;
	}

}
