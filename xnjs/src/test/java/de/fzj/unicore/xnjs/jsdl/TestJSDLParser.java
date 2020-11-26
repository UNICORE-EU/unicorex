package de.fzj.unicore.xnjs.jsdl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.ApplicationType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.CreationFlagEnumeration;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.DataStagingDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.DataStagingType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.IndividualCPUTimeDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDescriptionType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.RangeType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.RangeValueType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ResourcesDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ResourcesType;
import org.junit.Test;

import de.fzj.unicore.xnjs.beans.idb.AllowedType;
import de.fzj.unicore.xnjs.beans.idb.ResourceDocument;
import de.fzj.unicore.xnjs.idb.ApplicationInfo;
import de.fzj.unicore.xnjs.io.DataStageInInfo;
import de.fzj.unicore.xnjs.io.DataStageOutInfo;
import de.fzj.unicore.xnjs.io.IFileTransfer.OverwritePolicy;
import de.fzj.unicore.xnjs.io.impl.OAuthToken;
import de.fzj.unicore.xnjs.io.impl.UsernamePassword;
import de.fzj.unicore.xnjs.resources.IntResource;
import de.fzj.unicore.xnjs.resources.ReservationResource;
import de.fzj.unicore.xnjs.resources.Resource;
import de.fzj.unicore.xnjs.resources.Resource.Category;
import de.fzj.unicore.xnjs.resources.ResourceRequest;
import de.fzj.unicore.xnjs.resources.ResourceSet;
import de.fzj.unicore.xnjs.util.XmlBeansUtils;
import eu.unicore.jsdl.extensions.IgnoreFailureDocument;
import eu.unicore.jsdl.extensions.ResourceRequestDocument;
import eu.unicore.jsdl.extensions.ResourceRequestType;

public class TestJSDLParser {

	String[] testJSDLs=new String[]{
			"src/test/resources/jsdl/posix.jsdl",
	};

	@Test
	public void testPosix()throws Exception{
		for(String file: testJSDLs){
			JobDefinitionDocument jd=JobDefinitionDocument.Factory.parse(new File(file));
			doTest(jd);
		}
	}

	private void doTest(JobDefinitionDocument jd){
		JSDLParser p=new JSDLParser();
		ApplicationType app=jd.getJobDefinition().getJobDescription().getApplication();
		assertEquals("/bin/date",p.getExecutable(app));
		assertEquals("stdin",p.getInput(app));
		assertEquals("stdout",p.getOutput(app));
		assertEquals("stderr",p.getError(app));
	}

	@Test
	public void testAppInfo()throws Exception{
		JSDLParser p=new JSDLParser();
		for(String file: testJSDLs){
			JobDefinitionDocument jd=JobDefinitionDocument.Factory.parse(new File(file));
			ApplicationInfo ai=p.parseApplicationInfo(jd);
			assertEquals("/bin/date",ai.getExecutable());
			assertEquals("test_value",ai.getEnvironment().get("test"));
			assertEquals("bar",ai.getEnvironment().get("foo"));
		}
	}

	@Test
	public void testAppInfoPrePostCommand()throws Exception{
		JSDLParser p=new JSDLParser();
		String file="src/test/resources/jsdl/prepost.jsdl";
		JobDefinitionDocument jd=JobDefinitionDocument.Factory.parse(new File(file));
		ApplicationInfo ai=p.parseApplicationInfo(jd);
		assertEquals("Date",ai.getName());
		assertEquals("/bin/echo 'pre'",ai.getUserPreCommand());
		assertEquals("/bin/echo 'post'",ai.getUserPostCommand());
		assertTrue(ai.isUserPreCommandOnLoginNode());
		assertFalse(ai.isUserPostCommandOnLoginNode());
		
	}

