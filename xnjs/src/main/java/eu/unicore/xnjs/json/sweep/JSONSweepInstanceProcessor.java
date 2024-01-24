package eu.unicore.xnjs.json.sweep;

import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.json.JSONJobProcessor;

/**
 * handles sweep instances forked by SweepProcessor
 * 
 * TODO
 *  - perform input file modifications (file sweep)
 * 
 * @author schuller
 */
public class JSONSweepInstanceProcessor extends JSONJobProcessor {

	//for storing the sweep params
	static final String SWEEP_PARAMS_KEY = JSONSweepInstanceProcessor.class.getName()+"_SweepParams";
	
	//for storing the ID of the master job
	static final String SWEEP_PARENT_JOB_ID_KEY = JSONSweepInstanceProcessor.class.getName()+"_ParentJob_ID";
	
	//for storing the Uspace location of the master job
	static final String SWEEP_PARENT_JOB_USPACE_KEY = JSONSweepInstanceProcessor.class.getName()+"_ParentJob_Uspace";
	
	public JSONSweepInstanceProcessor(XNJS xnjs){
		super(xnjs);
	}
	
}
