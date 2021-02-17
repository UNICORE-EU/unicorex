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


package de.fzj.unicore.xnjs.jsdl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.apache.xmlbeans.XmlObject;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ApplicationDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ApplicationType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.DataStagingType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ProcessorArchitectureEnumeration;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ResourcesDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ResourcesType;
import org.ggf.schemas.jsdl.x2005.x11.jsdlPosix.ArgumentDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdlPosix.POSIXApplicationDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdlPosix.POSIXApplicationType;
import org.junit.Test;

import de.fzj.unicore.xnjs.ems.ExecutionContext;
import de.fzj.unicore.xnjs.io.impl.UsernamePassword;
import de.fzj.unicore.xnjs.tsi.remote.TSIUtils;
import de.fzj.unicore.xnjs.util.XmlBeansUtils;

public class TestUtils {

	private InputStream getResource(String name){
		InputStream is = getClass().getResourceAsStream(name);
		if(is==null){
			try{
				is=new FileInputStream(name);
			}catch(Exception e){}
		}
		return is;
	}

	@Test
	public void testAppend(){
		ApplicationDocument app=ApplicationDocument.Factory.newInstance();
		ApplicationType appl=app.addNewApplication();		
		appl.setApplicationName("Date");
		appl.setApplicationVersion("1.0");
		POSIXApplicationDocument pad=POSIXApplicationDocument.Factory.newInstance();
		pad.addNewPOSIXApplication();
		pad.getPOSIXApplication().addNewExecutable().setStringValue("/bin/date");
		XmlBeansUtils.append(pad,appl);
		assertTrue(app.toString().contains("Date"));
		assertTrue(app.toString().contains("1.0"));
		assertTrue(app.toString().contains("/bin/date"));
	}

	@Test
	public void testStageIn() throws Exception{
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.newInstance();
		DataStagingType dst=jdd.addNewJobDefinition().addNewJobDescription().addNewDataStaging();
		dst.addNewSource().setURI("http://foo.org/myfile");
		boolean hasStageIn=JSDLUtils.hasStageIn(jdd);
		assertTrue(hasStageIn);
		assertFalse(JSDLUtils.hasStageOut(jdd));
	}

	@Test
	public void testStageOut() throws Exception{
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.newInstance();
		DataStagingType dst=jdd.addNewJobDefinition().addNewJobDescription().addNewDataStaging();
		dst.addNewTarget().setURI("http://my.org/out.txt");
		boolean hasStageIn=JSDLUtils.hasStageIn(jdd);
		assertFalse(hasStageIn);
		assertTrue(JSDLUtils.hasStageOut(jdd));
	}

	@Test
	public void testExtractReservationID()throws Exception{
		JobDefinitionDocument jd=JobDefinitionDocument.Factory.newInstance();
		ResourcesType rt=jd.addNewJobDefinition().addNewJobDescription().addNewResources();
		rt.addNewCPUArchitecture().setCPUArchitectureName(ProcessorArchitectureEnumeration.X_86);
		String resID="<ReservationReference xmlns=\"http://www.unicore.eu/unicore/xnjs\">1234</ReservationReference>";
		XmlObject o=XmlObject.Factory.parse(resID);
		ResourcesDocument rd=ResourcesDocument.Factory.newInstance();
		rd.setResources(rt);
		XmlBeansUtils.append(o, rd);
		rt=rd.getResources();
		assertTrue(rt.toString().contains("1234"));
		String id=TSIUtils.extractReservationID(rt);
		assertTrue("1234".equals(id));
	}

	@Test
	public void testExtractReservationID_2()throws Exception{
		String rr="<jsdl:Resources xmlns:jsdl=\"http://schemas.ggf.org/jsdl/2005/11/jsdl\"><jsdl:IndividualCPUTime>"+
		"<jsdl:Exact>60.0</jsdl:Exact>"+
		"</jsdl:IndividualCPUTime>"+
		"<jsdl:IndividualCPUCount>"+
		"    <jsdl:Exact>1.0</jsdl:Exact>"+
		"  </jsdl:IndividualCPUCount>"+
		"   <jsdl:TotalResourceCount>"+
		"      <jsdl:Exact>1.0</jsdl:Exact>"+
		"     </jsdl:TotalResourceCount>"+
		"      <u6rr:ReservationReference "+
		"xmlns:u6rr=\"http://www.unicore.eu/unicore/xnjs\">041314212111</u6rr:ReservationReference>"+
		"</jsdl:Resources>";
		ResourcesDocument dd=ResourcesDocument.Factory.parse(rr);
		ResourcesType rt=dd.getResources();
		String id=TSIUtils.extractReservationID(rt);
		assertTrue("041314212111".equals(id));
	}

	@Test
	public void testExtractUserEmail(){
		String[] test=new String[]{"email=foo@bar.org","user email: foo@bar.org", "User email: foo@bar.org",  "User email=foo@bar.org"};
		String[] expected=new String[]{null, "foo@bar.org", "foo@bar.org", "foo@bar.org"};
		for(int i=0;i<test.length;i++){
			String email = JSDLParser.getEmailAddress(test[i]);
			if(email!=null){
				assertEquals(expected[i],email);
			}
		}
		for(int i=0;i<test.length;i++){
			JobDefinitionDocument j=JobDefinitionDocument.Factory.newInstance();
			j.addNewJobDefinition().addNewJobDescription().addNewJobIdentification().addJobAnnotation(test[i]);
			assertEquals(expected[i],JSDLParser.extractEmail(j));
		}
	}

	@Test
	public void testAppendEnvironment(){
		ExecutionContext ec=new ExecutionContext(UUID.randomUUID().toString());
		ec.getEnvironment().put("foo","bar");
		StringBuilder sb=new StringBuilder();
		TSIUtils.appendEnvironment(sb, ec, true);
		assertTrue(sb.toString().contains("foo=\"bar\"; export foo"));
	}

	@Test
	public void testExtractAttributes()throws Exception{
		String app=
			"<jsdl:Argument xmlns:jsdl=\"http://schemas.ggf.org/jsdl/2005/11/jsdl-posix\"" +
			" Type=\"boolean\""+ 
			" Description=\"Verbose Execution\">-v$VERBOSE?</jsdl:Argument>";
		ArgumentDocument arg=ArgumentDocument.Factory.parse(app);
		Map<String,String>attributes=XmlBeansUtils.extractAttributes(arg);
		assertEquals(2,attributes.size());
		assertEquals("boolean",attributes.get("Type"));
	}

	@Test
	public void testExtractScheduledStartTime()throws Exception{
		long d0=System.currentTimeMillis()+3600*1000;
		String date=JSDLUtils.getDateFormat().format(new Date(d0));
		String s1="scheduledStartTime="+date;
		System.out.println(s1);
		long d1=JSDLUtils.getDateFormat().parse(JSDLParser.getTagValue(s1, "scheduledStartTime")).getTime();
		assertTrue(Math.abs( d0-d1 )< 1100 ); //ignore millis for comparison
		
		String s2="scheduledStartTime: "+date;
		d1=JSDLUtils.getDateFormat().parse(JSDLParser.getTagValue(s2, "scheduledStartTime")).getTime();
		assertTrue(Math.abs( d0-d1 )< 1100 ); //ignore millis for comparison
	}

}