	@Test
	public void testParseStaging()throws Exception{
		JobDefinitionDocument jd=JobDefinitionDocument.Factory.newInstance();
		JobDescriptionType job=jd.addNewJobDefinition().addNewJobDescription();
		DataStagingType dst1=DataStagingType.Factory.newInstance();
		dst1.setFileName("file1");
		dst1.setFilesystemName("SCRATCH");
		dst1.addNewSource().setURI("http://file1");
		dst1.setCreationFlag(CreationFlagEnumeration.APPEND);
		dst1.setDeleteOnTermination(true);

		DataStagingType dst2=DataStagingType.Factory.newInstance();
		dst2.setFileName("file2");
		dst2.addNewSource().setURI("http://file2");
		dst2.setCreationFlag(CreationFlagEnumeration.OVERWRITE);

		DataStagingDocument dsd3=DataStagingDocument.Factory.newInstance();
		DataStagingType dst3=dsd3.addNewDataStaging();
		dst3.setFileName("file3");
		dst3.setFilesystemName("SCRATCH");
		dst3.addNewTarget().setURI("http://file3");
		dst3.setCreationFlag(CreationFlagEnumeration.DONT_OVERWRITE);
		IgnoreFailureDocument f=IgnoreFailureDocument.Factory.newInstance();
		f.setIgnoreFailure(true);
		XmlBeansUtils.append(f, dsd3);

		job.setDataStagingArray(new DataStagingType[]{dst1, dst2, dsd3.getDataStaging()});
		System.out.println(job);


		List<DataStageInInfo>dsi=new JSDLParser().parseImports(jd);
		assertNotNull(dsi);
		assertEquals(2,dsi.size());
		DataStageInInfo dsi1=dsi.get(0);
		assertEquals("file1",dsi1.getFileName());
		assertEquals("SCRATCH", dsi1.getFileSystemName());
		assertEquals(1,dsi1.getSources().length);
		assertEquals("http://file1",dsi1.getSources()[0].toString());
		assertEquals(OverwritePolicy.APPEND,dsi1.getOverwritePolicy());
		assertFalse(dsi1.isIgnoreFailure());
		assertNull(dsi1.getCredentials());

		DataStageInInfo dsi2=dsi.get(1);
		assertEquals("file2",dsi2.getFileName());
		assertEquals(1,dsi2.getSources().length);
		assertEquals("http://file2",dsi2.getSources()[0].toString());
		assertEquals(OverwritePolicy.OVERWRITE,dsi2.getOverwritePolicy());
		assertFalse(dsi2.isIgnoreFailure());
		assertNull(dsi2.getCredentials());

		List<DataStageOutInfo>dso=new JSDLParser().parseExports(jd);
		assertNotNull(dso);
		assertEquals(1,dso.size());

		DataStageOutInfo dso1=dso.get(0);
		assertEquals("file3",dso1.getFileName());
		assertEquals("SCRATCH",dso1.getFileSystemName());
		assertEquals("http://file3",dso1.getTarget().toString());
		assertEquals(OverwritePolicy.DONT_OVERWRITE,dso1.getOverwritePolicy());
		assertTrue(dso1.isIgnoreFailure());
		assertNull(dso1.getCredentials());

	}

	@Test
	public void testParseStagingCredentials()throws Exception{
		JobDefinitionDocument jd=JobDefinitionDocument.Factory.parse(new File("src/test/resources/jsdl/staging_credentials.jsdl"));

		List<DataStageInInfo>dsi=new JSDLParser().parseImports(jd);
		assertNotNull(dsi);
		assertEquals(3, dsi.size());
		DataStageInInfo dsi1 = dsi.get(0);
		UsernamePassword creds1=(UsernamePassword)dsi1.getCredentials();
		assertEquals("user", creds1.getUser());
		assertEquals("pass", creds1.getPassword());

		DataStageInInfo dsi2 = dsi.get(1);
		OAuthToken creds2=(OAuthToken)dsi2.getCredentials();
		assertEquals("123", creds2.getToken());
		
		DataStageInInfo dsi3 = dsi.get(2);
		OAuthToken creds3=(OAuthToken)dsi3.getCredentials();
		assertTrue("Token is '"+creds3.getToken()+"'", creds3.getToken().isEmpty());
	}
	
	@Test
	public void testInlineStaging()throws Exception{
		JobDefinitionDocument jd=JobDefinitionDocument.Factory.parse(new File("src/test/resources/jsdl/inline_data_staging.jsdl"));
		List<DataStageInInfo>dsi=new JSDLParser().parseImports(jd);
		assertNotNull(dsi);
		assertEquals(1,dsi.size());
		DataStageInInfo dsi1=dsi.get(0);
		assertEquals("input.txt",dsi1.getFileName());
		assertEquals(1,dsi1.getSources().length);
		assertEquals("inline://foo",dsi1.getSources()[0].toString());
		assertEquals(OverwritePolicy.OVERWRITE,dsi1.getOverwritePolicy());
		String data = dsi1.getInlineData();
		assertTrue(data.startsWith("#!/bin/sh"));
	}
	
