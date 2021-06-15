package de.fzj.unicore.uas.impl.sms.ws;

import org.unigrids.x2006.x04.services.sms.ACLSupportedDocument;

import de.fzj.unicore.uas.impl.sms.SMSBaseImpl;
import eu.unicore.services.Resource;
import eu.unicore.services.ws.renderers.ValueRenderer;

public class ACLSupportedRP extends ValueRenderer{
	
	public ACLSupportedRP(Resource parent){
		super(parent, ACLSupportedDocument.type.getDocumentElementName());
	}
	
	@Override
	protected ACLSupportedDocument getValue() throws Exception {
		ACLSupportedDocument acl = ACLSupportedDocument.Factory.newInstance();
		SMSBaseImpl sms = (SMSBaseImpl)parent;
		boolean supported = sms.getStorageAdapter().isACLSupported("/");
		acl.setACLSupported(supported);
		return acl;
	}

}
