package de.fzj.unicore.uas.xtreemfs;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.DataStagingType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.unigrids.services.atomic.types.ProtocolType;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.client.StorageClient;
import de.fzj.unicore.uas.xnjs.XNJSFacade;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.ems.InternalManager;
import de.fzj.unicore.xnjs.ems.Manager;
import eu.unicore.security.Client;
import eu.unicore.security.SecurityTokens;

public class TestXtreemFSFileTransferFromRemote {

	private static XNJS config;
	
	private static Kernel kernel;
	
	@BeforeClass
	public static void setupXNJS()throws Exception{
		File tmp=new File("target/data");
		FileUtils.deleteQuietly(tmp);
		tmp.mkdirs();
		UAS uas=new UAS("src/test/resources/uas.config");
		uas.startSynchronous();
		kernel=uas.getKernel();
		config=XNJSFacade.get(null,kernel).getXNJS();
	}

	@AfterClass
	public static void shutDown()throws Exception{
		kernel.shutdown();
		XNJSFacade.get(null, kernel).shutdown();
		File tmp=new File("target/data");
		FileUtils.deleteQuietly(tmp);
	}
	
	@Test
	public void testStageOut()throws Exception{
		XtreemProperties cfg = kernel.getAttribute(XtreemProperties.class);
		cfg.setProperty(XtreemProperties.XTREEMFS_LOCAL_MOUNT, null);
		String url="http://localhost:65321/services/StorageManagement?res=WORK";
		cfg.setProperty(XtreemProperties.XTREEMFS_REMOTE_URL, url);
		
		JobDefinitionDocument job=JobDefinitionDocument.Factory.newInstance();
		job.addNewJobDefinition().addNewJobDescription().addNewApplication().setApplicationName("Date");
		DataStagingType dst=job.getJobDefinition().getJobDescription().addNewDataStaging();
		dst.setFileName("stdout");
		String path="test123";
		dst.addNewTarget().setURI("xtreemfs://"+path);
		Action a=config.makeAction(job);
		a.getProcessingContext().put(Action.AUTO_SUBMIT,Boolean.TRUE);
		Client client=makeClient();
		config.get(Manager.class).add(a, client);
		waitForFinished(a.getUUID(), client);
		a=config.get(InternalManager.class).getAction(a.getUUID());
		a.printLogTrace();
		assertTrue(a.getResult().isSuccessful());
		//check that file was exported
		File exported=new File("target/unicorex-test",path);
		assertTrue(exported.exists());
	}
	
	@Test
	public void testStageOutDir()throws Exception{
		XtreemProperties cfg = kernel.getAttribute(XtreemProperties.class);
		cfg.setProperty(XtreemProperties.XTREEMFS_LOCAL_MOUNT, null);
		String url="http://localhost:65321/services/StorageManagement?res=WORK";
		cfg.setProperty(XtreemProperties.XTREEMFS_REMOTE_URL, url);
		
		JobDefinitionDocument job=JobDefinitionDocument.Factory.newInstance();
		job.addNewJobDefinition().addNewJobDescription().addNewApplication().setApplicationName("Date");
		DataStagingType dst=job.getJobDefinition().getJobDescription().addNewDataStaging();
		dst.setFileName("/");
		String path="outfolder/";
		dst.addNewTarget().setURI("xtreemfs://"+path);
		Action a=config.makeAction(job);
		a.getProcessingContext().put(Action.AUTO_SUBMIT,Boolean.TRUE);
		Client client=makeClient();
		config.get(Manager.class).add(a, client);
		waitForFinished(a.getUUID(), client);
		a=config.get(InternalManager.class).getAction(a.getUUID());
		assertTrue(a.getResult().isSuccessful());
		//check that files were exported
		File exported1=new File("target/unicorex-test",path+"/stdout");
		assertTrue(exported1.exists());
		File exported2=new File("target/unicorex-test",path+"/stderr");
		assertTrue(exported2.exists());
	}
	
	
	@Test
	public void testStageIn()throws Exception{
		XtreemProperties cfg = kernel.getAttribute(XtreemProperties.class);
		cfg.setProperty(XtreemProperties.XTREEMFS_LOCAL_MOUNT, null);
		String url="http://localhost:65321/services/StorageManagement?res=WORK";
		cfg.setProperty(XtreemProperties.XTREEMFS_REMOTE_URL, url);
		JobDefinitionDocument job=JobDefinitionDocument.Factory.newInstance();
		job.addNewJobDefinition().addNewJobDescription().addNewApplication().setApplicationName("Date");
		DataStagingType dst=job.getJobDefinition().getJobDescription().addNewDataStaging();
		dst.setFileName("infile");
		String path="testdata";
		
		//import test data
		EndpointReferenceType epr=EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue(url);
		StorageClient sms=new StorageClient(epr,kernel.getClientConfiguration());
		InputStream is=new ByteArrayInputStream("test".getBytes());
		sms.getImport("testdata", ProtocolType.BFT).writeAllData(is);
		
		dst.addNewSource().setURI("xtreemfs://"+path);
		Action a=config.makeAction(job);
		a.getProcessingContext().put(Action.AUTO_SUBMIT,Boolean.TRUE);
		Client client=makeClient();
		config.get(Manager.class).add(a, client);
		waitForFinished(a.getUUID(), client);
		a=config.get(InternalManager.class).getAction(a.getUUID());
		assertTrue(a.getResult().isSuccessful());
		//check that file was exported
		File imported=new File(a.getExecutionContext().getWorkingDirectory()+"infile");
		assertTrue(imported.exists());
	}

	protected void waitForFinished(String id, Client client)throws Exception{
		int i=0;
		while(i<300){
			if(ActionStatus.DONE==config.get(Manager.class).getStatus(id, client)){
				break;
			}
			else{
				Thread.sleep(1000);
				i++;
			}
		}
	}
	
	private Client makeClient(){
		Client client=new Client();
		SecurityTokens t=new SecurityTokens();
		t.setUserName("CN=test");
		client.setAuthenticatedClient(t);
		return client;
	}
	
}
