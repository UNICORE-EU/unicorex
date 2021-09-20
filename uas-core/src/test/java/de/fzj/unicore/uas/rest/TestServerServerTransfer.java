package de.fzj.unicore.uas.rest;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;

import org.junit.Test;

import de.fzj.unicore.uas.Base;
import de.fzj.unicore.uas.UASProperties;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.FileList;
import eu.unicore.client.core.FileList.FileListEntry;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.core.StorageFactoryClient;
import eu.unicore.client.data.TransferControllerClient;
import eu.unicore.services.rest.client.UsernamePassword;

/**
 * tests server-to-server transfer
 * 
 * @author schuller
 */
public class TestServerServerTransfer extends Base {

	StorageFactoryClient sfc;
	StorageClient source, target;
	String sourceURL,targetURL;


	@Test
	public void testTransfer()throws Exception{
		String url = kernel.getContainerProperties().getContainerURL()+"/rest";
		Endpoint sfcEndpoint = new Endpoint(url+"/core/storagefactories/default_storage_factory");
		sfc = new StorageFactoryClient(sfcEndpoint, kernel.getClientConfiguration(),
				new UsernamePassword("demouser", "test123"));
		initSource();
		UASProperties cfg = kernel.getAttribute(UASProperties.class);
		for(String noOpt: new String[]{"true", "false"}){
			System.out.println("Testing with disabled local copy optimization: "+noOpt);
			cfg.setProperty(UASProperties.SMS_TRANSFER_FORCEREMOTE, noOpt);
			dataTransfer();
			//checkSpacesInNames();
			fetchSingleFileWhichDoesNotExist();
		}
	}
	
	@Test
	public void testTransferWildcards()throws Exception{
		String url = kernel.getContainerProperties().getContainerURL()+"/rest";
		Endpoint sfcEndpoint = new Endpoint(url+"/core/storagefactories/default_storage_factory");
		sfc = new StorageFactoryClient(sfcEndpoint, kernel.getClientConfiguration(),
				new UsernamePassword("demouser", "test123"));
		
		initSource();
		UASProperties cfg = kernel.getAttribute(UASProperties.class);
		for(String noOpt: new String[]{"true", "false"}){
			System.out.println("Testing with disabled local copy optimization: "+noOpt);
			cfg.setProperty(UASProperties.SMS_TRANSFER_FORCEREMOTE, noOpt);
			String [] protocols=new String[]{"BFT",};
			for(String protocol: protocols){
				System.out.println("\n\nTesting wildcards server-server transfer using <"+protocol+">");

				System.out.println(" ... Send using wildcards");
				sendWildcard(protocol);
				
				System.out.println(" ... Fetch using wildcards");
				fetchWildcard(protocol);
				
				System.out.println(" ... Fetch using directory wildcards");
				fetchWildcard2(protocol);
			}
		}
	}

	protected void initSource() throws Exception {
		source = sfc.createStorage();
		source.mkdir("folder1/folder11");
		source.mkdir("folder2");
		source.upload("folder1/test11").write("test11".getBytes());
		source.upload("folder1/test12").write("test12".getBytes());
		source.upload("folder1/zeros");
		source.upload("folder1/folder11/test111").write("test111".getBytes());
		source.upload("folder2/test21").write("test12".getBytes());
		source.upload("test.txt").write("this is a test".getBytes());
		source.upload("test1.txt").write("this is a test".getBytes());
		sourceURL = source.getEndpoint().getUrl();
	}

	protected void verifyTargetFolder1Content()throws Exception{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		target.download("folder1/test11").readAllData(bos);
		assertTrue("test11".equals(bos.toString()));
		bos=new ByteArrayOutputStream();
		target.download("folder1/folder11/test111").readAllData(bos);
		assertTrue("test111".equals(bos.toString()));
	}

