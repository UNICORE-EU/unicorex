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

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Properties;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.junit.Ignore;
import org.junit.Test;

import de.fzj.unicore.xnjs.ConfigurationSource;
import de.fzj.unicore.xnjs.ems.BasicManager;
import de.fzj.unicore.xnjs.ems.EMSTestBase;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.persistence.IActionStore;
import de.fzj.unicore.xnjs.persistence.JDBCActionStore;

@Ignore
public class LoadTest extends EMSTestBase {

	static JobDefinitionDocument jdd;

	private static String smallDoc="src/test/resources/ems/date.jsdl";
	private static String bigDoc="src/test/resources/ems/big.jsdl";
	private static String d1="src/test/resources/ems/ls_with_stagein.jsdl";

	@Override
	protected void addProperties(ConfigurationSource cs){
		super.addProperties(cs);
		Properties props = cs.getProperties();
		System.setProperty(IActionStore.CLEAR_ON_STARTUP,"true");
		props.put("XNJS.autosubmit","true");
		props.put("XNJS.numberofworkers","4");
	}

	protected int getNumberOfTasks(){
		return 10;
	}
	
	@Test
	public void testMultipleSmallJobs()throws Exception{
		((JDBCActionStore)xnjs.getActionStore("JOBS")).doCleanup();
		int n=getNumberOfTasks();

		long start,end;

		ArrayList<String> ids=new ArrayList<String>();
		JobDefinitionDocument job=getJSDLDoc(smallDoc);
		start=System.currentTimeMillis();
		System.out.println("adding "+n+" jobs.");
		String id="";
		for(int i=0;i<n;i++){
			id=(String)mgr.add(
					xnjs.makeAction(job),null);
			ids.add(id);
		}

		end=System.currentTimeMillis();
		float time=(0.0f+(end-start)/1000);
		System.out.println("Took: "+(time)+" secs.");
		int c=0;
		System.out.println("Waiting for jobs to finish.");
		while(((BasicManager)mgr).getDoneJobs()<n && c<n){
			Thread.sleep(1000);
			c++;
		}
		System.out.println(((JDBCActionStore)xnjs.getActionStore("JOBS")).printDiagnostics());

		end=System.currentTimeMillis();
		time=(0.0f+(end-start)/1000);

		System.out.println("Took: "+(time)+" secs.");
		System.out.println("Rate: "+(n/time+0.0f)+" per sec.");

	}

	@Test
	public void testMultipleLargeJobs() throws Exception {
		((JDBCActionStore)xnjs.getActionStore("JOBS")).doCleanup();
		int n=getNumberOfTasks();
		long start,end;

		ArrayList<String> ids=new ArrayList<String>();
		JobDefinitionDocument job=getJSDLDoc(bigDoc);

		start=System.currentTimeMillis();
		System.out.println("adding "+n+" jobs.");
		String id="";
		for(int i=0;i<n;i++){
			id=(String)mgr.add(xnjs.makeAction(job),null);
			ids.add(id);
		}
		end=System.currentTimeMillis();
		float time=(0.0f+(end-start)/1000);
		System.out.println("Took: "+(time)+" secs.");
		System.out.println("Waiting for jobs to finish.");
		int c=0;
		while(((BasicManager)mgr).getDoneJobs()<n && c < n){
			Thread.sleep(1000);
			c++;
		}
		System.out.println(((JDBCActionStore)xnjs.getActionStore("JOBS")).printDiagnostics());
		end=System.currentTimeMillis();
		time=(0.0f+(end-start)/1000);
		System.out.println("Took: "+(time)+" secs.");
		System.out.println("Rate: "+(n/time+0.0f)+" per sec.");

	}

	@Test
	public void testMultipleJobsWithStaging()throws Exception{
		int n=getNumberOfTasks();
		
		long start,end;
		
		ArrayList<String> ids=new ArrayList<String>();
		xnjs.setProperty("XNJS.autosubmit","true");
		
		start=System.currentTimeMillis();
		try {
			String id="";
			for(int i=0;i<n;i++)
				id=(String)mgr.add(xnjs.makeAction(getJSDLDoc(d1)),null);
				ids.add(id);
		} catch (ExecutionException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		while(((BasicManager)mgr).getDoneJobs()<2*n){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		end=System.currentTimeMillis();
		float time=((float)(end-start))/1000;
		System.out.println("Took: "+time+" secs");
		System.out.println("Rate: "+(n/time+0.0f)+" per sec.");
	}
}
