package de.fzj.unicore.uas.impl.tss.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;

import de.fzj.unicore.persist.PersistenceException;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.impl.job.JobManagementImpl;
import de.fzj.unicore.uas.impl.tss.TargetSystemImpl;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.exceptions.ResourceUnknownException;
import de.fzj.unicore.wsrflite.security.util.AuthZAttributeStore;
import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.security.Client;
import eu.unicore.util.Log;

/**
 * Re-creates the list of accessible job references in a TSS as it is created.
 * This only happens if it is the only TSS for the user.
 * <p>
 * We must be careful here: as this is invoked from the thread pool, the client 
 * identity must be set upon creation (that's why we don't use the actual thread-local
 * user).
 * 
 * @author schuller
 */
public class RecreateJMSReferenceList implements Runnable{

	private final Logger logger = LogUtil.getLogger(LogUtil.JOBS, RecreateJMSReferenceList.class);

	private final String tssID;

	private final Client client;

	private final Home tssHome;

	private final Home jms;

	public RecreateJMSReferenceList(Kernel kernel, String tssID, Client client)throws PersistenceException{
		this.tssID=tssID;
		this.client=client;
		this.tssHome=kernel.getHome(UAS.TSS);
		this.jms=kernel.getHome(UAS.JMS);
	}

	public void run(){
		try{
			AuthZAttributeStore.setClient(client);
			String user = client.getDistinguishedName();
			logger.info("Re-generating job list for " + X500NameUtils.getReadableForm(user));
			
			//check if owner has more TSSs
			Collection<String>tssIds=tssHome.getStore().getUniqueIDs();
			tssIds.remove(tssID);
			Collection<String>tssIdsOwnedByUser = new HashSet<>();
			for(String id: tssIds){
				TargetSystemImpl t=(TargetSystemImpl)tssHome.get(id);
				if (X500NameUtils.equal(t.getOwner(), user)){
					tssIdsOwnedByUser.add(id);
				}
			}
			
			List<String>oldJobs=new ArrayList<String>();
			for(String jobID: getExistingJobs()){
				try{
					JobManagementImpl j=(JobManagementImpl)jms.get(jobID);
					// skip if this job is already listed by another TSS
					// or is marked as not re-attachable
					if(tssIdsOwnedByUser.contains(j.getModel().getParentUID()) 
						|| !j.getModel().isReAttachable()){
						continue;
					}
					if(j.getOwner()==null || X500NameUtils.equal(j.getOwner(), user)){
						oldJobs.add(jobID);
						try{
							j=(JobManagementImpl)jms.getForUpdate(jobID);
							j.setTSSID(tssID);
							jms.persist(j);
						}catch(Exception ex){
							Log.logException("Could not change TSS ID of job <"+jobID+">", ex, logger);
						}
					}
				}catch(ResourceUnknownException re){
					logger.debug("Job <"+jobID+"> not found any more.");
				}
			}
			TargetSystemImpl tss=null;
			try{
				try{
					tss=(TargetSystemImpl)tssHome.getForUpdate(tssID);	
				}catch(ResourceUnknownException rue){
					logger.error(rue);
				}
				List<String> ids = tss.getModel().getJobIDs();
				int count = 0;
				for(String id: oldJobs){
					if(!ids.contains(id)){
						ids.add(id);
						count++;
					}
				}
				logger.info("Added <"+count+"> existing jobs to new target system");
			}
			finally{
				tssHome.getStore().persist(tss);
			}
		}catch(Exception ex){
			logger.error("Could not restore jobs for "+client.getDistinguishedName(),ex);
		}
	}

	private Collection<String>getExistingJobs()throws PersistenceException{
		return jms.getStore().getUniqueIDs();
	}

}
