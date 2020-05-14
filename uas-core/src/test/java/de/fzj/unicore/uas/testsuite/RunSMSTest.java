package de.fzj.unicore.uas.testsuite;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;

import junit.framework.Assert;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.ApplicationType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.CreationFlagEnumeration;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.DataStagingType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.FileSystemType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.junit.Test;
import org.unigrids.services.atomic.types.GridFileType;
import org.unigrids.services.atomic.types.StatusType;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.client.JobClient;
import de.fzj.unicore.uas.client.StorageClient;
import de.fzj.unicore.wsrflite.ContainerProperties;
import eu.unicore.bugsreporter.annotation.FunctionalTest;
import eu.unicore.bugsreporter.annotation.RegressionTest;

/**
 * runs sms tests on a newly created USspace
 * @author schuller
 */
public class RunSMSTest extends AbstractJobRun{

	JobDefinitionDocument jobDescription;

	@Test
	@Override
	public void testRunJob()throws Exception{
		jobDescription=getJob1();
		super.testRunJob();
	}

	
	@FunctionalTest(id="RunSMSTest", description="Tests the storage management service")
	@Test
	public void testDefaultSMS()throws Exception{
		url=kernel.getContainerProperties().getValue(ContainerProperties.WSRF_BASEURL);
		EndpointReferenceType epr=EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue(url+"/StorageManagement?res=WORK");
		StorageClient sms=new StorageClient(epr,kernel.getClientConfiguration());
		FileSystemType fst = sms.getFileSystem();
		assertNotNull(fst);
		GridFileType fs=sms.listProperties("/");
		assertTrue("/".equals(fs.getPath()));
		System.out.println(fs);
		System.out.println(sms.getResourcePropertiesDocument().getStorageProperties());
		assertTrue(sms.getAvailableSpace()>-1);
	}

	@Test
	public void testFind()throws Exception{
		initClients();
		jobDescription=getJob1();
		JobClient job=submitJob(tss);
		job.start();
		job.waitUntilDone(30000);
		StorageClient sms=job.getUspaceClient();
		GridFileType[] found=sms.find("/",false,"*out",false,null,null);
		assertTrue(found.length==1);
		assertTrue(found[0].getPath().endsWith("out"));
		for(GridFileType gf: found){
			System.out.println(gf);
		}

		found=sms.find("/",false,"std...",true,null,null);
		assertTrue(found.length==2);
		assertTrue(found[0].getPath().startsWith("/std"));
		for(GridFileType gf: found){
			System.out.println(gf);
		}
	}

	@Test
	public void testMultiple()throws Exception{
		initClients();
		jobDescription=getJob1();
		JobClient job=submitJob(tss);
		StorageClient sms=job.getUspaceClient();

		//stage in a file and test listdirectory
		String in = "this is a test";
		stageIn(job,"foo", in.getBytes());
		GridFileType[] dir=sms.listDirectory(".");
		assertTrue(dir.length==1);
		String name=dir[0].getPath();
		System.out.println(name);
		String out=new String(stageOut(job,name));
		assertTrue(out.equals(in));

		//copy
		sms.copy("foo", "spam");
		//and list dir again
		dir=sms.listDirectory(".");
		assertTrue(dir.length==2);
		name=dir[0].getPath();
		//and test if it is really a copy
		String copy=dir[1].getPath();
		out=new String(stageOut(job,copy));
		assertTrue(out.equals(in));
		//get its properties
		GridFileType gf=sms.listProperties("spam");
		assertTrue(gf.getPath().endsWith("spam"));
		//rename it
		sms.rename("spam","ham");
		//delete it
		sms.delete("ham");
		//and list dir again to make sure it's gone
		dir=sms.listDirectory(".");
		assertTrue(dir.length==1);

		//test create directory
		sms.createDirectory("bar");
		dir=sms.listDirectory(".");
		assertTrue(dir.length==2);
		for(GridFileType f: dir){
			if(f.getPath().equals("./bar")){
				assertTrue(f.getIsDirectory());
			}
		}

		//stage in stuff to new directory
		in = "this is a second test";
		stageIn(job,"./bar/foo2", in.getBytes());
		dir=sms.listDirectory("./bar");
		assertTrue(dir.length==1);
		name=dir[0].getPath();
		out=new String(stageOut(job,name));
		assertTrue(out.equals(in));

		//import file with non-existent directory
		//this will create the directory
		in = "this is a third test";
		String target="./newdir/foo3";
		stageIn(job,target, in.getBytes());
		out=new String(stageOut(job,target));
		assertTrue(out.equals(in));

		in = "this is a fourth test";
		target="./newdir2/subdir/foo4";
		stageIn(job,target, in.getBytes());
		out=new String(stageOut(job,target));
		assertTrue(out.equals(in));

		in = "this is a fifth test";
		target="./newdir3/subdir2/subdir/foo4";
		stageIn(job,target, in.getBytes());
		out=new String(stageOut(job,target));
		assertTrue(out.equals(in));

		in = "this is a sixth test";
		target="./foo6";
		sms.upload(target).write(in.getBytes());
		out=new String(stageOut(job,target));
		assertTrue(out.equals(in));
		
		in = "this is a seventh test";
		target="./foo7";
		int len = 5;
		ByteArrayInputStream bis=new ByteArrayInputStream(in.getBytes());
		sms.upload(target).writeAllData(bis,len);
		out=new String(stageOut(job,target));
		assertTrue(out.equals(in.substring(0, len)));
		
		// multi-delete
		sms.delete(Arrays.asList("foo6","foo7"));
		try{
			sms.listProperties("foo6");
			Assert.fail("File should no longer exist!");
		}catch(Exception ex){}
		try{
			sms.listProperties("foo7");
			Assert.fail("File should no longer exist!");
		}catch(Exception ex){}
	}


