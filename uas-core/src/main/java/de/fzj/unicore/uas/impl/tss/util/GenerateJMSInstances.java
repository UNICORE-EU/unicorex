package de.fzj.unicore.uas.impl.tss.util;

import java.util.Collection;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.impl.tss.TargetSystemImpl;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.uas.xnjs.XNJSFacade;
import eu.unicore.security.Client;
import eu.unicore.services.Home;
import eu.unicore.services.Kernel;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.xnjs.XNJSConstants;
import eu.unicore.xnjs.ems.Action;

/**
 * re-create JMS instances from XNJS jobs
 * 
 * @author schuller
 */
public class GenerateJMSInstances implements Runnable{

	private final Logger logger = LogUtil.getLogger(LogUtil.JOBS, GenerateJMSInstances.class);

	private final String tssID;

	private final Client client;

	private final String xnjsReference;

	private final Home tssHome;

	private final Home jms;

	private final Collection<String>existingJobs;

	private final Collection<String>jobIDs;

	private final Kernel kernel;
	
	public GenerateJMSInstances(Kernel kernel, String tssID, Client client, String xnjsReference)throws Exception{
		this.kernel=kernel;
		this.tssID=tssID;
		this.client=client;
		this.xnjsReference=xnjsReference;
		this.tssHome=kernel.getHome(UAS.TSS);
		this.jms=kernel.getHome(UAS.JMS);
		this.existingJobs=getExistingJobs();
		this.jobIDs=getXNJSJobs();
	}

	Collection<String>getXNJSJobs()throws Exception{
		XNJSFacade xnjs=XNJSFacade.get(xnjsReference, kernel);
		return xnjs.listJobIDs(client);
	}
	
	public void run(){
		try{
			logger.info("Regenerating UNICORE jobs for {}", client.getDistinguishedName());
			AuthZAttributeStore.setClient(client);
			XNJSFacade xnjs=XNJSFacade.get(xnjsReference, kernel);
			int num = 0;
			for(String jobID: jobIDs){
				Action action=xnjs.getAction(jobID);
				if(action==null)continue;
				if(accept(action) && !jobExists(jobID)){
					try{
						add(action);
						num++;
					}catch(Exception ex){
						logger.error("Could not restore job: {}", action.getUUID(),ex);
					}
				}
			}
			logger.info("Restored <{}> UNICORE jobs for {}", num, client.getDistinguishedName());
		}catch(Exception ex){
			logger.error("Could not restore jobs for ", client.getDistinguishedName(), ex);
		}
	}

	protected boolean accept(Action action){
		//do not accept internal actions
		if(action.isInternal()){
			return false;
		}
		//only accept jobs
		String type=action.getType();
		if(!XNJSConstants.jobActionType.equals(type))return false;
		//and those which belong to the current user
		String owner=action.getClient().getDistinguishedName();
		return client.getDistinguishedName().equalsIgnoreCase(owner);
	}

	protected boolean jobExists(String uid){
		return existingJobs.contains(uid);
	}

	protected void add(Action action)throws Exception{
		try(TargetSystemImpl tss=(TargetSystemImpl)tssHome.getForUpdate(tssID)){
			tss.registerJob(tss.createJobResource(action,null));
		}
	}

	private Collection<String>getExistingJobs()throws Exception{
		return jms.getStore().getUniqueIDs();
	}

}
