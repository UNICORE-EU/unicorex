package de.fzj.unicore.uas.fts.ws;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.SourceDocument;

import de.fzj.unicore.uas.fts.FileTransferImpl;
import de.fzj.unicore.wsrflite.Resource;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.ValueRenderer;

public class SourceResourceProperty extends ValueRenderer {
	
	public SourceResourceProperty(Resource parent){
		super(parent, SourceDocument.type.getDocumentElementName());
	}
	
	@Override
	protected SourceDocument getValue() {
		String source=((FileTransferImpl)parent).getModel().getSource();
		if(source==null)return null;
		
		SourceDocument res=SourceDocument.Factory.newInstance();
		res.addNewSource().setURI(source);
		return res;
	}
	
}