package de.fzj.unicore.uas.testsuite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ApplicationType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.unigrids.services.atomic.types.GridFileType;
import org.unigrids.services.atomic.types.ProtocolType;
import org.unigrids.x2006.x04.services.sms.ImportFileDocument;
import org.unigrids.x2006.x04.services.sms.ImportFileResponseDocument;

import de.fzj.unicore.uas.client.FileTransferClient;
import de.fzj.unicore.uas.client.HttpFileTransferClient;
import de.fzj.unicore.uas.client.JobClient;
import de.fzj.unicore.uas.client.StorageClient;
import de.fzj.unicore.uas.fts.http.FileServlet;
import de.fzj.unicore.wsrflite.utils.StopWatch;
import de.fzj.unicore.wsrflite.xmlbeans.WSUtilities;
import eu.unicore.bugsreporter.annotation.FunctionalTest;
import eu.unicore.util.httpclient.HttpUtils;
import junit.framework.Assert;

/**
 * runs http filetransfer tests on a newly created USspace<br/>
 * 
 * @author schuller
 */
public class HttpTest extends AbstractJobRun{

	private StorageClient sms;

	@Override
	@FunctionalTest(id="HTTPTest", description="Tests the HTTP file transfer")
	public void testRunJob()throws Exception{
		initClients();
		Calendar c=tss.getCurrentTime();
		assertTrue(c!=null);
		runJob(tss);
	}


	@Override
	protected JobDefinitionDocument getJob() {
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.newInstance();
		ApplicationType app=jdd.addNewJobDefinition().addNewJobDescription().addNewApplication();
		app.setApplicationName("Date");
		app.setApplicationVersion("1.0");
		return jdd;
	}


	@Override
	protected void onFinish(JobClient jms) throws Exception {
		sms=jms.getUspaceClient();

		// data stage in with perf measurement
		HttpFileTransferClient importClient=(HttpFileTransferClient)sms.getImport("bigfile", true, null, ProtocolType.BFT);
		int size=128000;
		int loops=100;
		byte[] data=new byte[size];
		Random r=new Random();

		StopWatch sw=new StopWatch();
		long total=size*loops;

		sw.start("Start import of "+total/(1024*1024)+ " MBytes");
		System.out.println("Start import of "+size*loops/(1024*1024)+ " MBytes");
		for (int i = 0; i < loops; i++) {
			r.nextBytes(data);
			importClient.write(data);
		}
		sw.stop("End.");
		long time=sw.getCurrentTotal();
		System.out.println("End of data upload, time="+time/1000+ " sec");
		System.out.println("Rate="+total/time+ " kB/sec.");

		//file size
		GridFileType gf=sms.listProperties("bigfile");
		assertEquals(total, gf.getSize());

		sw.start("Start Baseline/HTTP export.");

		FileTransferClient ft=sms.getExport("bigfile", ProtocolType.BFT);
		ft.readAllData(new NullOutputStream());
		sw.stop("End HTTP.");
		time=sw.getCurrentTotal();
		System.out.println("End of http data download, time="+time/1000+ " sec");
		System.out.println("Rate="+(size*loops)/time+ " kB/sec.");

		// check that export URL gives a 404 after destroying the resource
		ft.destroy();
		try{
			kernel.getAttribute(FileServlet.class).getResource(WSUtilities.extractResourceID(ft.getUrl()));
			ft.readAllData(new NullOutputStream());
		}catch(IOException ioe){
			assertTrue(ioe.getMessage().contains("404 Not Found"));
		}
		
		//test putting a file

		String testString="this is a test";
		ByteArrayInputStream bis=new ByteArrayInputStream(testString.getBytes());
		ImportFileDocument imp=ImportFileDocument.Factory.newInstance();
		imp.addNewImportFile().setProtocol(ProtocolType.BFT);
		imp.getImportFile().setDestination("/testfile");

		ImportFileResponseDocument res2=sms.ImportFile(imp);
		HttpFileTransferClient http2=new HttpFileTransferClient(
				res2.getImportFileResponse().getImportEPR(),
				kernel.getClientConfiguration());
		http2.writeAllData(bis);
		http2.close();
		//and read it again
		ByteArrayOutputStream os=new ByteArrayOutputStream();
		sms.download("/testfile").readAllData(os);
		System.out.println(os.toString());
		assertTrue(os.toString().equals(testString));


		//test error: export non existent file
		try{
			sms.download("no such file");
			Assert.fail("Expected exception since file does not exist");
		}catch(Exception e){
			assertTrue("Got: "+e.getMessage(),e.getMessage().contains("FileNotFound"));
		}

		partialRead();

	}

	//test if we can read a range of a file
	protected void partialRead()throws Exception{
		String testString="this_is_a_test";
		ByteArrayInputStream bis=new ByteArrayInputStream(testString.getBytes());
		ImportFileDocument imp=ImportFileDocument.Factory.newInstance();
		imp.addNewImportFile().setProtocol(ProtocolType.BFT);
		imp.getImportFile().setDestination("/partial_testfile");

		ImportFileResponseDocument res2=sms.ImportFile(imp);
		HttpFileTransferClient http2=new HttpFileTransferClient(
				res2.getImportFileResponse().getImportEPR(),
				kernel.getClientConfiguration());
		http2.writeAllData(bis);
		http2.close();
		//export it
		HttpFileTransferClient ht = (HttpFileTransferClient)sms.getExport("/partial_testfile", ProtocolType.BFT);
		String url=ht.getAccessURL();
		HttpGet get=new HttpGet(url);
		get.addHeader("Range", "bytes=5-6");
		HttpClient client=HttpUtils.createClient(url, ht.getSecurityConfiguration());
		HttpResponse res=client.execute(get);
		int result=res.getStatusLine().getStatusCode();
		//check for 200 response
		if(result<200 || result >299 ){
			throw new IOException("Can't read remote data, server returned "+res.getStatusLine());
		}
		InputStream is=res.getEntity().getContent();
		ByteArrayOutputStream bos=new ByteArrayOutputStream();
		IOUtils.copy(is, bos);
		assertEquals("is",bos.toString());

		get=new HttpGet(url);
		get.addHeader("Range", "bytes=0-3");
		client=HttpUtils.createClient(url, ht.getSecurityConfiguration());
		res=client.execute(get);
		result=res.getStatusLine().getStatusCode();
		//check for 200 response
		if(result<200 || result >299 ){
			throw new IOException("Can't read remote data, server returned "+res.getStatusLine());
		}
		
		is=res.getEntity().getContent();
		bos=new ByteArrayOutputStream();
		IOUtils.copy(is, bos);
		assertEquals("this",bos.toString());
	}

}
