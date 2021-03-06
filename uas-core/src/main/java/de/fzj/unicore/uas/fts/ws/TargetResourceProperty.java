package de.fzj.unicore.uas.fts.ws;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.TargetDocument;

import de.fzj.unicore.uas.fts.FileTransferImpl;
import eu.unicore.services.Resource;
import eu.unicore.services.ws.renderers.ValueRenderer;

public class TargetResourceProperty extends ValueRenderer{

	public TargetResourceProperty(Resource parent){
		super(parent, TargetDocument.type.getDocumentElementName());
	}

	@Override
	protected TargetDocument getValue() {
		String target=((FileTransferImpl)parent).getModel().getTarget();
		if(target==null)return null;
		
		TargetDocument res=TargetDocument.Factory.newInstance();
		res.addNewTarget().setURI(target);
		return res;
	}
}