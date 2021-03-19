package de.fzj.unicore.uas.fts.ws;

import org.unigrids.x2006.x04.services.fts.SizeDocument;

import de.fzj.unicore.uas.fts.FileTransferImpl;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.ValueRenderer;

/**
 * size of the file to export
 * @author schuller
 */
public class UASSizeResourceProperty extends ValueRenderer {
	
	public UASSizeResourceProperty(FileTransferImpl parent){
		super(parent, SizeDocument.type.getDocumentElementName());
	}
	
	@Override
	public SizeDocument getValue() {
		SizeDocument sd=SizeDocument.Factory.newInstance();
		sd.setSize(((FileTransferImpl)parent).getDataSize());
		return sd;
	}
}
