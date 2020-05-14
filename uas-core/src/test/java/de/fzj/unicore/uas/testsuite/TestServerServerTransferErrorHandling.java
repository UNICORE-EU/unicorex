package de.fzj.unicore.uas.testsuite;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.unigrids.x2006.x04.services.fts.SummaryType;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.Base;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.UASProperties;
import de.fzj.unicore.uas.client.StorageClient;
import de.fzj.unicore.uas.client.StorageFactoryClient;
import de.fzj.unicore.uas.client.TransferControllerClient;
import eu.unicore.bugsreporter.annotation.FunctionalTest;
import eu.unicore.services.ws.utils.WSServerUtilities;

/**
 * tests error handling for sendFile() and receiveFile()
 * 
 * @author schuller
 */
public class TestServerServerTransferErrorHandling extends Base {

	EndpointReferenceType factory;
	StorageFactoryClient sfc;
	StorageClient source, target;
	String sourceURL,targetURL;
	
	
	@FunctionalTest(id="ServerServerFTErrorsTest", 
					description="Tests error handling of server-to-server filetransfers")
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
	
	protected void reInitTarget() throws Exception {
		target=sfc.createSMS();
		targetURL=target.getEPR().getAddress().getStringValue();
	}

	protected void dataTransfer()throws Exception{
		String [] protocols=new String[]{"BFT"};
		for(String protocol: protocols){
			System.out.println("\n\nTesting error handling of server-server transfer using <"+protocol+">");
			System.out.println(" ... Fetch single file");
			fetchSingleFileWhichDoesNotExist(protocol);
		}
	}

	protected void fetchSingleFileWhichDoesNotExist(String protocol)throws Exception{
		reInitTarget();
		TransferControllerClient c=target.fetchFile(protocol+":"+sourceURL+"#/noSuchFile",
				"/in");
		
		assertTrue(c!=null);
		int cnt=0;
		while(!c.isComplete()&& !c.hasFailed()){
			if(cnt>0)System.out.print(".");
			Thread.sleep(1000);
			cnt++;
			if(cnt>100)throw new Exception("Filetransfer took too long, aborting test...");
		}
		
		assertTrue(SummaryType.FAILED.equals(c.getStatusSummary()));
	}

	protected void sendSingleFile(String protocol)throws Exception{
		reInitTarget();
		TransferControllerClient c=source.sendFile("/test.txt",
				protocol+":"+targetURL+"#testfile2");
		assertTrue(c!=null);

		int cnt=0;
		do{
			if(cnt>0)System.out.print(".");
			Thread.sleep(1000);
			cnt++;
			if(cnt>20)throw new Exception("Filetransfer took too long, aborting test...");
		}while(!c.isComplete() && !c.hasFailed());
		assertTrue(SummaryType.DONE.equals(c.getStatusSummary()));
	}


}
