package eu.unicore.xnjs.ems;

import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.processors.DefaultProcessor;

/**
 * a dummy processor for testing
 * goes from CREATED to RUNNING to DONE state...

 * @author schuller
 */
public class DummyProcessor extends DefaultProcessor {
	
	public DummyProcessor(XNJS xnjs){
		super(xnjs);
	}

	protected void handleCreated() throws ExecutionException {
		logger.info("Dummy processor, changing status to RUNNING.");
		action.addLogTrace("Changing status to RUNNING.");
		action.setStatus(ActionStatus.RUNNING);
	}

	protected void handleRunning() throws ExecutionException {
		action.addLogTrace("Changing status to DONE.");
		action.setStatus(ActionStatus.DONE);
	}


}
	