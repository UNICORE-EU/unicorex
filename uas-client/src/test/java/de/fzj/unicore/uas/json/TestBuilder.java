package de.fzj.unicore.uas.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.apache.xmlbeans.XmlObject;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ApplicationType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.DataStagingType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ResourcesType;
import org.ggf.schemas.jsdl.x2005.x11.jsdlPosix.EnvironmentType;
import org.ggf.schemas.jsdl.x2005.x11.jsdlPosix.POSIXApplicationDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdlPosix.POSIXApplicationType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import de.fzj.unicore.uas.util.LogMessageWriter;
import de.fzj.unicore.uas.util.MessageWriter;
import de.fzj.unicore.wsrflite.xmlbeans.WSUtilities;
import eu.unicore.jsdl.extensions.UserPostCommandDocument;
import eu.unicore.jsdl.extensions.UserPreCommandDocument;
import eu.unicore.util.Log;

public class TestBuilder{

	//test creating builder from a JSON string
	@Test
	public void testBuildFromJSONString() throws Exception{
		Builder b=new Builder(new File("src/test/resources/jobs/test.u"));
		assertNotNull(b.getJob());
		ApplicationType a=b.getJob().getJobDefinition().getJobDescription().getApplication();
		assertEquals(a.getApplicationName(),"foo");
		assertEquals(a.getApplicationVersion(),"1.0");

		POSIXApplicationType posix=(POSIXApplicationType)WSUtilities.extractAny(a, POSIXApplicationDocument.type.getDocumentElementName())[0];
		assertEquals("-v", posix.getArgumentArray()[0].getStringValue());
		assertEquals("-d", posix.getArgumentArray()[1].getStringValue());
		assertEquals("somebody",posix.getUserName().getStringValue());

		EnvironmentType[] env=posix.getEnvironmentArray();
		assertEquals(2, env.length);
		DataStagingType[] ds=b.getJob().getJobDefinition().getJobDescription().getDataStagingArray();
		assertEquals(2, ds.length);

		assertTrue(ds[0].toString().contains("IgnoreFailure>true")
				|| ds[1].toString().contains("IgnoreFailure>true")
				);
		
		ResourcesType res=b.getJob().getJobDefinition().getJobDescription().getResources();
		assertEquals(128000000d, res.getIndividualPhysicalMemory().getExactArray(0).getDoubleValue(),0.1d);
		assertEquals(32d, res.getIndividualCPUCount().getExactArray(0).getDoubleValue(),0.1d);
		assertEquals(3600d, res.getIndividualCPUTime().getExactArray(0).getDoubleValue(),0.1d);
		assertEquals(4d, res.getTotalResourceCount().getExactArray(0).getDoubleValue(),0.1d);
		assertTrue(res.toString().contains("64738"));

		assertEquals("DEMO-SITE",b.getProperty("Site"));
		
		String[] proj=b.getJob().getJobDefinition().getJobDescription().getJobIdentification().getJobProjectArray();
		assertNotNull(proj);
		assertEquals(1, proj.length);
		assertEquals("my_project", proj[0]);
		String[] tags=b.getJob().getJobDefinition().getJobDescription().getJobIdentification().getJobAnnotationArray();
		assertNotNull(tags);
		assertEquals(2, tags.length);
	}

	@Test
	public void testBuildFromJSONString2() throws Exception{
		Builder b=new Builder(new File("src/test/resources/jobs/test2.u"));
		assertNotNull(b.getJob());
		ApplicationType a=b.getJob().getJobDefinition().getJobDescription().getApplication();
		assertEquals(a.getApplicationName(),"foo");

		POSIXApplicationType posix=(POSIXApplicationType)WSUtilities.extractAny(a, POSIXApplicationDocument.type.getDocumentElementName())[0];
		EnvironmentType[] env=posix.getEnvironmentArray();
		assertEquals(3, env.length);
		System.out.println(posix);
		assertTrue(posix.toString().contains("bar"));
		assertTrue(posix.toString().contains("spam"));
		assertTrue(posix.toString().contains("debug"));
	}