	@Test
	public void testExportAppend()throws Exception {
		initClients();
		jobDescription=getJob1();
		JobClient job=runJob(tss);
		StorageClient sms=job.getUspaceClient();
		System.out.println(sms.getResourcePropertyDocument());
		ByteArrayOutputStream bos=new ByteArrayOutputStream();
		sms.download("stdout").readAllData(bos);
		int originalLength=bos.toString().length();
		EndpointReferenceType storageEPR=job.getResourcePropertiesDocument().getJobProperties().getWorkingDirectoryReference();
		jobDescription=getJobAppend(storageEPR,true);

		//now run second job, which will stage out data to the uspace of the first job
		runJob(tss);
		//check that stdout of 1st job has been appended to
		bos=new ByteArrayOutputStream();
		sms.download("stdout").readAllData(bos);
		assertTrue(bos.toString().length()>=originalLength*2);
	}

	@Test
	public void testExportDontOverwrite()throws Exception{
		initClients();
		jobDescription=getJob1();
		runJob(tss);
		JobClient job=runJob(tss);
		StorageClient sms=job.getUspaceClient();
		ByteArrayOutputStream bos=new ByteArrayOutputStream();
		sms.download("stdout").readAllData(bos);
		//int originalLength=bos.toString().length();
		EndpointReferenceType storageEPR=job.getResourcePropertiesDocument().getJobProperties().getWorkingDirectoryReference();
		jobDescription=getJobDontOverwrite(storageEPR);
		//now run second job, which will stage out data to the uspace of the first job
		try{
			job = runJob(tss);
		}catch(Exception ignored){}
		//job should have failed
		System.out.println(job.getResourcePropertiesDocument().getJobProperties().getLog());
		assertTrue(StatusType.FAILED.equals(job.getStatus()));
	}

	@Test
	public void testFileProperties()throws Exception{
		initClients();
		jobDescription=getJob1();
		JobClient job=submitJob(tss);
		StorageClient sms=job.getUspaceClient();

		//stage in a file and test listdirectory
		String in = "this is a test";
		stageIn(job,"foo", in.getBytes());
		GridFileType file=sms.listProperties("foo");
		assertTrue(file!=null);
		//check that path is relative to SMS root
		assertTrue("/foo".equals(file.getPath()));
		try{
			file=sms.listProperties("fooxx");
			System.out.println(file);
			assertTrue(file==null);
		}
		catch(FileNotFoundException expected){
			/* OK */
		}
	}

	@Test
	@RegressionTest(
			description="Tests that invalid import paths are not accepted", 
			url="https://sourceforge.net/tracker/?func=detail&aid=3297977&group_id=102081&atid=633902")
	public void testImportWithInvalidPath()throws Exception{
		initClients();
		jobDescription=getJob1();
		JobClient job=submitJob(tss);
		StorageClient sms=job.getUspaceClient();
		sms.createDirectory("test");
		//now try to import to "test"
		try{
			sms.upload("test");
			Assert.fail("import to directory should not work");
		}catch(Exception ex){
			//OK
		}
	}
	
	@Test
	public void testLS()throws Exception{
		initClients();
		StorageClient sms2=tss.getStorage("TEMP");
		assertTrue("TEMP".equals(sms2.getStorageName()));
		GridFileType[] gf1=sms2.listDirectory("/");
		System.out.println(Arrays.asList(gf1));
	}

	
	@Override
	protected JobDefinitionDocument getJob() {
		return jobDescription;
	}

	private JobDefinitionDocument getJob1() {
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.newInstance();
		ApplicationType app=jdd.addNewJobDefinition().addNewJobDescription().addNewApplication();
		app.setApplicationName("Date");
		return jdd;
	}

	//second job (stages data out to first job in APPEND mode)
	private JobDefinitionDocument getJobAppend(EndpointReferenceType uspace1EPR, boolean append) {
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.newInstance();
		ApplicationType app=jdd.addNewJobDefinition().addNewJobDescription().addNewApplication();
		app.setApplicationName("Date");
		DataStagingType dst=jdd.getJobDefinition().getJobDescription().addNewDataStaging();
		String uri="BFT:"+uspace1EPR.getAddress().getStringValue()+"#/stdout";
		dst.addNewTarget().setURI(uri);
		dst.setFileName("stdout");
		if(append){
			dst.setCreationFlag(CreationFlagEnumeration.APPEND);
		}
		return jdd;
	}

	//second job (stages data out to first job but with DONT_OVERWRITE flag set)
	private JobDefinitionDocument getJobDontOverwrite(EndpointReferenceType uspace1EPR) {
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.newInstance();
		ApplicationType app=jdd.addNewJobDefinition().addNewJobDescription().addNewApplication();
		app.setApplicationName("Date");
		DataStagingType dst=jdd.getJobDefinition().getJobDescription().addNewDataStaging();
		String uri="BFT:"+uspace1EPR.getAddress().getStringValue()+"#/stdout";
		dst.addNewTarget().setURI(uri);
		dst.setFileName("stdout");
		dst.setCreationFlag(CreationFlagEnumeration.DONT_OVERWRITE);
		return jdd;
	}



}
