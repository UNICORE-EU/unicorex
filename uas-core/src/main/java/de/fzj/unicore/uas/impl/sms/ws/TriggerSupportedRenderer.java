package de.fzj.unicore.uas.impl.sms.ws;

import org.unigrids.x2006.x04.services.sms.TriggeringSupportedDocument;

import de.fzj.unicore.uas.impl.sms.SMSBaseImpl;
import eu.unicore.services.Resource;
import eu.unicore.services.ws.renderers.ValueRenderer;

public class TriggerSupportedRenderer extends ValueRenderer{
	
	public TriggerSupportedRenderer(Resource parent){
		super(parent, TriggeringSupportedDocument.type.getDocumentElementName());
	}
	
	@Override
	protected TriggeringSupportedDocument getValue() throws Exception {
		TriggeringSupportedDocument res = TriggeringSupportedDocument.Factory.newInstance();
		SMSBaseImpl sms = (SMSBaseImpl)parent;
		boolean supported = sms.isTriggerEnabled();
		res.setTriggeringSupported(supported);
		return res;
	}

}