	protected void verifyTargetFolderWildcards(boolean included, String targetDir, String... files)throws Exception{
		FileList lsResult = target.ls(targetDir);
		for(String f: files){
			if(included){
				assertTrue("File "+f+" is missing.",contains(f, lsResult));
			}
			else{
				assertTrue("File "+f+" was wrongly included.",!contains(f, lsResult));
			}
		}
	}

	protected void reInitTarget() throws Exception {
		target = sfc.createStorage();
		targetURL = target.getEndpoint().getUrl();
	}

	protected void dataTransfer()throws Exception{
		String [] protocols=new String[]{"BFT"};
		for(String protocol: protocols){
			System.out.println("\n\nTesting server-server transfer using <"+protocol+">");
			System.out.println(" ... Fetch single file");
			fetchSingleFile(protocol);
			System.out.println(" ... Send single file");
			sendSingleFile(protocol);
			System.out.println(" ... Fetch folder");
			fetchFolder(protocol);
			System.out.println(" ... Send folder");
			sendFolder(protocol);
		}
	}

	protected void fetchSingleFile(String protocol)throws Exception{
		reInitTarget();
		TransferControllerClient c = target.fetchFile(sourceURL+"/files/test.txt", "/test.txt", protocol);
		assertTrue(c!=null);
		int cnt=0;
		while(!c.isComplete()&& !c.hasFailed()){
			if(cnt>0)System.out.print(".");
			Thread.sleep(1000);
			cnt++;
			if(cnt>100)throw new Exception("Filetransfer took too long, aborting test...");
		}
	}


	protected void fetchFolder(String protocol)throws Exception{
		reInitTarget();
		TransferControllerClient c = target.fetchFile(sourceURL+"/files/folder1", "/folder1", protocol);
		assertTrue(c!=null);

		int cnt=0;
		while(!c.isComplete()&& !c.hasFailed()){
			System.out.print(".");
			Thread.sleep(1000);
			cnt++;
			if(cnt>1000)throw new Exception("Filetransfer took too long, aborting test...");
		}
		verifyTargetFolder1Content();
	}

	protected void fetchWildcard(String protocol)throws Exception{
		reInitTarget();
		TransferControllerClient c = target.fetchFile(sourceURL+"/files/folder1/test1*", "/folder1", protocol);
		assertTrue(c!=null);

		int cnt=0;
		while(!c.isComplete()&& !c.hasFailed()){
			System.out.print(c.getProperties().toString(2));
			Thread.sleep(2000);
			cnt++;
			if(cnt>120)throw new Exception("Filetransfer took too long, aborting test...");
		}
		System.out.println(c.getProperties().toString(2));
		
		verifyTargetFolderWildcards(true,"folder1","test11","test12");
		verifyTargetFolderWildcards(false,"folder1","zeros","test111");
	}


	protected void fetchWildcard2(String protocol)throws Exception{
		reInitTarget();
		TransferControllerClient c = target.fetchFile(sourceURL+"/files/folder*/test*", "/target", protocol);
		assertTrue(c!=null);

		int cnt=0;
		while(!c.isComplete()&& !c.hasFailed()){
			System.out.print(".");
			Thread.sleep(1000);
			cnt++;
			if(cnt>1000)throw new Exception("Filetransfer took too long, aborting test...");
		}
		System.out.println(c.getProperties().toString(2));
		
		verifyTargetFolderWildcards(true, "target/folder1", "test11", "test12");
		verifyTargetFolderWildcards(true, "target/folder2", "test21");
		verifyTargetFolderWildcards(false, "target/folder1", "folder11/test111");
	}

	protected TransferControllerClient sendSingleFile(String protocol)throws Exception{
		reInitTarget();
		TransferControllerClient c = source.sendFile("/test.txt", targetURL+"/files/testfile2", protocol);
		assertTrue(c!=null);

		int cnt=0;
		do{
			if(cnt>0)System.out.print(".");
			Thread.sleep(1000);
			cnt++;
			if(cnt>100)throw new Exception("Filetransfer took too long, aborting test...");
		}while(!c.isComplete() && !c.hasFailed());
		return c;
	}

