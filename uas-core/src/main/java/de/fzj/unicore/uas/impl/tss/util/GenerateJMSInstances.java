package de.fzj.unicore.uas.impl.tss.util;

import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.persist.PersistenceException;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.impl.tss.TargetSystemImpl;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.uas.xnjs.XNJSFacade;
import de.fzj.unicore.xnjs.ems.Action;
import eu.unicore.security.Client;
import eu.unicore.services.Home;
import eu.unicore.services.Kernel;
import eu.unicore.services.security.util.AuthZAttributeStore;

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

	private final List<String>jobIDs;

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

	List<String>getXNJSJobs()throws Exception{
		XNJSFacade xnjs=XNJSFacade.get(xnjsReference, kernel);
		return xnjs.listJobIDs(client);
	}
	
	public void run(){
		try{
			logger.info("Regenerating UNICORE jobs for "+client.getDistinguishedName());
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
						logger.error("Could not restore job for : "+action.getUUID(),ex);
					}
				}
			}
			logger.info("Restored <"+num+"> UNICORE jobs for "+client.getDistinguishedName());
		}catch(Exception ex){
			logger.error("Could not restore jobs for "+client,ex);
		}
	}

	protected boolean accept(Action action){
		//do not accept internal actions
		if(action.isInternal()){
			return false;
		}
		//only accept jsdl actions
		String type=action.getType();
		if(!"JSDL".equals(type))return false;
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

	private Collection<String>getExistingJobs()throws PersistenceException{
		return jms.getStore().getUniqueIDs();
	}

}
