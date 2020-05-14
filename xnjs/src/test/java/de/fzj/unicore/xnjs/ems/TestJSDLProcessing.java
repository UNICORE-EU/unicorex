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


package de.fzj.unicore.xnjs.ems;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.junit.Test;

import de.fzj.unicore.xnjs.jsdl.JSDLProcessor;
import de.fzj.unicore.xnjs.jsdl.JSDLUtils;
import eu.unicore.bugsreporter.annotation.FunctionalTest;
import eu.unicore.bugsreporter.annotation.RegressionTest;
import eu.unicore.security.Client;

/**
 * setup and run tests of the core ems processing
 */
public class TestJSDLProcessing extends EMSTestBase {

	//jsdl document paths
	private static String 
	d1="src/test/resources/ems/date.jsdl",
	d2="src/test/resources/ems/ls_with_stagein.jsdl",
	d3="src/test/resources/ems/stagein_problem.jsdl",
	d4="src/test/resources/ems/non-zero-exit-code.jsdl",
	d5="src/test/resources/ems/email.jsdl",
	d7="src/test/resources/ems/empty.jsdl",
	d8="src/test/resources/ems/sleep.jsdl",
	
	d10="src/test/resources/ems/stageout_problem.jsdl",
	d11="src/test/resources/ems/date_with_inlinedata.jsdl",	
	//jsdl sweep examples
	d_s_1="src/test/resources/jsdl/sweep-example1.xml",
	d_s_2="src/test/resources/jsdl/sweep-example-with-stage-ins.xml",
	d_s_3="src/test/resources/jsdl/sweep-stage-ins.xml";

	@Test
	@FunctionalTest(id="testJSDLProcessing", 
	description="Tests processing of JSDL jobs by the XNJS")
	public void testJSDLAction() throws Exception {
		Action action=xnjs.makeAction(getJSDLDoc(d1));
		String id=action.getUUID();
		mgr.add(action,null);
		doRun(id);
		action=((BasicManager)mgr).getAction(id);
		assertEquals("my test job",action.getJobName());

		List<String>log=action.getLog();
		System.out.println(log.get(log.size()-1));

		assertSuccessful(id);

		//print stats
		String timeProfile=JSDLProcessor.getTimeProfile(action.getProcessingContext());
		assertTrue(timeProfile.contains("Total:"));
		System.out.println(timeProfile);

		// notifications
		MockChangeListener m = (MockChangeListener)xnjs.get(ActionStateChangeListener.class);
		assertTrue(m.getOrCreate(id).get()>0);
		System.out.println("State change notifications: "+m.getOrCreate(id).get());
		
		//test destroy()
		mgr.destroy(id, null);
		Thread.sleep(1000);
		Action a=xnjs.getActionStore("JOBS").get(id);
		assertNull(a);
	}

	@Test
	public void testJSDLAction2() throws Exception {
		String id=(String)mgr.add(xnjs.makeAction(getJSDLDoc(d2)),null);
		doRun(id);
		assertSuccessful(id);
	}

	@Test
	public void testJSDLActionSleep() throws Exception {
		String id=(String)mgr.add(xnjs.makeAction(getJSDLDoc(d8)),null);
		doRun(id);
		assertSuccessful(id);
	}

	@Test
	public void testJSDLInlineData() throws Exception {
		String id=(String)mgr.add(xnjs.makeAction(getJSDLDoc(d11)),null);
		doRun(id);
		assertSuccessful(id);
	}

	@Test
	public void testFaultyJSDL1() throws Exception {
		String id=(String)mgr.add(xnjs.makeAction(getJSDLDoc(d3)),null);
		waitUntilDone(id);
		Action a=internalMgr.getAction(id);
		ActionResult res=a.getResult();
		assertFalse(res.isSuccessful());
		System.out.println(res.getErrorMessage());
	}

	@Test
	public void testFailingStageout() throws Exception {
		String id=(String)mgr.add(xnjs.makeAction(getJSDLDoc(d10)),null);
		doRun(id);
		Action a=internalMgr.getAction(id);
		ActionResult res=a.getResult();
		assertFalse(res.isSuccessful());
		System.out.println(res.getErrorMessage());
	}
	