	protected void sendFolder(String protocol)throws Exception{
		reInitTarget();
		TransferControllerClient c = source.sendFile("folder1", targetURL+"/files/folder1", protocol);
		assertTrue(c!=null);

		int cnt=0;
		while(!c.isComplete()&& !c.hasFailed()){
			System.out.print(".");
			Thread.sleep(1000);
			cnt++;
			if(cnt>12000)throw new Exception("Filetransfer took too long, aborting test...");
		}
		verifyTargetFolder1Content();
	}

	protected void sendWildcard(String protocol)throws Exception{
		reInitTarget();
		TransferControllerClient c = source.sendFile("folder1/test1*", targetURL+"/files/folder1", protocol);
		assertTrue(c!=null);

		int cnt=0;
		while(!c.isComplete()&& !c.hasFailed()){
			System.out.print(".");
			Thread.sleep(1000);
			cnt++;
			if(cnt>12000)throw new Exception("Filetransfer took too long, aborting test...");
		}
		System.out.println(c.getProperties().toString(2));
		verifyTargetFolderWildcards(true,"folder1","test11","test12");
		verifyTargetFolderWildcards(false,"folder1","zeros");
	}

	protected void checkSpacesInNames()throws Exception{
		reInitTarget();
		//import a file that includes a space character
		String testdata = "testdata";
		source.upload("test%20file").write(testdata.getBytes());
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		source.download("test file").readAllData(bos);
		assertTrue(testdata.equals(bos.toString()));
		bos = new ByteArrayOutputStream();
		source.download("test%20file").readAllData(bos);
		assertTrue(testdata.equals(bos.toString()));

		TransferControllerClient c1 = source.sendFile("/test file", targetURL+"/files/test%20file", "BFT");
		int cnt=0;
		do{
			if(cnt>0)System.out.println("Not complete...");
			Thread.sleep(1000);
			cnt++;
			if(cnt>100)throw new Exception("Filetransfer took too long, aborting test...");
		}while(!c1.isComplete() && !c1.hasFailed());

		//get data from sms and check correctness
		bos.reset();
		target.download("test file").readAllData(bos);
		assertTrue(testdata.equals(bos.toString()));

		//same story with receiveFile
		c1 = source.fetchFile(targetURL+"/files/test%20file","/another test file", "BFT");
		assertTrue(c1!=null);
		cnt=0;
		do{
			if(cnt>0)System.out.println("Not complete...");
			Thread.sleep(1000);
			cnt++;
		}while(!c1.isComplete() && !c1.hasFailed());
		System.out.println("Size of remote file = "+c1.getSize());
		System.out.println("Transferred         = "+c1.getTransferredBytes());
		System.out.println("Final status        = "+c1.getStatus());

		//get data from sms and check correctness
		bos.reset();
		source.download("another test file").readAllData(bos);
		assertTrue(testdata.equals(bos.toString()));
	}

	boolean contains(String fileName, FileList lsResult) throws Exception {
		for(FileListEntry gf: lsResult.list(0, 1000)){
			if(gf.path.endsWith(fileName))return true;
		}
		return false;
	}


	protected void fetchSingleFileWhichDoesNotExist() throws Exception{
		reInitTarget();
		TransferControllerClient c = target.fetchFile(sourceURL+"/files/noSuchFile", "/in", "BFT");
		int cnt=0;
		while(!c.isComplete()&& !c.hasFailed()){
			if(cnt>0)System.out.print(".");
			Thread.sleep(1000);
			cnt++;
			if(cnt>100)throw new Exception("Filetransfer took too long, aborting test...");
		}
		System.out.println("ERROR MESSAGE: "+c.getStatusMessage());
		assertTrue(c.hasFailed());
	}
}
