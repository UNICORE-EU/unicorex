package de.fzj.unicore.xnjs.json.sweep;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.XNJSProperties;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ActionResult;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.ems.ExecutionContext;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.ems.ProcessingContext;
import de.fzj.unicore.xnjs.ems.ProcessingException;
import de.fzj.unicore.xnjs.io.DataStageInInfo;
import de.fzj.unicore.xnjs.json.JSONJobProcessor;
import de.fzj.unicore.xnjs.util.LogUtil;

/**
 * JSON Parameter Sweep processor. <br/>
 * 
 * After processing the stage-ins, the processor will generate 
 * jobs from the initial job template and submit them. 
 *  
 * These are then processed using the JSONSweepInstanceProcessor
 * 
 * @author schuller
 */
public class JSONSweepProcessor extends JSONJobProcessor {

	private static final Logger logger = LogUtil.getLogger(LogUtil.JOBS,JSONSweepProcessor.class);

	public static final String sweepActionType = "json_parameter_sweep";

	public static final String sweepInstanceType = "json-sweep-instance";

	public static final String sweepLinkMarker="__json-sweep-linked-stage-in";
	
	public static final String sweepFileMarker="__json-sweep-stage-in";

	public static final String sweepDescription="__json-sweep-description";

	static final String JOBLIST_KEY = JSONSweepProcessor.class.getName()+"_JobList";

	static final String INITIAL_JOBLIST_KEY = JSONSweepProcessor.class.getName()+"_InitialJobList";

	public JSONSweepProcessor(XNJS xnjs){
		super(xnjs);
	}

	@Override
	protected void extractFromJobDescription()throws ExecutionException{
		try {
			action.setStageIns(extractStageInInfo());
			action.setStageOuts(extractStageOutInfo());
			action.setDirty();
		} catch (Exception e) {
			if(e instanceof ExecutionException){
				throw (ExecutionException)e;
			}
			else{
				throw new ExecutionException(e);
			}
		}
	}

	/**
	 * stage-in handling: perform non-sweeped imports in the parent, sweeped 
	 * ones in the child
	 */
	@Override
	protected List<DataStageInInfo> extractStageInInfo()throws Exception {
		if(action.getStageIns()==null){
			JSONObject jd = getJobDescriptionDocument();
			JSONArray stage = jd.optJSONArray("Imports");
			if(stage==null)stage = new JSONArray();

			JSONArray doneByParent = new JSONArray();
			List<JSONObject>doneByChild = new ArrayList<>();
			for(int i=0; i<stage.length(); i++){
				JSONObject d = stage.getJSONObject(i);
				if(d.get("From")instanceof JSONArray){
					JSONArray inputs = d.getJSONArray("From");
					// will be sweeped
					d.put("From", sweepFileMarker);
					jd.put(sweepFileMarker, inputs);
					doneByChild.add(d);
				}
				else{
					// not sweeped, so the parent must stage it in, but the
					// child must link to it! So we'll add it to the doneByChild
					// list, but with a special marker!
					doneByParent.put(d);
					JSONObject childDS = new JSONObject(d.toString());
					childDS.put("From",sweepLinkMarker);
					doneByChild.add(childDS);
				}
			}

			// create stage-ins for the parent now
			action.setStageIns(doExtractStageIn(doneByParent));

			// and change the job description for the child to be done later
			jd.put("Imports", doneByChild.toArray(new JSONObject[0]));
			action.setAjd(jd.toString());
		}
		return action.getStageIns();
	}

	@Override
	protected void handleCreated()throws ProcessingException{
		super.handleCreated();
	}

	@Override
	protected void handleReady(){
		if(xnjs.getXNJSProperties().isAutoSubmitWhenReady()){
			action.setStatus(ActionStatus.PENDING);
		}
		else{
			if(action.getProcessingContext().get(Action.AUTO_SUBMIT)==null){
				//action will sleep until client starts it
				action.setWaiting(true);
				return;
			}
			else {
				action.setStatus(ActionStatus.PENDING);
			}
		}
	}

	/**
	 * generates and submits sweep jobs and stores their ID in the master job's processing
	 * context
	 */
	@Override
	protected void handlePending() throws ProcessingException{
		forkChildJobs();
		action.setStatus(ActionStatus.RUNNING);
	}

