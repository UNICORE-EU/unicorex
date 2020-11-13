/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *********************************************************************************/
 

package de.fzj.unicore.xnjs.performance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.junit.Test;

import de.fzj.unicore.xnjs.ConfigurationSource;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.ems.BasicManager;
import de.fzj.unicore.xnjs.ems.EMSTestBase;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.persistence.IActionStore;
import de.fzj.unicore.xnjs.tsi.local.LocalExecution;

public class MultiJobTest extends EMSTestBase {

	static JobDefinitionDocument jdd;
	
	//jsdl document paths
	private static String 
	    d1="src/test/resources/ems/date.jsdl";
	

	@Override
	protected void addProperties(ConfigurationSource cs){
		super.addProperties(cs);
		Properties props = cs.getProperties();
		System.setProperty(IActionStore.CLEAR_ON_STARTUP,"true");
		props.put("XNJS.autosubmit", "true");
		props.put("XNJS.numberofworkers", "4");
	}

	protected int getNumberOfTasks(){
		return 8;
	}
	
	@Test
	public void testMultipleJobs()throws Exception{
		int n=getNumberOfTasks(); //how many jobs
		
		assertTrue(xnjs.getXNJSProperties().isAutoSubmitWhenReady());
		
		long start,end;
		
		ArrayList<String> ids=new ArrayList<String>();
		JobDefinitionDocument job=getJSDLDoc(d1);
		
		start=System.currentTimeMillis();
		try {
			String id="";
			for(int i=0;i<n;i++){
				if(i%50==0 && i>0)System.out.println("Submitted "+i+" jobs.");
				id=(String)mgr.add(
						xnjs.makeAction(job),null);
				ids.add(id);
			}
		} catch (ExecutionException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		end=System.currentTimeMillis();
		float time=(0.0f+(end-start)/1000);
		System.out.println("All "+n+" jobs submitted in "+time+" secs.");
		System.out.println("Submission rate: "+(n/time+0.0f)+" per sec.");
		System.out.println("Using "+xnjs.getProperty("XNJS.numberofworkers")+" worker threads.");
		
		int p=0;
		int q=0;
		int c=0;
		int p_new=0;
		int timeout=60;
		try{
			do{
				p_new=((BasicManager)mgr).getDoneJobs();
				if(p==p_new){
					c++; 
				}else c=0;
				
				if(c>timeout){
					throw new InterruptedException("Timeout: job processing did not make progress for "
							+timeout+" seconds.");
				}
				
				p=p_new;
				q=((BasicManager)mgr).getAllJobs();
				int queued=((BasicManager)mgr).getActionStore().size(ActionStatus.QUEUED);
				int pending=((BasicManager)mgr).getActionStore().size(ActionStatus.PENDING);
				if(p>0)System.out.println("Processed "+p+" jobs (of "+q+", pending="+
						pending+", queued="+queued+") in "
						+(System.currentTimeMillis()-start)+ "ms.");
				Thread.sleep(1000);
			}while(p<n);
		}catch(InterruptedException e){
			e.printStackTrace();
			System.out.println("ERROR processing jobs.");
			System.out.println((((BasicManager)mgr).getActionStore()).printDiagnostics());
			Set<String> jobs=LocalExecution.getRunningJobs();
			System.out.println("Jobs in execution table: "+jobs.size());
			for(String s: jobs)System.out.println(s);
		}
		end=System.currentTimeMillis();
		time=(0.0f+(end-start)/1000);
		if(xnjs.getTargetSystemInterface(null).isLocal()){
			System.out.println("Tasks executed: "+LocalExecution.getCompletedTasks());
			System.out.println("Rejected tasks: "+LocalExecution.getNumberOfRejectedTasks());
		}

		System.out.println("Took: "+(time)+" secs.");
		System.out.println("Rate: "+(n/time+0.0f)+" per sec.");
		
		assertEquals(n,LocalExecution.getCompletedTasks());
	
	}
}
