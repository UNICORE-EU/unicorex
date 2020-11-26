package de.fzj.unicore.uas.impl.tss.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;

import de.fzj.unicore.persist.PersistenceException;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.impl.job.JobManagementImpl;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.uas.xnjs.XNJSFacade;
import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ActionResult;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.ems.IExecutionContextManager;
import de.fzj.unicore.xnjs.ems.Manager;
import de.fzj.unicore.xnjs.ems.ProcessingException;
import de.fzj.unicore.xnjs.ems.processors.DefaultProcessor;
import de.fzj.unicore.xnjs.io.XnjsFileWithACL;
import de.fzj.unicore.xnjs.tsi.remote.TSIUtils;
import de.fzj.unicore.xnjs.tsi.TSI;
import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.security.Client;

/**
 * re-create "lost" XNJS action instances from uspace information.
 * 
 * @author schuller
 */
public class RecreateXNJSJobs implements Runnable{

	private final Logger logger = LogUtil.getLogger(LogUtil.JOBS, RecreateXNJSJobs.class);

	private final Client client;

	private final String xnjsReference;

	private final Home jmsHome;

	// all JMS instance IDs
	private final Collection<String>existingJobs;

	private final Kernel kernel;
	
	public RecreateXNJSJobs(Kernel kernel, Client client, String xnjsReference)throws Exception{
		this.kernel=kernel;
		this.client=client;
		this.xnjsReference=xnjsReference;
		this.jmsHome=kernel.getHome(UAS.JMS);
		this.existingJobs=getExistingJobs();
	}

	public void run(){
		try{
			logger.info("Regenerating UNICORE XNJS Jobs for "+client.getDistinguishedName());
			ensureProcessing();
			XNJSFacade xnjs=XNJSFacade.get(xnjsReference, kernel);
			for(String jobID: existingJobs){
				// check if job belongs to us
				try{
					JobManagementImpl jms=(JobManagementImpl)jmsHome.get(jobID);
					String owner=jms.getOwner();
					if(!X500NameUtils.equal(client.getDistinguishedName(), owner))
						continue;

					Action action=xnjs.getAction(jobID);
					if(action==null){
						restore(jms);
					}
				}
				catch(Exception ex){
					logger.error("Could not restore XNJS action for : "+jobID,ex);
				}
				
			}
		}catch(Exception ex){
			logger.error("Could not restore jobs for "+client,ex);
		}
	}

	// fix processing chain to skip all processing for re-created jobs
	protected void ensureProcessing(){
		XNJS config=XNJSFacade.get(xnjsReference, kernel).getXNJS();
		synchronized(config){
			List<String> chain=config.getProcessorChain("JSDL");
			if(!ReCreateProcessor.class.getName().equals(chain.get(0))){
				chain.add(0,ReCreateProcessor.class.getName());
			}
		}
	}

	/**
	 * TODO load persisted information from uspace if available
	 * @param jms
	 * @throws Exception
	 */
	protected void restore(JobManagementImpl jms)throws Exception{
		Action a=new Action();
		a.setUUID(jms.getUniqueID());
		a.setClient(client);
		a.setType("JSON");
		a.setStatus(ActionStatus.DONE);
		a.setResult(new ActionResult(ActionResult.UNKNOWN, 
				"Job was lost and restored during server restart."));
		a.setAjd((Serializable)"{}");
		XNJS xnjs=XNJSFacade.get(xnjsReference, kernel).getXNJS();
		xnjs.get(IExecutionContextManager.class).getContext(a);
		xnjs.get(Manager.class).add(a, client);
	}

	private Collection<String>getExistingJobs()throws PersistenceException{
		return jmsHome.getStore().getUniqueIDs();
	}
	
	public static class ReCreateProcessor extends DefaultProcessor{

		public ReCreateProcessor(XNJS configuration) {
			super(configuration);
		}
		
		@Override
		protected void begin(){
			if(isRecreated()){
				// no further processing after this processor
				next=null;
			}
		}
		
		@Override
		protected void done() throws ProcessingException {
			if(!isRecreated())return;
			
			// at least check if exit code exists -> successful
			TSI tsi = xnjs.getTargetSystemInterface(action.getClient());
			tsi.setStorageRoot(action.getExecutionContext().getWorkingDirectory());
			int result=ActionResult.UNKNOWN;
			try{
				XnjsFileWithACL f=tsi.getProperties(TSIUtils.EXITCODE_FILENAME);
				if(f!=null){
					result=ActionResult.SUCCESSFUL;
				}
			}
			catch(Exception e){}
			action.getResult().setStatusCode(result);
			action.setDirty();
		}
		
		protected boolean isRecreated(){
			boolean recreated=false;
			try{
				JobDefinitionDocument job=(JobDefinitionDocument)action.getAjd();
				recreated="Recreated job".equals(job.getJobDefinition().
						getJobDescription().getJobIdentification().getJobAnnotationArray(0));
			}catch(Exception ex){}
			return recreated;
		}
		
	}

}
