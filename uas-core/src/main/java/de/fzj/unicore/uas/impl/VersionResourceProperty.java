package de.fzj.unicore.uas.impl;

import java.security.MessageDigest;

import org.unigrids.services.atomic.types.VersionDocument;

import eu.unicore.services.Kernel;
import eu.unicore.services.Resource;
import eu.unicore.services.ws.AbstractXmlRenderer;

/**
 * publishes the version of the parent resource 
 */
public class VersionResourceProperty extends AbstractXmlRenderer{

	private final Resource parent;
	
	public VersionResourceProperty(Resource parent){
		super(VersionDocument.type.getDocumentElementName());
		this.parent=parent;
	}
	
	@Override
	public VersionDocument[] render() {
		VersionDocument ver=VersionDocument.Factory.newInstance();
		ver.setVersion(getVersion());
		return new VersionDocument[]{ver};
	}

	
	@Override
	public void updateDigest(MessageDigest md) throws Exception {
		md.update(getVersion().getBytes());
	}

	private String getVersion(){
		return Kernel.getVersion(parent.getClass());
	}

}
