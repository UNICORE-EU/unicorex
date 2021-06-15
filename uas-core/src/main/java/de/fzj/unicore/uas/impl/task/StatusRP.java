package de.fzj.unicore.uas.impl.task;

import java.math.BigInteger;

import org.unigrids.services.atomic.types.StatusInfoDocument;
import org.unigrids.services.atomic.types.StatusInfoType;

import de.fzj.unicore.uas.impl.task.TaskImpl.TaskStatus;
import eu.unicore.services.ws.renderers.ValueRenderer;

public class StatusRP extends ValueRenderer {
	
	public StatusRP(TaskImpl parent){
		super(parent,StatusInfoDocument.type.getDocumentElementName());
	}
	
	@Override
	protected StatusInfoDocument getValue() throws Exception{
		StatusInfoDocument d=StatusInfoDocument.Factory.newInstance();
		TaskStatus state=((TaskImpl)parent).getStatus();
		StatusInfoType statusInfo=d.addNewStatusInfo();
		statusInfo.setStatus(state.status);
		if(state.message!=null)statusInfo.setDescription(state.message);
		if(state.progress!=null)statusInfo.setProgress(state.progress);
		if(state.exitCode!=null)statusInfo.setExitCode(BigInteger.valueOf(state.exitCode));
		return d;
	}
}

