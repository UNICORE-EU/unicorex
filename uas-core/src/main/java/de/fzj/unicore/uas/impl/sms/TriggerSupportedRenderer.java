package de.fzj.unicore.uas.impl.sms;

import org.unigrids.x2006.x04.services.sms.TriggeringSupportedDocument;

import de.fzj.unicore.wsrflite.impl.ResourceImpl;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.ValueRenderer;

public class TriggerSupportedRenderer extends ValueRenderer{
	
	public TriggerSupportedRenderer(ResourceImpl parent){
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
