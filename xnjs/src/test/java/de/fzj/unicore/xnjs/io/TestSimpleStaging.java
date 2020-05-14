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
 

package de.fzj.unicore.xnjs.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.List;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.DataStagingType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.junit.Test;

import de.fzj.unicore.xnjs.jsdl.JSDLUtils;

public class TestSimpleStaging {

	@Test
	//test my understanding of URI :)
	public void testURI()throws Exception{
		URI uri=new URI("http://foo:1234/index.html?query#ref");
		System.out.println(uri);
		assertEquals(uri.getScheme(),"http");
		assertEquals(uri.getHost(),"foo");
		assertEquals(uri.getPort(),1234);
		assertEquals(uri.getQuery(),"query");
		assertEquals(uri.getRawPath(),"/index.html");
		assertEquals(uri.getFragment(),"ref");
	}
	
	@Test
	public void testDSTFIx()throws Exception{
		JobDefinitionDocument job=JobDefinitionDocument.Factory.newInstance();
		DataStagingType ds=job.addNewJobDefinition().addNewJobDescription().addNewDataStaging();
		ds.addNewTarget().setURI("infile.txt");
		ds.addNewSource().setURI("http://server.org/infile.txt");
		List<DataStagingType> list=JSDLUtils.getStageInArrayAsList(job);
		System.out.println(list.get(0).getTarget().getURI());
		assertTrue(list.get(0).getTarget().getURI().startsWith("file://"));
		
		JobDefinitionDocument job1=JobDefinitionDocument.Factory.newInstance();
		DataStagingType ds1=job1.addNewJobDefinition().addNewJobDescription().addNewDataStaging();
		ds1.addNewTarget().setURI("http://server/infile.txt");
		ds1.addNewSource().setURI("infile.txt");
		list=JSDLUtils.getStageInArrayAsList(job);
		assertTrue(list.get(0).getTarget().getURI().startsWith("file://"));
	}

}

