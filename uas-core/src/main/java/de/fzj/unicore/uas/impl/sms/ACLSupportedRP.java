package de.fzj.unicore.uas.impl.sms;

import org.unigrids.x2006.x04.services.sms.ACLSupportedDocument;

import de.fzj.unicore.wsrflite.impl.ResourceImpl;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.ValueRenderer;

public class ACLSupportedRP extends ValueRenderer{
	
	public ACLSupportedRP(ResourceImpl parent){
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
