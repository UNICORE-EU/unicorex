package de.fzj.unicore.uas.impl;

import java.security.MessageDigest;

import org.unigrids.services.atomic.types.TagsDocument;

import eu.unicore.services.Resource;
import eu.unicore.services.ws.AbstractXmlRenderer;

/**
 * renders the tags of a resource 
 *
 * @author schuller
 */
public class TagsRenderer extends AbstractXmlRenderer {

	private final Resource parent;
	
	public TagsRenderer(Resource parent) {
		super(TagsDocument.type.getDocumentElementName());
		this.parent=parent;
	}

	@Override
	public TagsDocument[] render() {
		TagsDocument doc=TagsDocument.Factory.newInstance();
		doc.addNewTags();
		for(String t: parent.getModel().getTags()){
			doc.getTags().addTag(t);
		}
		return new TagsDocument[]{doc};
	}
	
	@Override
	public void updateDigest(MessageDigest md) throws Exception {
		md.update(render()[0].toString().getBytes());
	}
}
