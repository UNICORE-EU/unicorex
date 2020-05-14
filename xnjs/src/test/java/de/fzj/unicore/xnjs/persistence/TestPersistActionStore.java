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
 

package de.fzj.unicore.xnjs.persistence;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.junit.Before;
import org.junit.Test;

import de.fzj.unicore.persist.impl.PersistImpl;
import de.fzj.unicore.persist.util.Export;
import de.fzj.unicore.xnjs.ConfigurationSource;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.BasicManager;
import de.fzj.unicore.xnjs.ems.EMSTestBase;
import de.fzj.unicore.xnjs.ems.ExecutionException;

public class TestPersistActionStore extends EMSTestBase {

	static JobDefinitionDocument jdd;
	
	@Before
	public void setUp3()throws Exception{
		System.setProperty(IActionStore.CLEAR_ON_STARTUP,"true");
	}

	//jsdl document paths
	private static String smallDoc="src/test/resources/ems/date.jsdl";
	private static String bigDoc="src/test/resources/ems/big.jsdl";
	private static String faultyJob="src/test/resources/ems/stageout_problem.jsdl";
	
	@Override
	protected void addProperties(ConfigurationSource cs){
		super.addProperties(cs);
		cs.getProperties().put("XNJS.autosubmit","true");
	}
	
	@Test
	public void test1()throws Exception{
		doTest(1);
	}

	@Test
	public void testN()throws Exception{
		doTest(5);
	}
	
	@Test
	public void testFailing()throws Exception{
		doTest(5, true, false, faultyJob, 2);
	}
	
	@Test
	public void testNLargeJob()throws Exception{
		((JDBCActionStore)xnjs.getActionStore("JOBS")).doCleanup();
		doTest(5,false,false,bigDoc, 1);		
	}
	
	@Test
	public void testExport()throws Exception{
		JDBCActionStore as = (JDBCActionStore)xnjs.getActionStore("JOBS");
		as.doCleanup();
		doTest(2,false,false,smallDoc,1);
		Export exporter=new Export((PersistImpl<?>)as.getDoneJobsStorage());
		exporter.doExport();
	}
	
	private void doTest(int n)throws Exception{
		doTest(n, false, false, smallDoc, 1);
	}
	
	private List<String> doTest(int n, boolean checkSuccess, boolean expectSuccess, String doc, int actionsPerJob)throws Exception{
		((JDBCActionStore)xnjs.getActionStore("JOBS")).doCleanup();
		
		long start,end;
		
		ArrayList<String> ids=new ArrayList<String>();
		JobDefinitionDocument job=getJSDLDoc(doc);
		start=System.currentTimeMillis();
		System.out.println("adding "+n+" jobs.");
		try {
			String id="";
			for(int i=0;i<n;i++){
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
		System.out.println("Took: "+(time)+" secs.");
		int c=0;
		
		System.out.println("Waiting for jobs to finish.");
		while(((BasicManager)mgr).getDoneJobs()<actionsPerJob*n && c<n){
			Thread.sleep(1000);
			c++;
		}
		System.out.println(((JDBCActionStore)xnjs.getActionStore("JOBS")).printDiagnostics());
		
		end=System.currentTimeMillis();
		time=(0.0f+(end-start)/1000);
		
		System.out.println("Took: "+(time)+" secs.");
		System.out.println("Rate: "+(n/time+0.0f)+" per sec.");
		
		int success = 0;
		if(checkSuccess){
			for(String id : ids){
				Action a = ((BasicManager)mgr).getAction(id);
				if(expectSuccess){
					if(a.getResult().isSuccessful())success++;
				}
				else{
					if(!a.getResult().isSuccessful())success++;
					else{
						System.out.println("Result for "+id+": "+a.getResult());
					}
				}
			}
			System.out.println("Check expected result: "+success+" Jobs are OK.");
			if(n!=success)fail("Some jobs are not OK: "+(n-success)+" jobs not have the expected results.");
		}
		
		return ids;
	}
}
