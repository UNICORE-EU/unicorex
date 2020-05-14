package de.fzj.unicore.uas.testsuite;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import org.unigrids.services.atomic.types.GridFileType;
import org.unigrids.services.atomic.types.ProtocolType;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.client.FileTransferClient;
import de.fzj.unicore.uas.client.JobClient;
import de.fzj.unicore.uas.client.StorageClient;
import eu.unicore.bugsreporter.annotation.FunctionalTest;

/**
 * tests the basic filetransfer client functions for all the standard protocols
 * 
 * @see FileTransferClient
 * @author schuller
 */
public class TestFileTransfer extends AbstractJobRun{

	String url;
	EndpointReferenceType tssepr;

	final ProtocolType.Enum[] protocols=
		{ProtocolType.BFT};

	final String testFileName="/testfile";

	StorageClient sms;

	@FunctionalTest(id="RunFTTest", 
					description="Tests standard filetransfer protocols (BFT)")
	@Override
	public void testRunJob()throws Exception{
		initClients();
		JobClient job=submitJob(tss);
		sms=job.getUspaceClient();

		//import data
		String data="testdata";

		//mock extra parameters for file transfers
		Map<String,String>extra=new HashMap<String,String>();
		extra.put("something","something else");

		for(ProtocolType.Enum protocol: protocols){
			ByteArrayInputStream bis=new ByteArrayInputStream(data.getBytes());

			FileTransferClient fts=sms.getImport(testFileName, false, extra, protocol);
			System.out.println("Importing to storage "+fts.getParentStorage().getAddress().getStringValue());
			fts.writeAllData(bis);
			//now check properties
			GridFileType props=sms.listProperties(testFileName);
			assertTrue(data.length()==props.getSize());
			fts.destroy();

			//append data
			bis=new ByteArrayInputStream(data.getBytes());
			fts=sms.getImport(testFileName, true, extra, protocol);
			fts.writeAllData(bis);
			props=sms.listProperties(testFileName);
			assertTrue(2*data.length()==props.getSize());
			fts.destroy();

			//and overwrite 
			bis=new ByteArrayInputStream(data.getBytes());
			fts=sms.getImport(testFileName, false, extra, protocol);
			fts.writeAllData(bis);
			props=sms.listProperties(testFileName);
			assertTrue(data.length()==props.getSize());
			fts.destroy();

			//finally remove file
			sms.delete(testFileName);
		}
	}


}
