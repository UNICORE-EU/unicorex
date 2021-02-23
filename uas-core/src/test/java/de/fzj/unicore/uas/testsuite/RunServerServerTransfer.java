package de.fzj.unicore.uas.testsuite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;

import org.junit.Test;
import org.unigrids.services.atomic.types.GridFileType;
import org.unigrids.services.atomic.types.ProtocolType;
import org.unigrids.x2006.x04.services.fts.SummaryType;
import org.w3.x2005.x08.addressing.EndpointReferenceDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.Base;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.UASProperties;
import de.fzj.unicore.uas.client.EnumerationClient;
import de.fzj.unicore.uas.client.StorageClient;
import de.fzj.unicore.uas.client.StorageFactoryClient;
import de.fzj.unicore.uas.client.TransferControllerClient;
import eu.unicore.services.ws.utils.WSServerUtilities;

/**
 * tests sendFile() and receiveFile()
 * 
 * @author schuller
 */
public class RunServerServerTransfer extends Base {

	EndpointReferenceType factory;
	StorageFactoryClient sfc;
	StorageClient source, target;
	String sourceURL,targetURL;


	@Test
	public void testTransfer()throws Exception{
		factory= WSServerUtilities.makeEPR(UAS.SMF, "default_storage_factory",kernel);
		sfc=new StorageFactoryClient(factory,kernel.getClientConfiguration());
		initSource();
		UASProperties cfg = kernel.getAttribute(UASProperties.class);
		for(String noOpt: new String[]{"true", "false"}){
			System.out.println("Testing with disabled local copy optimization: "+noOpt);
			cfg.setProperty(UASProperties.SMS_TRANSFER_FORCEREMOTE, noOpt);
			dataTransfer();
			checkSpacesInNames();
		}
	}

	@Test
	public void testTransferReferences()throws Exception{
		factory= WSServerUtilities.makeEPR(UAS.SMF, "default_storage_factory",kernel);
		sfc=new StorageFactoryClient(factory,kernel.getClientConfiguration());
		source=sfc.createSMS();
		source.upload("test.txt").write("test123".getBytes());
		reInitTarget();
		TransferControllerClient c=sendSingleFile("BFT");
		EnumerationClient<EndpointReferenceDocument> ftReferences=source.getFiletransferEnumeration();
		assertEquals(1, ftReferences.getNumberOfResults());
		EndpointReferenceDocument ft1=ftReferences.getResults(0, 1).get(0);
		System.out.println(ft1);
		System.out.println("File transfer url = "+
				ft1.getEndpointReference().getAddress().getStringValue());

		System.out.println(source.getResourcePropertyDocument());

		c.destroy();
		ftReferences.setUpdateInterval(-1);
		assertEquals(0, ftReferences.getNumberOfResults());
	}

	@Test
	public void testTransferWildcards()throws Exception{
		factory= WSServerUtilities.makeEPR(UAS.SMF, "default_storage_factory",kernel);
		sfc=new StorageFactoryClient(factory,kernel.getClientConfiguration());
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
		source=sfc.createSMS();
		source.createDirectory("folder1/folder11");
		source.createDirectory("folder2");
		source.upload("folder1/test11").write("test11".getBytes());
		source.upload("folder1/test12").write("test12".getBytes());
		source.upload("folder1/zeros");
		source.upload("folder1/folder11/test111").write("test111".getBytes());
		source.upload("folder2/test21").write("test12".getBytes());
		source.upload("test.txt").write("this is a test".getBytes());
		source.upload("test1.txt").write("this is a test".getBytes());
		sourceURL=source.getEPR().getAddress().getStringValue();
	}

	protected void verifyTargetFolder1Content()throws Exception{
		ByteArrayOutputStream bos=new ByteArrayOutputStream();
		target.getExport("folder1/test11", ProtocolType.BFT).readAllData(bos);
		assertTrue("test11".equals(bos.toString()));
		bos=new ByteArrayOutputStream();
		target.getExport("folder1/folder11/test111", ProtocolType.BFT).readAllData(bos);
		assertTrue("test111".equals(bos.toString()));
	}

	protected void verifyTargetFolderWildcards(boolean included, String targetDir, String... files)throws Exception{
		GridFileType[]lsResult = target.listDirectory(targetDir);
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
		target=sfc.createSMS();
		targetURL=target.getEPR().getAddress().getStringValue();
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
		TransferControllerClient c=target.fetchFile(protocol+":"+sourceURL+"#test.txt",
				"/test.txt");
		assertTrue(c!=null);
		int cnt=0;
		while(!c.isComplete()&& !c.hasFailed()){
			if(cnt>0)System.out.print(".");
			Thread.sleep(1000);
			cnt++;
			if(cnt>100)throw new Exception("Filetransfer took too long, aborting test...");
		}
		assertTrue(SummaryType.DONE.equals(c.getStatusSummary()));
		//check rate property
		System.out.println("Transfer rate (according to server) " + c.getRate()+" Bytes/sec.");
		//assertTrue(c.getRate()>-1);
	}


