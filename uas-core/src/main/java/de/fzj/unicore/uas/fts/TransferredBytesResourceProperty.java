package de.fzj.unicore.uas.fts;

import org.unigrids.x2006.x04.services.fts.TransferredBytesDocument;

import de.fzj.unicore.wsrflite.impl.ResourceImpl;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.ValueRenderer;

public class TransferredBytesResourceProperty extends ValueRenderer {
	
	public TransferredBytesResourceProperty(ResourceImpl inst){
		super(inst, TransferredBytesDocument.type.getDocumentElementName());
	}
	
	@Override
	protected TransferredBytesDocument getValue() throws Exception {
		TransferredBytesDocument res=TransferredBytesDocument.Factory.newInstance();
		res.setTransferredBytes(((FileTransferImpl)parent).getTransferredBytes());
		return res;
	}

}
	