	protected void forkChildJobs() throws ProcessingException{
		JSONObject job = getJobDescriptionDocument();
		List<String>jobs=getOrCreateJobList();
		try{
			int limit = xnjs.getXNJSProperties().getIntValue(XNJSProperties.SWEEP_LIMIT);
			DocumentSweep ds = createSweep();
			ds.setParent(job);
			for(JSONObject child: ds) {
				if(limit>0 && jobs.size()==limit) {
					action.addLogTrace("Reached limit <"+limit+"> for number of sweep jobs!");
					break;
				}
				adaptJobDescription(child, null);
				String id = submitChild(child, null);
				action.addLogTrace(id+" : "+child.optString(sweepDescription));
				jobs.add(id);
			}
			action.addLogTrace("Added "+jobs.size()+" sweep jobs.");
			//store inital job ID list for later cleanup/status checking
			List<String>initialList=new ArrayList<String>();
			initialList.addAll(jobs);
			action.getProcessingContext().put(INITIAL_JOBLIST_KEY, initialList);
			action.setStatus(ActionStatus.READY);
		}catch(Exception e){
			throw new ProcessingException("Error performing sweep",e);
		}
	}

	/**
	 * check if there are any remaining jobs that are still running
	 */
	@Override
	protected void handleRunning()throws ProcessingException{
		List<String>jobs=getOrCreateJobList();
		logger.debug("Checking status for {} sweep instances.", jobs.size());
		for(Iterator<String> it=jobs.iterator();it.hasNext();){
			try{
				String id=it.next();
				Action a=manager.getAction(id);
				if(a==null || ActionStatus.DONE==a.getStatus()){
					if(a!=null)action.addLogTrace("Sweep instance "+a.getUUID()+" is DONE.");
					it.remove();
				}
			}
			catch(Exception e){
				throw new ProcessingException(e);
			}
		}
		if(jobs.size()==0){
			action.addLogTrace("All sweep jobs done");
			action.setStatus(ActionStatus.DONE);
			action.setResult(new ActionResult(ActionResult.SUCCESSFUL,"Success.", 0));
			logger.info("Action {} SUCCESSFUL.", action.getUUID());
		}
		else{
			//if we still have some running sweep instances, reduce CPU load
			sleep(3000);
		}
	}

	private List<String>getOrCreateJobList(){
		@SuppressWarnings("unchecked")
		List<String>subJobs=(List<String>)action.getProcessingContext().get(JOBLIST_KEY);
		if(subJobs==null){
			subJobs=new ArrayList<String>();
			action.getProcessingContext().put(JOBLIST_KEY,subJobs);
		}
		return subJobs;
	}

	/**
	 * Modifies the sweep instance by fixing stage-ins. Shared inputs can be linked. 
	 * 
	 * @param job - the emitted sweep instance which will be modified
	 * @param params
	 */
	protected void adaptJobDescription(JSONObject job, Map<String, Object> params) throws JSONException {
		JSONArray staging = job.optJSONArray("Imports");
		if(staging!=null){		
			for(int i=0;i<staging.length();i++){
				JSONObject dst = staging.getJSONObject(i);
				if(sweepLinkMarker.equals(dst.getString("From"))){
					String target=dst.getString("To");
					dst.put("From", "link:"+action.getExecutionContext().getWorkingDirectory()+"/"+target);
				}
			}
		}
	}

	protected String submitChild(JSONObject job, Map<String, Object> params)throws Exception{
		Action subAction=new Action();
		subAction.setInternal(true);
		subAction.setType(sweepInstanceType);
		subAction.setAjd(job.toString());
		subAction.setClient(action.getClient());
		ProcessingContext pc=subAction.getProcessingContext();
		// there are no more client stage-ins, so we can autostart
		pc.put(Action.AUTO_SUBMIT, Boolean.TRUE); 
		if(params!=null){
			pc.put(JSONSweepInstanceProcessor.SWEEP_PARAMS_KEY,params);
		}
		pc.put(JSONSweepInstanceProcessor.SWEEP_PARENT_JOB_ID_KEY,action.getUUID());
		pc.put(JSONSweepInstanceProcessor.SWEEP_PARENT_JOB_USPACE_KEY,
				action.getExecutionContext().getWorkingDirectory());

		ExecutionContext ec=new ExecutionContext();
		subAction.setExecutionContext(ec);

		String base=action.getExecutionContext().getWorkingDirectory();
		String uspace=ecm.createUSpace(subAction, base);

		ec.setWorkingDirectory(uspace);

		String id=(String)manager.addInternalAction(subAction);
		return id;
	}

}
