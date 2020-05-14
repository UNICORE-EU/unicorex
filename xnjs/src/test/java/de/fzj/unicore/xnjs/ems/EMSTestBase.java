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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.junit.Before;

import com.google.inject.AbstractModule;

import de.fzj.unicore.xnjs.XNJSTestBase;
import de.fzj.unicore.xnjs.persistence.IActionStoreFactory;
import de.fzj.unicore.xnjs.persistence.JDBCActionStoreFactory;
import de.fzj.unicore.xnjs.util.IOUtils;


/**
 * setup tests of the core ems processing
 */
public abstract class EMSTestBase extends XNJSTestBase {

	protected Manager mgr;

	protected InternalManager internalMgr;

	@Before
	public void setUp2() throws Exception {
		internalMgr=xnjs.get(InternalManager.class);
		mgr=xnjs.get(Manager.class);
	}

	@Override
	protected AbstractModule getPersistenceModule(){
		return new AbstractModule() {
			
			@Override
			protected void configure() {
				bind(IActionStoreFactory.class).to(JDBCActionStoreFactory.class);
			}
		};
	}
	
	/**
	 * build a JobDefinition document from an XML file on the file system
	 * @param name
	 * @return
	 */
	protected JobDefinitionDocument getJSDLDoc(String name) throws Exception {
		JobDefinitionDocument jdd;
		InputStream is=null;
		try {
			is=getResource(name);
			assertNotNull(is);
			jdd=JobDefinitionDocument.Factory.parse(is);
			return jdd;
		}finally{
			IOUtils.closeQuietly(is);
		} 
	}

	/**
	 * wait until the specified action becomes "ready" with a 60 seconds timeout
	 * @param actionID
	 * @throws Exception
	 */
	protected void waitUntilReady(String actionID)throws Exception{
		assertNotNull(actionID);
		int count=0;
		int status=0;
		do{
			status=mgr.getStatus(actionID,null).intValue();
			Thread.sleep(1000);
			if(ActionStatus.DONE==status){
				fail("Action already done->must have failed");
			}
			count++;
		}while(count<getTimeOut() && !("READY".equals(ActionStatus.toString(status))));
		if(count>=getTimeOut())throw new Exception("Timeout");
	}

	/**
	 * wait until the specified action becomes "done" with a timeout defined by getTimeOut()
	 * @param actionID
	 * @throws Exception
	 */
	protected void waitUntilDone(String actionID)throws Exception{
		assertNotNull(actionID);
		int status=0;
		int count=0;
		do{
			status=mgr.getStatus(actionID,null).intValue();
			Thread.sleep(1000);
			count++;
		}while(count<getTimeOut() && !("DONE".equals(ActionStatus.toString(status))));
		if(count>=getTimeOut())throw new Exception("Timeout");
	}
	
	//timeout in seconds
	protected int getTimeOut(){
		return 60;
	}

	protected void doRun(String actionID)throws Exception{
		doRun(actionID,null);
	}

	protected void doRun(String actionID, Runnable callback)throws Exception{
		assertNotNull(actionID);
		waitUntilReady(actionID);
		mgr.run(actionID,null);
		waitUntilDone(actionID);
		if(callback!=null)callback.run();
	}
	
	protected void assertSuccessful(String actionID)throws Exception{
		Action a=xnjs.get(InternalManager.class).getAction(actionID);
		assertNotNull(a);
		int result = a.getResult().getStatusCode();
		assertEquals("Action is "+a.getResult(), ActionResult.SUCCESSFUL, result);
	}
	
	protected void assertNotSuccessful(String actionID)throws Exception{
		Action a=xnjs.get(InternalManager.class).getAction(actionID);
		assertNotNull(a);
		assertTrue(ActionResult.SUCCESSFUL!=a.getResult().getStatusCode());
	}
	
	protected void printActionLog(String actionID)throws Exception{
		Action a=xnjs.get(InternalManager.class).getAction(actionID);
		assertNotNull(a);
		a.printLogTrace();
	}

}
