package de.fzj.unicore.uas.fts;

import org.unigrids.x2006.x04.services.fts.TransferRateDocument;

import de.fzj.unicore.wsrflite.impl.ResourceImpl;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.ValueRenderer;

/**
 * publishes the estimated transfer rate of a server-server transfer
 * in bytes per second (this is not the LIVE rate, but the "aggregate" rate,
 * i.e. it will be constant after the transfer has finished)
 * 
 * @author schuller
 */
public class TransferRateResourceProperty extends ValueRenderer {
	
	public TransferRateResourceProperty(ResourceImpl inst){
		super(inst, TransferRateDocument.type.getDocumentElementName());
	}
	
	@Override
	public TransferRateDocument getValue() throws Exception {
		long rate=(((ServerToServerFileTransferImpl)parent).getTransferRate());
		TransferRateDocument res=TransferRateDocument.Factory.newInstance();
		res.setTransferRate(rate);
		return res;
	}

}
	