	@Test
	public void testFailOnNonzeroExitcode() throws Exception {
		String id=(String)mgr.add(xnjs.makeAction(getJSDLDoc(d4)),null);
		doRun(id);
		Action a=internalMgr.getAction(id);
		assertTrue(a.getExecutionContext().getExitCode()!=0);
		ActionResult res=a.getResult();
		assertFalse(res.isSuccessful());
		System.out.println(res.getErrorMessage());
	}
	
	@Test
	public void testIgnoreNonzeroExitcode() throws Exception {
		JobDefinitionDocument jdd = getJSDLDoc(d4);
		jdd.getJobDefinition().getJobDescription().addNewJobIdentification().addJobAnnotation("IgnoreNonZeroExitCode: true");
		String id=(String)mgr.add(xnjs.makeAction(jdd),null);
		doRun(id);
		Action a=internalMgr.getAction(id);
		assertTrue(a.getExecutionContext().getExitCode()!=0);
		ActionResult res=a.getResult();
		assertTrue(res.isSuccessful());
		System.out.println(res.getErrorMessage());
	}
	
	@Test
	@RegressionTest(id=3102308,
	date="2011-18-02",
	url="https://sourceforge.net/tracker/?func=detail&aid=3102308&group_id=102081&atid=633902", 
	description="Tests that user email specified in JSDL is used.")
	public void testUserEmail() throws Exception {
		Client c=new Client();
		c.setAnonymousClient();
		String id=(String)mgr.add(xnjs.makeAction(getJSDLDoc(d5)),c);
		doRun(id);
		Action j=internalMgr.getAction(id);
		assertEquals("test@foo.bar",j.getClient().getUserEmail());
	}

	@Test
	public void testEmptyJSDL() throws Exception {
		String id=(String)mgr.add(xnjs.makeAction(getJSDLDoc(d7)),null);
		doRun(id);
		assertSuccessful(id);
	}

	@Test
	public void testScheduledProcessing() throws Exception {
		Action a = xnjs.makeAction(getJSDLDoc(d1));
		int delay=10000;
		long start=System.currentTimeMillis();
		a.setNotBefore(start+delay);
		String id=(String)mgr.add(a,null);
		doRun(id);
		a=internalMgr.getAction(id);
		ActionResult res=a.getResult();
		assertTrue(res.isSuccessful());
		long end=System.currentTimeMillis();
		assertTrue(end-start>delay);
		List<String>log=a.getLog();
		for(String s: log){
			if(s.contains("Further processing scheduled"))System.out.println(s);
		}
	}

	@Test
	public void testScheduledProcessingFromJSDL() throws Exception {
		JobDefinitionDocument jdd=getJSDLDoc(d1);
		int delay=10000;
		long start=System.currentTimeMillis();
		String sD="notBefore:"+JSDLUtils.getDateFormat().format(new Date(start+delay));
		jdd.getJobDefinition().getJobDescription().getJobIdentification().addJobAnnotation(sD);
		System.out.println(jdd);
		String id=(String)mgr.add(xnjs.makeAction(jdd),null);
		doRun(id);
		Action a=internalMgr.getAction(id);
		ActionResult res=a.getResult();
		assertTrue(res.isSuccessful());
		long end=System.currentTimeMillis();
		assertTrue(end-start>delay/2);
		List<String>log=a.getLog();
		for(String s: log){
			if(s.contains("Further processing scheduled"))System.out.println(s);
		}
	}

	@Test
	public void testSweepWithAutoStart() throws Exception {
		String id=(String)mgr.add(xnjs.makeAction(getJSDLDoc(d_s_1)),null);
		doRun(id);
		assertSuccessful(id);
		printActionLog(id);
	}

	@Test
	public void testSweepWithClientStart() throws Exception {
		String id=(String)mgr.add(xnjs.makeAction(getJSDLDoc(d_s_1)),null);
		doRun(id);
		assertSuccessful(id);
	}

	@Test
	public void testSweepWithStageIn() throws Exception {
		String id=(String)mgr.add(xnjs.makeAction(getJSDLDoc(d_s_2)),null);
		doRun(id);
		assertSuccessful(id);
		printActionLog(id);
		mgr.destroy(id, null);
		Thread.sleep(1000);
	}

	@Test
	public void testSweepWithSweepedStageIns() throws Exception {
		String id=(String)mgr.add(xnjs.makeAction(getJSDLDoc(d_s_3)),null);
		doRun(id);assertSuccessful(id);
		printActionLog(id);
		mgr.destroy(id, null);
		Thread.sleep(1000);
	}
}
