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
 ********************************************************************************/
 
package de.fzj.unicore.xnjs.tsi.remote;

import static org.junit.Assert.*;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.junit.Test;

import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.tsi.remote.TSIConnection;
import de.fzj.unicore.xnjs.tsi.remote.TSIConnectionFactory;
import eu.unicore.security.Client;
import eu.unicore.security.Xlogin;


public class MultiAddressTSITest extends LegacyTSITestCase {

	private static String 
		d1="src/test/resources/ems/date.jsdl";
	
	@Override
	protected String getTSIMachine(){
		return "localhost localhost, localhost";
	}
	
	@Test
	public void testConnectionFactory()throws Exception{
		TSIConnectionFactory f=xnjs.get(TSIConnectionFactory.class);
		assertNotNull(f);
		TSIConnection c=f.getTSIConnection("nobody", null, null, -1);
		System.out.println("TSI "+c.getTSIVersion()+" isAlive="+c.isAlive());
		System.out.println(c);
		System.out.println(c.getConnectionID());
		c.done();
	}
	
	@Test
	public void testConnectionFactory2()throws Exception{
		TSIConnectionFactory f=xnjs.get(TSIConnectionFactory.class);
		assertNotNull(f);
		TSIConnection c;
		try {
			c=f.getTSIConnection("nobody", null, "no such node", -1);
		}catch(Exception ex) {
			assertTrue(ex.getMessage().contains("No TSI is configured at"));
			System.out.println("EX: "+ex.getMessage());
		}
		c=f.getTSIConnection("nobody", null, "localh*", -1);
		System.out.println("TSI "+c.getTSIVersion()+" isAlive="+c.isAlive());
		System.out.println(c);
		System.out.println(c.getConnectionID());
		c.done();
	}
	
	@Test
	public void testRunJSDL() throws Exception {
		TSIConnectionFactory cf=xnjs.get(TSIConnectionFactory.class);
		assertNotNull(cf);
		assertEquals(3,cf.getTSIHosts().length);
	
		JobDefinitionDocument job=getJSDLDoc(d1);
		String id="";
		Action a=xnjs.makeAction(job);
		Client c=new Client();
		c.setXlogin(new Xlogin(new String[] {"nobody"}));
		id=a.getUUID();
		mgr.add(a,c);
		doRun(id);
		assertSuccessful(id);
	}
}
