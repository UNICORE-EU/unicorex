package de.fzj.unicore.uas.impl.job.ws;

import org.unigrids.x2006.x04.services.jms.StdErrDocument;

import de.fzj.unicore.uas.impl.BaseResourceImpl;
import de.fzj.unicore.uas.impl.job.JobManagementImpl;
import eu.unicore.services.ws.renderers.ValueRenderer;

public class StdErrProperty extends ValueRenderer {

	public StdErrProperty(BaseResourceImpl parent){
		super(parent, StdErrDocument.type.getDocumentElementName());
	}
	
	@Override
	protected StdErrDocument getValue() {
		StdErrDocument s=StdErrDocument.Factory.newInstance();
		s.setStdErr(((JobManagementImpl)parent).getXNJSAction().getExecutionContext().getStderr());
		return s;
	}

}