	protected void fetchFolder(String protocol)throws Exception{
		reInitTarget();
		TransferControllerClient c=target.fetchFile(protocol+":"+sourceURL+"#folder1",
				"/folder1");
		assertTrue(c!=null);

		int cnt=0;
		while(!c.isComplete()&& !c.hasFailed()){
			System.out.print(".");
			Thread.sleep(1000);
			cnt++;
			if(cnt>1000)throw new Exception("Filetransfer took too long, aborting test...");
		}
		assertTrue(SummaryType.DONE.equals(c.getStatusSummary()));
		verifyTargetFolder1Content();
	}

	protected void fetchWildcard(String protocol)throws Exception{
		reInitTarget();
		TransferControllerClient c=target.fetchFile(protocol+":"+sourceURL+"#folder1/test1*",
				"/folder1");
		assertTrue(c!=null);

		int cnt=0;
		while(!c.isComplete()&& !c.hasFailed()){
			System.out.print(".");
			Thread.sleep(1000);
			cnt++;
			if(cnt>1000)throw new Exception("Filetransfer took too long, aborting test...");
		}
		assertTrue(SummaryType.DONE.equals(c.getStatusSummary()));
		verifyTargetFolderWildcards(true,"folder1","test11","test12");
		verifyTargetFolderWildcards(false,"folder1","zeros","test111");
	}


	protected void fetchWildcard2(String protocol)throws Exception{
		reInitTarget();
		TransferControllerClient c=target.fetchFile(protocol+":"+sourceURL+"#folder*/test*",
				"/target");
		assertTrue(c!=null);

		int cnt=0;
		while(!c.isComplete()&& !c.hasFailed()){
			System.out.print(".");
			Thread.sleep(1000);
			cnt++;
			if(cnt>1000)throw new Exception("Filetransfer took too long, aborting test...");
		}
		assertTrue(SummaryType.DONE.equals(c.getStatusSummary()));
		verifyTargetFolderWildcards(true, "target/folder1", "test11", "test12");
		verifyTargetFolderWildcards(true, "target/folder2", "test21");
		verifyTargetFolderWildcards(false, "target/folder1/folder11", "test111");
	}

	protected TransferControllerClient sendSingleFile(String protocol)throws Exception{
		reInitTarget();
		TransferControllerClient c=source.sendFile("/test.txt",
				protocol+":"+targetURL+"#testfile2");
		assertTrue(c!=null);

		int cnt=0;
		do{
			if(cnt>0)System.out.print(".");
			Thread.sleep(1000);
			cnt++;
			if(cnt>100)throw new Exception("Filetransfer took too long, aborting test...");
		}while(!c.isComplete() && !c.hasFailed());
		assertTrue(SummaryType.DONE.equals(c.getStatusSummary()));
		//check rate property
		System.out.println("Transfer rate (according to server) " + c.getRate()+" Bytes/sec.");
		//assertTrue(c.getRate()>-1);
		return c;
	}

	protected void sendFolder(String protocol)throws Exception{
		reInitTarget();
		TransferControllerClient c=source.sendFile("folder1",protocol+":"+targetURL+"#folder1");
		assertTrue(c!=null);

		int cnt=0;
		while(!c.isComplete()&& !c.hasFailed()){
			System.out.print(".");
			Thread.sleep(1000);
			cnt++;
			if(cnt>12000)throw new Exception("Filetransfer took too long, aborting test...");
		}
		assertTrue(SummaryType.DONE.equals(c.getStatusSummary()));
		verifyTargetFolder1Content();
	}

	protected void sendWildcard(String protocol)throws Exception{
		reInitTarget();
		TransferControllerClient c=source.sendFile("folder1/test1*",protocol+":"+targetURL+"#folder1");
		assertTrue(c!=null);

		int cnt=0;
		while(!c.isComplete()&& !c.hasFailed()){
			System.out.print(".");
			Thread.sleep(1000);
			cnt++;
			if(cnt>12000)throw new Exception("Filetransfer took too long, aborting test...");
		}
		assertTrue(SummaryType.DONE.equals(c.getStatusSummary()));
		verifyTargetFolderWildcards(true,"folder1","test11","test12");
		verifyTargetFolderWildcards(false,"folder1","zeros");
	}

	protected void checkSpacesInNames()throws Exception{
		//import a file that includes a space character
		String testdata="testdata";
		source.upload("test%20file").write(testdata.getBytes());
		ByteArrayOutputStream bos=new ByteArrayOutputStream();
		source.download("test file").readAllData(bos);
		assertTrue(testdata.equals(bos.toString()));

		//now check we can send this to our default storage
		//tell uspace sms to send a file to the target sms
		TransferControllerClient c1=source.sendFile("/test file","BFT:"+targetURL+"#test file");
		assertTrue(c1!=null);
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
		c1=source.fetchFile("BFT:"+targetURL+"#test file","/another test file");
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

	boolean contains(String fileName, GridFileType[]lsResult){
		for(GridFileType gf: lsResult){
			if(gf.getPath().endsWith(fileName))return true;
		}
		return false;
	}

}
