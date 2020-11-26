package de.fzj.unicore.xnjs.jsdl;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.DataStagingType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.w3c.dom.Node;

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
import de.fzj.unicore.xnjs.util.LogUtil;
import edu.virginia.vcgr.jsdl.sweep.SweepException;
import edu.virginia.vcgr.jsdl.sweep.SweepListener;
import edu.virginia.vcgr.jsdl.sweep.SweepUtility;

/**
 * JSDL Parameter Sweep processor. <br/>
 * 
 * After processing the stage-ins, the processor will generate 
 * jobs from the initial job template and submit them. 
 *  
 * These are then processed using the normal {@link JSDLProcessor}.
 * 
 * @author schuller
 */
public class SweepProcessor extends JSDLBaseProcessor{

	private static final Logger logger=LogUtil.getLogger(LogUtil.JOBS,SweepProcessor.class);

	public static final String sweepActionType="jsdl_parameter_sweep";
	
	public static final String sweepInstanceType="JSDL-sweep-instance";
	
	public static final String sweepLinkMarker="JSDL-sweep-linked-stage-in";
	
	static final String JOBLIST_KEY=SweepProcessor.class.getName()+"_JobList";
	
	static final String INITIAL_JOBLIST_KEY=SweepProcessor.class.getName()+"_InitialJobList";
	
	public SweepProcessor(XNJS xnjs){
		super(xnjs);
	}

	@Override
	protected void extractFromJobDescription()throws ExecutionException{
		//NOP, because this will be done in the instances 
	}

	/**
	 * stage-in handling: perform non-sweeped imports in the parent, sweeped 
	 * ones in the child
	 */
	@Override
	protected List<DataStageInInfo> extractStageInInfo()throws Exception {
		if(action.getStageIns()==null){
			JobDefinitionDocument jd=(JobDefinitionDocument)action.getAjd();
			DataStagingType[] stage = jd.getJobDefinition().getJobDescription().getDataStagingArray();
			List<DataStagingType>doneByParent=new ArrayList<DataStagingType>();
			List<DataStagingType>doneByChild=new ArrayList<DataStagingType>();
			for(DataStagingType d: stage){
				boolean parent = true;
				if(d.isSetSource()){
					String name =  d.getSource().getURI();
					if(new URI(name).getScheme()==null){
						// this is sweeped, so we leave it in
						parent = false;
					}
					else{
						// not sweeped, so the parent must stage it in, but the
						// child must link to it! So we'll add it to the doneByChild
						// list, but with a special marker!
						DataStagingType childDS = DataStagingType.Factory.parse(d.toString());
						childDS.getSource().setURI(sweepLinkMarker);
						doneByChild.add(childDS);
					}
				}
				
				if(parent){
					doneByParent.add(d);
				}
				else{
					doneByChild.add(d);
				}
			}
			
			// create stage-ins for the parent now
			JobDefinitionDocument jdd=JobDefinitionDocument.Factory.newInstance();
			jdd.addNewJobDefinition().addNewJobDescription().setDataStagingArray(doneByParent.toArray(new DataStagingType[doneByParent.size()]));
			action.setStageIns(jsdlParser.parseImports(jdd));

			// and change the job description for the child to be done later
			jd.getJobDefinition().getJobDescription().setDataStagingArray(doneByChild.toArray(new DataStagingType[0]));
			action.setDirty();
		}
		return action.getStageIns();
	}

	@Override
	protected void handleReady(){
		if(logger.isTraceEnabled())logger.trace("Handling READY state for Action "
				+action.getUUID());

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
		JobDefinitionDocument job=(JobDefinitionDocument)action.getAjd();
		List<String>jobs=getOrCreateJobList();
		try{
			int limit = xnjs.getXNJSProperties().getIntValue(XNJSProperties.SWEEP_LIMIT);
			SweepListener sl=new MySweepListener(jobs, limit);
			SweepUtility.performSweep(job.getDomNode(), sl);
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
		if(logger.isDebugEnabled()){
			logger.debug("Checking status for "+jobs.size()+" sweep instances.");
		}
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
			logger.info("Action "+action.getUUID()+ " SUCCESSFUL.");
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
	
	
	
	class MySweepListener implements SweepListener{
		
		final int limit;
		
		final List<String>jobIDs;
		
		/**
		 * @param jobIDs - for storing the job IDs
		 * @param limit - limit on the number of jobs
		 */
		public MySweepListener(List<String>jobIDs, int limit){
			this.limit=limit;
			this.jobIDs=jobIDs;
		}
		
		/**
		 * TODO params contain file sweep info
		 */
		public void emitSweepInstance(Node jobNode, Map<String, Object> params)
				throws SweepException {
			try{
				if(jobIDs.size()>=limit)throw new SweepException("Sweep limit of <"+limit+"> was exceeded!");
				JobDefinitionDocument job=JobDefinitionDocument.Factory.parse(jobNode);
				adaptJobDescription(job, params);
				String id=submitChild(job, params);
				jobIDs.add(id);
			}catch(Exception ex){
				throw new SweepException("Can't submit sub job", ex);
			}
		}
	}

	/**
	 * Modifies the sweep instance by fixing stage-ins. Shared inputs can be linked. 
	 * 
	 * @param job - the emitted sweep instance which will be modified
	 * @param params
	 */
	protected void adaptJobDescription(JobDefinitionDocument job, Map<String, Object> params){
		DataStagingType[] staging=job.getJobDefinition().getJobDescription().getDataStagingArray();
		if(staging!=null){		
			for(DataStagingType dst: staging){
				if(dst.getSource()!=null){
					if(sweepLinkMarker.equals(dst.getSource().getURI())){
						String target=dst.getFileName();
						dst.getSource().setURI("link:../"+target);
					}
				}
			}
		}
	}

	protected String submitChild(JobDefinitionDocument job, Map<String, Object> params)throws Exception{
		Action subAction=new Action();
		subAction.setInternal(true);
		subAction.setType(sweepInstanceType);
		subAction.setAjd((Serializable)job);
		subAction.setClient(action.getClient());
		ProcessingContext pc=subAction.getProcessingContext();
		// there are no more client stage-ins, so we can autostart
		pc.put(Action.AUTO_SUBMIT, Boolean.TRUE); 
		if(params!=null){
			pc.put(SweepInstanceProcessor.SWEEP_PARAMS_KEY,params);
		}
		pc.put(SweepInstanceProcessor.SWEEP_PARENT_JOB_ID_KEY,action.getUUID());
		pc.put(SweepInstanceProcessor.SWEEP_PARENT_JOB_USPACE_KEY,
			   action.getExecutionContext().getWorkingDirectory());
		
		ExecutionContext ec=new ExecutionContext(subAction.getUUID());
		subAction.setExecutionContext(ec);
		
		String base=action.getExecutionContext().getWorkingDirectory();
		String uspace=ecm.createUSpace(subAction, base);
		
		ec.setWorkingDirectory(uspace);
		
		String id=(String)manager.addInternalAction(subAction);
		return id;
	}

}
