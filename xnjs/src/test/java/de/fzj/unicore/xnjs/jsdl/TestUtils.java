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

import java.io.File;
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
import org.ggf.schemas.jsdl.x2006.x07.jsdlHpcpa.HPCProfileApplicationDocument;
import org.ggf.schemas.jsdl.x2006.x07.jsdlHpcpa.HPCProfileApplicationType;
import org.junit.Test;

import de.fzj.unicore.xnjs.ems.ExecutionContext;
import de.fzj.unicore.xnjs.io.impl.OAuthToken;
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
	public void testPosixAppExtraction() throws Exception{
		InputStream is = getResource("src/test/resources/jsdl/ex_posix.jsdl");
		assertNotNull(is);
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.parse(is);

		ApplicationDocument app=ApplicationDocument.Factory.newInstance();
		app.setApplication(jdd.getJobDefinition().getJobDescription().getApplication());

		POSIXApplicationDocument pd=JSDLUtils.extractPosixApplication(app);
		assertNotNull(pd);
		POSIXApplicationType pa=pd.getPOSIXApplication();
		assertEquals(pa.getExecutable().getStringValue(), "/bin/date");	
	}

	@Test
	public void testPosixAppExtraction2() throws Exception{
		InputStream is = getResource("src/test/resources/jsdl/sleep.jsdl");
		assertNotNull(is);
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.parse(is);
		//System.out.println(jdd);
		ApplicationDocument app=ApplicationDocument.Factory.newInstance();
		app.setApplication(jdd.getJobDefinition().getJobDescription().getApplication());
		POSIXApplicationDocument pd=JSDLUtils.extractPosixApplication(app);
		assertNotNull(pd);
		//System.out.println("After conversion: "+pd.toString());
		POSIXApplicationType pa=pd.getPOSIXApplication();
		assertEquals("10",pa.getArgumentArray(0).getStringValue());
	}

	@Test
	public void testJsdl1() throws Exception{
		InputStream is = getResource("src/test/resources/jsdl/empty.jsdl");
		assertNotNull(is);
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.parse(is);
		boolean hasStageIn=JSDLUtils.hasStageIn(jdd);
		assertFalse(hasStageIn);
		boolean hasStageOut=JSDLUtils.hasStageOut(jdd);
		assertFalse(hasStageOut);
	}

	@Test
	public void testJsdl2() throws Exception{
		InputStream is = getResource("src/test/resources/jsdl/staging_1.jsdl");
		assertNotNull(is);
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.parse(is);
		boolean hasStageIn=JSDLUtils.hasStageIn(jdd);
		assertTrue(hasStageIn);
		boolean hasStageOut=JSDLUtils.hasStageOut(jdd);
		assertFalse(hasStageOut);
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
	public void testGetStageIn() throws Exception {
		InputStream is = getResource("src/test/resources/jsdl/staging_1.jsdl");
		assertNotNull(is);
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.parse(is);
		assertEquals(JSDLUtils.getStageInArrayAsList(jdd).size(),1);
		assertEquals(JSDLUtils.getStageOutArrayAsList(jdd).size(),0);
	}

	@Test
	public void testGetStage2In() throws Exception {
		InputStream is = getResource("src/test/resources/jsdl/staging_2.jsdl");
		assertNotNull(is);
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.parse(is);
		assertEquals(JSDLUtils.getStageInArrayAsList(jdd).size(),2);
		assertEquals(JSDLUtils.getStageOutArrayAsList(jdd).size(),0);
	}

	@Test
	public void testGetStageOut() throws Exception {
		InputStream is = getResource("src/test/resources/jsdl/staging_3.jsdl");
		assertNotNull(is);
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.parse(is);
		assertEquals(JSDLUtils.getStageInArrayAsList(jdd).size(),0);
		assertEquals(JSDLUtils.getStageOutArrayAsList(jdd).size(),1);
	}

	@Test
	public void testGetStage2Out() throws Exception {
		InputStream is = getResource("src/test/resources/jsdl/staging_4.jsdl");
		assertNotNull(is);
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.parse(is);
		assertEquals(JSDLUtils.getStageInArrayAsList(jdd).size(),0);
		assertEquals(JSDLUtils.getStageOutArrayAsList(jdd).size(),2);
	}

	@Test
	public void testGetStageOut2() throws Exception {
		InputStream is =getResource("src/test/resources/jsdl/staging_3.jsdl");
		assertNotNull(is);
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.parse(is);
		assertEquals(JSDLUtils.getStageOutArrayAsList(jdd).size(),1);
		DataStagingType dst=JSDLUtils.getStageOutArrayAsList(jdd).get(0);
		assertEquals(dst.getTarget().getURI(),"http://foo.org/myfile");
	}

	@Test
	public void testHpcpAppExtraction() throws Exception{
		InputStream is = getResource("src/test/resources/jsdl/hpcp_sleep.jsdl");
		assertNotNull(is);
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.parse(is);
		//System.out.println(jdd);
		ApplicationDocument app=ApplicationDocument.Factory.newInstance();
		app.setApplication(jdd.getJobDefinition().getJobDescription().getApplication());
		HPCProfileApplicationDocument pd=JSDLUtils.extractHpcpApplication(app);
		assertNotNull(pd);
		//System.out.println("After conversion: "+pd.toString());
		HPCProfileApplicationType pa=pd.getHPCProfileApplication();
		assertEquals("10",pa.getArgumentArray(0).getStringValue());
	}

	@Test
	public void testHpcpAppExtraction2() throws Exception{
		InputStream is = getResource("src/test/resources/jsdl/hpcp_sleep.jsdl");
		assertNotNull(is);
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.parse(is);
		ApplicationDocument app=ApplicationDocument.Factory.newInstance();
		app.setApplication(jdd.getJobDefinition().getJobDescription().getApplication());
		POSIXApplicationDocument pd=JSDLUtils.extractUserApplication(app);
		assertNotNull(pd);
		POSIXApplicationType pa=pd.getPOSIXApplication();
		assertEquals("10",pa.getArgumentArray(0).getStringValue());
	}

	@Test
	public void testHpcpAppExtraction3() throws Exception{
		InputStream is = getResource("src/test/resources/jsdl/hpcp_sleep_other_namespace.jsdl");
		assertNotNull(is);
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.parse(is);
		//System.out.println(jdd);
		ApplicationDocument app=ApplicationDocument.Factory.newInstance();
		app.setApplication(jdd.getJobDefinition().getJobDescription().getApplication());
		HPCProfileApplicationDocument pd=JSDLUtils.extractHpcpApplication(app);
		assertNotNull(pd);
		//System.out.println("After conversion: "+pd.toString());
		HPCProfileApplicationType pa=pd.getHPCProfileApplication();
		assertEquals("10",pa.getArgumentArray(0).getStringValue());
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
	public void testExtractUserName()throws Exception{
		InputStream is = getResource("src/test/resources/jsdl/ex_posix.jsdl");
		assertNotNull(is);
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.parse(is);
		String user=JSDLUtils.extractUserName(jdd);
		assertNotNull(user);
		assertEquals("nobody", user);
	}

	@Test
	public void testExtractGroupName()throws Exception{
		InputStream is = getResource("src/test/resources/jsdl/ex_posix.jsdl");
		assertNotNull(is);
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.parse(is);
		String group=JSDLUtils.extractUserGroup(jdd);
		assertNotNull(group);
		assertEquals("agroup", group);
	}

	@Test
	public void testExtractContent()throws Exception{
		InputStream is = getResource("src/test/resources/jsdl/ex_posix.jsdl");
		assertNotNull(is);
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.parse(is);
		XmlObject o=JSDLUtils.getElement(jdd, POSIXApplicationDocument.type.getDocumentElementName());
		POSIXApplicationDocument p=(POSIXApplicationDocument)o;
		assertNotNull(p);
		assertEquals("/bin/date", p.getPOSIXApplication().getExecutable().getStringValue());
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
	
	@Test
	public void testHasSweep() throws Exception{
		JobDefinitionDocument d=JobDefinitionDocument.Factory.parse(
				new File("src/test/resources/jsdl/sweep-example1.xml"));
		assertTrue(JSDLUtils.hasSweep(d));
		
		JobDefinitionDocument d2=JobDefinitionDocument.Factory.parse(
				new File("src/test/resources/jsdl/staging_1.jsdl"));
		assertFalse(JSDLUtils.hasSweep(d2));
	}
	@Test
	public void testHPCPWithValidUserAndPasswd() throws Exception {
		InputStream is = new FileInputStream("src/test/resources/jsdl/jsdl_sample_with_hpc_ext.jsdl");
		assertNotNull(is);
		XmlObject jsdl = XmlObject.Factory.parse(is);
		UsernamePassword creds = JSDLUtils.extractUsernamePassword(jsdl);
		assertNotNull(creds);
		String userName = creds.getUser();
		String passwd = creds.getPassword();
		assertEquals("sc07demo",userName);
		assertEquals("hpcpsc07",passwd);
	}

	@Test
	public void testCredentialsEmptyUserAndPasswd()throws Exception{
		InputStream is = new FileInputStream("src/test/resources/jsdl/jsdl_sample_with_hpc_ext_nouser.jsdl");
		assertNotNull(is);
		XmlObject jsdl = XmlObject.Factory.parse(is);
		UsernamePassword creds = JSDLUtils.extractUsernamePassword(jsdl);
		assertNotNull(creds);
		String userName = creds.getUser();
		String passwd = creds.getPassword();
		assertEquals("",userName);
		assertEquals("",passwd);
	}

	@Test
	public void testNoCredentials()throws Exception{
		InputStream is = new FileInputStream("src/test/resources/jsdl/date2.jsdl");
		assertNotNull(is);
		XmlObject jsdl = XmlObject.Factory.parse(is);
		UsernamePassword creds = JSDLUtils.extractUsernamePassword(jsdl);
		assertNull(creds);
	}
	
	@Test
	public void testCredentialsExtract()throws Exception{
		InputStream is = new FileInputStream("src/test/resources/jsdl/staging_credentials.jsdl");
		assertNotNull(is);
		JobDefinitionDocument jsdl = JobDefinitionDocument.Factory.parse(is);
		DataStagingType dst1 = jsdl.getJobDefinition().getJobDescription().getDataStagingArray(0);
		UsernamePassword creds = JSDLUtils.extractUsernamePassword(dst1);
		assertNotNull(creds);
		assertEquals("user", creds.getUser());
		assertEquals("pass", creds.getPassword());
		DataStagingType dst2 = jsdl.getJobDefinition().getJobDescription().getDataStagingArray(1);
		OAuthToken token = JSDLUtils.extractOAuthToken(dst2);
		assertNotNull(token);
		assertEquals("123", token.getToken());
	}

}
