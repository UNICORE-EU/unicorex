package de.fzj.unicore.uas.impl.job.ws;

import org.unigrids.x2006.x04.services.jms.StdOutDocument;

import de.fzj.unicore.uas.impl.BaseResourceImpl;
import de.fzj.unicore.uas.impl.job.JobManagementImpl;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.ValueRenderer;

public class StdOutProperty extends ValueRenderer {

	public StdOutProperty(BaseResourceImpl parent){
		super(parent, StdOutDocument.type.getDocumentElementName());
	}
	
	@Override
	protected StdOutDocument getValue() {
		StdOutDocument s=StdOutDocument.Factory.newInstance();
		s.setStdOut(((JobManagementImpl)parent).getXNJSAction().getExecutionContext().getStdout());
		return s;
	}

}