	@Test
	public void testBuildFromJSONString3() throws Exception{
		Builder b=new Builder(new File("src/test/resources/jobs/test3.u"));
		assertNotNull(b.getJob());
		ApplicationType a=b.getJob().getJobDefinition().getJobDescription().getApplication();
		assertEquals(a.getApplicationName(),"Date");
		System.out.println(b.getJob());
		POSIXApplicationType posix=(POSIXApplicationType)WSUtilities.extractAny(a, POSIXApplicationDocument.type.getDocumentElementName())[0];
		EnvironmentType[] env=posix.getEnvironmentArray();
		assertEquals(3, env.length);
		System.out.println(posix);
		assertTrue(posix.toString().contains("JAVA_OPTS"));
		assertTrue(posix.toString().contains("SHELL"));
		assertTrue(posix.toString().contains("bar"));
	}
	

	@Test
	public void testBuildFromJSONString4() throws Exception{
		Builder b=new Builder(new File("src/test/resources/jobs/test4.u"));
		assertNotNull(b.getJob());
		ApplicationType a=b.getJob().getJobDefinition().getJobDescription().getApplication();
		assertEquals(a.getApplicationName(),"Date");
		UserPreCommandDocument pre = (UserPreCommandDocument)WSUtilities.extractAnyElements(b.getJob(), UserPreCommandDocument.type.getDocumentElementName())[0];
		assertEquals("pre", pre.getUserPreCommand().getStringValue());
		assertFalse(pre.getUserPreCommand().getRunOnLoginNode());
		UserPostCommandDocument post = (UserPostCommandDocument)WSUtilities.extractAnyElements(b.getJob(), UserPostCommandDocument.type.getDocumentElementName())[0];
		assertEquals("post", post.getUserPostCommand().getStringValue());
		assertTrue(post.getUserPostCommand().getRunOnLoginNode());
	}
	
	@Test
	public void testBuildFromJSONStringWithSweepArguments() throws Exception{
		Builder b=new Builder(new File("src/test/resources/jobs/test_sweep.u"));
		JobDefinitionDocument jdd=b.getJob();
		assertTrue(jdd.toString().contains("Assignment"));
		assertTrue(jdd.toString().contains("Match"));
		assertTrue(jdd.toString().contains("fox"));
		assertNotNull(jdd);
	}

	@Test
	public void testBuildFromJSONStringWithSweepEnvironment() throws Exception{
		Builder b=new Builder(new File("src/test/resources/jobs/test_sweep_env.u"));
		JobDefinitionDocument jdd=b.getJob();
		assertTrue(jdd.toString().contains("Assignment"));
		assertTrue(jdd.toString().contains("Match"));
		assertTrue(jdd.toString().contains("fox"));
		assertNotNull(jdd);
		System.out.println(jdd);
	}

	@Test
	public void testValidateJSDL()throws Exception{

		String jsdlSpec="<JobDefinition xmlns=\"http://schemas.ggf.org/jsdl/2005/11/jsdl\">"+
		"<JobDescription>   <Application>    <xApplicationName>foo</xApplicationName>  " +
		" </Application>  </JobDescription> </JobDefinition>";
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.parse(jsdlSpec);
		MessageWriter msg = new LogMessageWriter(Log.getLogger("test", Builder.class));
		try{
			assertFalse(Builder.isValidJSDL(jdd, msg));
		}catch(Exception ex){
			ex.printStackTrace();
			fail(ex.getMessage());
		}
		jsdlSpec="<JobDefinition xmlns=\"http://schemas.ggf.org/jsdl/2005/11/jsdl\">"+
		"<JobDescription>   <Application>    <ApplicationName>foo</ApplicationName>  " +
		" </Application>  </JobDescription> </JobDefinition>";

		jdd=JobDefinitionDocument.Factory.parse(jsdlSpec);

		try{
			assertTrue(Builder.isValidJSDL(jdd, msg));
		}catch(Exception ex){
			ex.printStackTrace();
			fail(ex.getMessage());
		}

	}

	@Test
	public void testCredential()throws Exception{
		JSONObject cred=new JSONObject();
		cred.put("Username", "foo");
		cred.put("Password", "bar");
		XmlObject co=new Builder().makeCredentials(cred);
		String c=co.toString();
		assertTrue(c.contains("<wsse:Username>foo</wsse:Username>"));
		assertTrue(c.contains("<wsse:Password>bar</wsse:Password>"));
	}
	
