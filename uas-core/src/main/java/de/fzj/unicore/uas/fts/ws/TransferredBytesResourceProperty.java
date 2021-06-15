package de.fzj.unicore.uas.fts.ws;

import org.unigrids.x2006.x04.services.fts.TransferredBytesDocument;

import de.fzj.unicore.uas.fts.FileTransferImpl;
import eu.unicore.services.impl.ResourceImpl;
import eu.unicore.services.ws.renderers.ValueRenderer;

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
	