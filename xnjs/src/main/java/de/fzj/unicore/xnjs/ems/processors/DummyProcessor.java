package de.fzj.unicore.xnjs.ems.processors;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.ems.ProcessingException;

/**
 * a dummy processor for testing
 * goes from CREATED to RUNNING to DONE state...

 * @author schuller
 */
public class DummyProcessor extends DefaultProcessor {
	
	public DummyProcessor(XNJS xnjs){
		super(xnjs);
	}

	protected void handleCreated() throws ProcessingException {
		logger.info("Dummy processor, changing status to RUNNING.");
		action.addLogTrace("Changing status to RUNNING.");
		action.setStatus(ActionStatus.RUNNING);
	}

	protected void handleRunning() throws ProcessingException {
		action.addLogTrace("Changing status to DONE.");
		action.setStatus(ActionStatus.DONE);
	}


}
	