	@Test
	public void testCpus(){
		ResourcesType rt=ResourcesType.Factory.newInstance();
		rt.addNewIndividualCPUCount().addNewExact().setDoubleValue(1024);
		JSDLResourceSet jRs=new JSDLResourceSet(rt);
		Resource cpus = jRs.getResource(JSDLResourceSet.CPUS_PER_NODE);
		assertEquals(Long.valueOf(1024),cpus.getValue());
	}

	@Test
	public void testNodes(){
		ResourcesType rt=ResourcesType.Factory.newInstance();
		rt.addNewTotalResourceCount().addNewExact().setDoubleValue(1024);
		JSDLResourceSet jRs=new JSDLResourceSet(rt);
		Resource nodes = jRs.getResource(JSDLResourceSet.NODES);
		assertEquals(Long.valueOf(1024),nodes.getValue());
	}

	@Test
	public void testMemory(){
		ResourcesType rt=ResourcesType.Factory.newInstance();
		rt.addNewIndividualPhysicalMemory().addNewExact().setDoubleValue(1024);
		JSDLResourceSet jRs=new JSDLResourceSet(rt);
		Resource mem = jRs.getResource(JSDLResourceSet.MEMORY_PER_NODE);
		assertEquals(Long.valueOf(1024),mem.getValue());
	}

	@Test
	public void testTotalCPU(){
		ResourcesType rt=ResourcesType.Factory.newInstance();
		rt.addNewTotalCPUCount().addNewExact().setDoubleValue(1024);
		JSDLResourceSet jRs=new JSDLResourceSet(rt);
		Resource cpus = jRs.getResource(JSDLResourceSet.TOTAL_CPUS);
		assertEquals(Long.valueOf(1024),cpus.getValue());
	}

	@Test
	public void testSiteSpecificResource(){
		ResourcesDocument rDoc=ResourcesDocument.Factory.newInstance();
		ResourcesType rt=rDoc.addNewResources();
		rt.addNewIndividualPhysicalMemory().addNewExact().setDoubleValue(1024);
		ResourceRequestDocument ssrD=ResourceRequestDocument.Factory.newInstance();
		ResourceRequestType ssr=ssrD.addNewResourceRequest();
		ssr.setName("Tasks");
		ssr.setValue("100");
		XmlBeansUtils.append(ssrD, rDoc);
		ResourceDocument resDoc=ResourceDocument.Factory.newInstance();
		resDoc.addNewResource().setDefault("1024");
		resDoc.getResource().setMax("4096");
		resDoc.getResource().setMin("1024");
		resDoc.getResource().setName("Quibbles");
		resDoc.getResource().setType(AllowedType.INT);
		XmlBeansUtils.append(resDoc, rDoc);

		JSDLResourceSet jRs=new JSDLResourceSet(rt);
		Resource mem = jRs.getResource(JSDLResourceSet.MEMORY_PER_NODE);
		assertEquals(Long.valueOf(1024),mem.getValue());
		ResourceRequest tasks=ResourceRequest.find(jRs.getExtensionJSDLResources(),"Tasks");
		assertEquals("100",tasks.getRequestedValue());	

		IntResource quibbles=(IntResource)jRs.getResource("Quibbles");
		assertNotNull(quibbles);
		assertEquals(Long.valueOf(1024),(Long)quibbles.getLower());
		assertEquals(Long.valueOf(4096),(Long)quibbles.getUpper());
		assertEquals(Long.valueOf(1024),(Long)quibbles.getValue());

	}

	@Test
	public void testValidateSiteResourceDefaults(){
		ResourcesDocument rDoc=ResourcesDocument.Factory.newInstance();
		ResourcesType rt=rDoc.addNewResources();
		rt.addNewIndividualPhysicalMemory().addNewExact().setDoubleValue(1024);
		RangeType range=rt.getIndividualPhysicalMemory().addNewRange();
		range.addNewLowerBound().setDoubleValue(2048);
		range.addNewUpperBound().setDoubleValue(4096);


		JSDLResourceSet jRs=new JSDLResourceSet(rt);
		IntResource foo=new IntResource("Foo", 10l, 20l, 11l, Category.OTHER);
		jRs.putResource(foo);

		List<String>errors=jRs.validateDefaults();
		if(errors.size()>0)System.out.println("Validation errors: "+errors);

		assertEquals(2, errors.size());

		Resource mem = jRs.getResource(JSDLResourceSet.MEMORY_PER_NODE);
		assertEquals(Long.valueOf(2048),mem.getValue());

		IntResource x=(IntResource)jRs.getResource("Foo");
		assertEquals(Long.valueOf(11),x.getValue());

	}
	