	@Test
	public void testCredentialFromJSON()throws Exception{
		String job="{ ApplicationName: date, \n" +
				"Imports: [ \n" +
				"{From: \"scp://test\", To: in, \n" +
				"Credentials: { Username: foo, Password: bar }, " +
				"\n}" +
				"]," +
				"Exports: [ \n" +
				"{To: \"scp://test1\", From: stdout, \n" +
				"Credentials: { Username: ham, Password: spam}, " +
				"\n}" +
				"]," +
				"}";
		Builder b=new Builder(job);
		String jsdl=b.getJob().toString();
		assertTrue(jsdl.contains("<wsse:Username>foo</wsse:Username>"));
		assertTrue(jsdl.contains("<wsse:Password>bar</wsse:Password>"));
		assertTrue(jsdl.contains("<wsse:Username>ham</wsse:Username>"));
		assertTrue(jsdl.contains("<wsse:Password>spam</wsse:Password>"));
	}
	
	@Test
	public void testBearerCredentialFromJSON()throws Exception{
		String job="{ ApplicationName: date, \n" +
				"Imports: [ \n" +
				"{From: \"scp://test\", To: in, \n" +
				"Credentials: { BearerToken: some_token }, " +
				"\n}" +
				"]," +
				"}";
		Builder b=new Builder(job);
		String jsdl=b.getJob().toString();
		assertTrue(jsdl.contains("<unic:BearerToken "
				+ "xmlns:unic=\"http://www.unicore.eu/unicore/jsdl-extensions\">some_token"
				+ "</unic:BearerToken>"));
	}
	
	@Test
	public void testInlineDataStageIn()throws Exception{
		String job="{ ApplicationName: date, \n" +
				"Imports: [ \n" +
				"{From: \"inline://test\", To: in, \n" +
				"Data: \"inline data test123\", " +
				"\n}" +
				"]," +
				"}";
		Builder b=new Builder(job);
		String jsdl=b.getJob().toString();
		System.out.println(jsdl);
		assertTrue(jsdl.contains("inline data test123"));
	}
	
	@Test
	public void testOS()throws Exception{
		String[] os=new String[]{"linux", "MacOS", "Aix", "WinNT", "UNKnoWn", 
				"freeBSD", "hpux", "netbsd", "irix", "solARIS"};
		Builder Bob=new Builder();//obvious really
		for(String o: os){
			assertNotNull("OS "+o+" not understood", Bob.getOSType(o));
		}
	}
	
	@Test
	public void testComments() throws Exception{
		String foo = "{\n" +
				"#test\n" +
				"  # another test\n" +
				" Executable: \"/bin/date,\"}";
		Builder bob = new Builder(foo);
		assertNotNull(bob);
	}
	
	@Test
	public void testResourceExpressions() throws Exception{
		String foo = "{\nResources: { Memory: \"${1024*1024}\"\n}}";
		Builder bob = new Builder(foo);
		assertNotNull(bob);
		ResourcesType rt = bob.getJob().getJobDefinition().getJobDescription().getResources();
		assertTrue(rt.toString().contains("expression=\"${1024*1024}\""));
	}
	

	@Test
	public void testConvertRESTURLs(){
		String[] rest = {
				"https://somehttp/files/a/b/c.txt",
				"https://foo:8080/TEST/rest/core/storages/default_storage/files/a/b/c.txt",
				"https://foo:8080/TEST/rest/core/storages/default_storage/files/foo/files/c.txt",
				"http://nosecurity/rest/core/storages/default_storage/files/a/b/c.txt",
				};
		String[] wsrf = {
				"https://somehttp/files/a/b/c.txt",
				"BFT:https://foo:8080/TEST/services/StorageManagement?res=default_storage#/a/b/c.txt",
				"BFT:https://foo:8080/TEST/services/StorageManagement?res=default_storage#/foo/files/c.txt",
				"BFT:http://nosecurity/services/StorageManagement?res=default_storage#/a/b/c.txt",
				};
		for(int i=0; i<rest.length; i++){
			assertEquals("REST-to-WSRF URL conversion", wsrf[i], Builder.convertRESTToWSRF(rest[i]));
		}
	}

	@Test
	public void testFoo() throws Exception {
		String foo = "{ Environment: { Memory: \"${1024*1024}\"\n}}";
		JSONObject j = new JSONObject(foo);
		JSONArray a = j.optJSONArray("Environment");
		assertNull(a);
		JSONObject o = j.optJSONObject("Environment");
		assertNotNull(o);
	}
	
}
