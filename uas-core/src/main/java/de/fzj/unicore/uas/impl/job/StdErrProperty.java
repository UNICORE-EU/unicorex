package de.fzj.unicore.uas.impl.job;

import org.unigrids.x2006.x04.services.jms.StdErrDocument;

import de.fzj.unicore.uas.impl.UASWSResourceImpl;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.ValueRenderer;

public class StdErrProperty extends ValueRenderer {

	public StdErrProperty(UASWSResourceImpl parent){
		super(parent, StdErrDocument.type.getDocumentElementName());
	}
	
	@Override
	protected StdErrDocument getValue() {
		StdErrDocument s=StdErrDocument.Factory.newInstance();
		s.setStdErr(((XnjsActionBacked)parent).getXNJSAction().getExecutionContext().getStderr());
		return s;
	}

}