	@Test
	public void testQueueRequest(){
		ResourcesDocument rDoc=ResourcesDocument.Factory.newInstance();
		ResourcesType rt=rDoc.addNewResources();
		rt.addNewIndividualPhysicalMemory().addNewExact().setDoubleValue(1024);
		ResourceRequestDocument ssrD=ResourceRequestDocument.Factory.newInstance();
		ssrD.addNewResourceRequest().setName("Queue");
		ssrD.getResourceRequest().setValue("fast");
		XmlBeansUtils.append(ssrD, rDoc);

		JSDLResourceSet jRs=new JSDLResourceSet(rt);
		List<ResourceRequest>rr=jRs.getExtensionJSDLResources();

		assertEquals(1,rr.size());
		ResourceRequest req=rr.get(0);
		assertEquals("Queue",req.getName());
		assertEquals("fast",req.getRequestedValue());
	}

	@Test
	public void testRenderResourceSet()throws Exception{
		ResourcesDocument rDoc=ResourcesDocument.Factory.newInstance();
		ResourcesType rt=rDoc.addNewResources();
		rt.addNewIndividualPhysicalMemory().addNewExact().setDoubleValue(1024);
		String expr = "${1+1}";
		JSDLUtils.setExpression(rt.addNewIndividualCPUCount(), expr);
		JSDLResourceSet jRs=new JSDLResourceSet(rt);
		jRs.putResource(new ReservationResource("1234"));

		ResourcesDocument doc=new JSDLRenderer().render(jRs);
		double mem=doc.getResources().getIndividualPhysicalMemory().getExactArray(0).getDoubleValue();
		assertEquals(1024d,mem,1);
		RangeValueType cpus = doc.getResources().getIndividualCPUCount();
		assertEquals(expr, JSDLUtils.getExpression(cpus));

		JobDefinitionDocument j = JobDefinitionDocument.Factory.newInstance();
		j.addNewJobDefinition().addNewJobDescription().setResources(doc.getResources());
		System.out.println(j);
	}
	
	@Test
	public void testRenderResourceSet2()throws Exception{
		ResourceSet jRs=new ResourceSet();
		jRs.putResource(new IntResource(ResourceSet.NODES, 4l, 32l, 1l, Category.PROCESSING));
		jRs.putResource(new IntResource(ResourceSet.TOTAL_CPUS, 8l, 64l, 2l, Category.PROCESSING));
		jRs.putResource(new IntResource(ResourceSet.CPUS_PER_NODE, null, 2l, 1l, Category.PROCESSING));
		
		ResourcesDocument doc=new JSDLRenderer().render(jRs);
		double nodes = doc.getResources().getTotalResourceCount().getExactArray(0).getDoubleValue();
		assertEquals(4d,nodes,0.1);
		double cpus = doc.getResources().getTotalCPUCount().getExactArray(0).getDoubleValue();
		assertEquals(8d,cpus,0.1);
		
		System.out.println(doc);
	}

	@Test
	public void testResourceNameCheck(){
		assertTrue(JSDLResourceSet.isJSDLResourceName(JSDLResourceSet.NODES));
		assertFalse(JSDLResourceSet.isJSDLResourceName("Queue"));
	}

	@Test
	public void testResourceExpressions(){
		String expr = "${123}";
		IndividualCPUTimeDocument test = IndividualCPUTimeDocument.Factory.newInstance();
		JSDLUtils.setExpression(test.addNewIndividualCPUTime(),expr);
		assertTrue(JSDLUtils.hasExpression(test.getIndividualCPUTime()));
		assertEquals(expr,JSDLUtils.getExpression(test.getIndividualCPUTime()));
	}


	@Test
	public void testJobTypeInteractive()throws Exception{
		JSDLParser p=new JSDLParser();
		String file="src/test/resources/jsdl/interactive.jsdl";
		JobDefinitionDocument jd=JobDefinitionDocument.Factory.parse(new File(file));
		ApplicationInfo ai=p.parseApplicationInfo(jd);
		assertEquals("Date",ai.getName());
		assertTrue(ai.isRunOnLoginNode());
	}
}
