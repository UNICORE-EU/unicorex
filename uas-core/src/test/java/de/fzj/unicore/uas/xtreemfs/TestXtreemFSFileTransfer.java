package de.fzj.unicore.uas.xtreemfs;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.DataStagingType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import de.fzj.unicore.uas.xnjs.U6HttpConnectionFactory;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.security.TestConfigUtil;
import de.fzj.unicore.xnjs.ConfigurationSource;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.ems.InternalManager;
import de.fzj.unicore.xnjs.ems.Manager;
import de.fzj.unicore.xnjs.io.DataStageOutInfo;
import de.fzj.unicore.xnjs.io.IFileTransfer;
import de.fzj.unicore.xnjs.io.IFileTransferEngine;
import de.fzj.unicore.xnjs.io.http.IConnectionFactory;
import de.fzj.unicore.xnjs.persistence.IActionStoreFactory;
import de.fzj.unicore.xnjs.persistence.JDBCActionStoreFactory;
import de.fzj.unicore.xnjs.ems.BasicManager;
import de.fzj.unicore.xnjs.tsi.local.LocalTSIModule;
import eu.unicore.security.Client;
import eu.unicore.security.SecurityTokens;
import eu.unicore.util.httpclient.IClientConfiguration;

public class TestXtreemFSFileTransfer {

	private static XNJS xnjs;
	private static Kernel k;
	
	@BeforeClass
	public static void setupXNJS()throws Exception{
		FileUtils.deleteQuietly(new File("target","data"));
		FileUtils.deleteQuietly(new File("target","xnjs_data"));
		k=new Kernel(TestConfigUtil.getInsecureProperties());
		ConfigurationSource cs = new ConfigurationSource();
		try(FileInputStream fis = new FileInputStream("src/test/resources/xnjs.properties")){
			cs.getProperties().load(fis);
		}
		cs.addModule(new MyBaseModule(cs.getProperties()));
		cs.addModule(new LocalTSIModule(cs.getProperties()));
		xnjs=new XNJS(cs);
		xnjs.start();
		
	}
	
	public static class MyBaseModule extends AbstractModule {
		
		protected final Properties properties;
		
		public MyBaseModule(Properties properties){
			this.properties = properties;
		}

		@Override
		protected void configure(){
			bind(InternalManager.class).to(BasicManager.class);
			bind(Manager.class).to(BasicManager.class);
			bind(IActionStoreFactory.class).to(JDBCActionStoreFactory.class);
		}
		
		@Provides
		public Kernel getKernel(){
			return k;
		}
		
		@Provides
		public IClientConfiguration getSecurityConfiguration(){
			return getKernel().getClientConfiguration();
		}
		
		@Provides
		public IConnectionFactory getConnectionFactory(){
			return new U6HttpConnectionFactory(getKernel());
		}
		
		
	}
	
	
	@AfterClass
	public static void shutdownXNJS()throws Exception{
		xnjs.stop();
		FileUtils.deleteQuietly(new File("target","data"));
		FileUtils.deleteQuietly(new File("target","xnjs_data"));
	}
	
	@Test
	public void testCreatorRegistration(){
		IFileTransferEngine sf=xnjs.get(IFileTransferEngine.class);
		String[]p=sf.listProtocols();
		assertTrue(Arrays.asList(p).contains("xtreemfs"));
	}
	
	@Test
	public void testCreateExport()throws Exception{
		IFileTransferEngine sf=xnjs.get(IFileTransferEngine.class);
		Client client=makeClient();
		String source="test";
		String wd="target";
		URI target=new URI("xtreemfs://CN=Test,C=DE/test.txt");
		DataStageOutInfo info = new DataStageOutInfo();
		info.setFileName(source);
		info.setTarget(target);
		IFileTransfer ft=sf.createFileExport(client, wd, info);
		assertNotNull(ft);
	}
	
	@Test
	public void testStageOut()throws Exception{
		String targetDir=new File("target").getAbsolutePath();
		xnjs.setProperty(Constants.XTREEMFS_LOCAL_MOUNT, targetDir);
		
		JobDefinitionDocument job=JobDefinitionDocument.Factory.newInstance();
		job.addNewJobDefinition().addNewJobDescription().addNewApplication().setApplicationName("Date");
		DataStagingType dst=job.getJobDefinition().getJobDescription().addNewDataStaging();
		dst.setFileName("stdout");
		String path="CN=test/test123";
		dst.addNewTarget().setURI("xtreemfs://"+path);
		Action a=xnjs.makeAction(job);
		a.getProcessingContext().put(Action.AUTO_SUBMIT,Boolean.TRUE);
		Client client=makeClient();
		xnjs.get(Manager.class).add(a, client);
		waitForFinished(a.getUUID(), client);
		a=xnjs.get(InternalManager.class).getAction(a.getUUID());
		assertTrue(a.getResult().isSuccessful());
		//check that file was exported
		File exported=new File(targetDir,path);
		assertTrue(exported.exists());
	}

	@Test
	public void testStageIn()throws Exception{
		String sourceDir=new File("src/test/resources").getAbsolutePath();
		xnjs.setProperty(Constants.XTREEMFS_LOCAL_MOUNT, sourceDir);
		JobDefinitionDocument job=JobDefinitionDocument.Factory.newInstance();
		job.addNewJobDefinition().addNewJobDescription().addNewApplication().setApplicationName("Date");
		DataStagingType dst=job.getJobDefinition().getJobDescription().addNewDataStaging();
		dst.setFileName("infile");
		String path="simpleidb";
		dst.addNewSource().setURI("xtreemfs://"+path);
		Action a=xnjs.makeAction(job);
		a.getProcessingContext().put(Action.AUTO_SUBMIT,Boolean.TRUE);
		Client client=makeClient();
		xnjs.get(Manager.class).add(a, client);
		waitForFinished(a.getUUID(), client);
		a=xnjs.get(InternalManager.class).getAction(a.getUUID());
		assertTrue(a.getResult().isSuccessful());
		//check that file was exported
		File imported=new File(a.getExecutionContext().getWorkingDirectory()+"infile");
		assertTrue(imported.exists());
	}
	
	protected void waitForFinished(String id, Client client)throws Exception{
		int i=0;
		while(i<10){
			if(ActionStatus.DONE==xnjs.get(Manager.class).getStatus(id, client)){
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
