package de.fzj.unicore.uas.impl.job;

import org.unigrids.x2006.x04.services.jms.StdOutDocument;

import de.fzj.unicore.uas.impl.UASWSResourceImpl;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.ValueRenderer;

public class StdOutProperty extends ValueRenderer {

	public StdOutProperty(UASWSResourceImpl parent){
		super(parent, StdOutDocument.type.getDocumentElementName());
	}
	
	@Override
	protected StdOutDocument getValue() {
		StdOutDocument s=StdOutDocument.Factory.newInstance();
		s.setStdOut(((XnjsActionBacked)parent).getXNJSAction().getExecutionContext().getStdout());
		return s;
	}

}
