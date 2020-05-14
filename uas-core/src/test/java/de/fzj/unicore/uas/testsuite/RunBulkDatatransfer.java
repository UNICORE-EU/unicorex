package de.fzj.unicore.uas.testsuite;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.unigrids.services.atomic.types.ProtocolType;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.UASProperties;
import de.fzj.unicore.uas.client.FileTransferClient;
import de.fzj.unicore.uas.client.HttpFileTransferClient;
import de.fzj.unicore.uas.client.JobClient;
import de.fzj.unicore.uas.client.StorageClient;
import de.fzj.unicore.wsrflite.utils.StopWatch;
import eu.unicore.bugsreporter.annotation.FunctionalTest;

/**
 * runs some data transfer tests
 * @author schuller
 */
public class RunBulkDatatransfer extends AbstractJobRun{

	String url;
	EndpointReferenceType tssepr;


	@Override
	@FunctionalTest(id="BulkHTTPTest", description="Tests the HTTP file transfer using large files")
	public void testRunJob()throws Exception{
		//POST should be preferred for data upload
		UASProperties cfg = kernel.getAttribute(UASProperties.class);
		cfg.setProperty(UASProperties.FTS_HTTP_PREFER_POST, "true");
		JobClient job=submitJob(tss);
		StorageClient sms=job.getUspaceClient();

		//data stage in with perf measurement
		HttpFileTransferClient importClient=(HttpFileTransferClient)sms.getImport("bigfile", true, null, ProtocolType.BFT);
		System.out.println("Importing to "+importClient.getAccessURL());
		int size=128000;
		int loops=5*600;
		File file=new File("target","bigfile");
		file.deleteOnExit();
		writeTestData(file, size, loops);
		long checkIn=FileUtils.checksumCRC32(file);
		StopWatch sw=new StopWatch();
		sw.start("Start import of "+size*loops/(1024*1024)+ " MBytes");
		System.out.println("Start import of "+size*loops/(1024*1024)+ " MBytes");
		importClient.writeAllData(new FileInputStream(file));
		sw.stop("End.");
		long time=sw.getCurrentTotal();
		System.out.println("End of data import, time="+time/1000+ " sec");
		System.out.println("Rate="+(size*loops)/time+ " kB/sec.");

		boolean doExport=false;

		if(doExport){
			//data export
			FileTransferClient exportClient=sms.getExport("bigfile", ProtocolType.BFT);
			sw.start("Start export of "+size*loops/(1024*1024)+ " MBytes");
			System.out.println("Start export of "+size*loops/(1024*1024)+ " MBytes");
			File out=new File("target","bigfile_out");
			out.deleteOnExit();
			FileOutputStream fos=new FileOutputStream(out);
			exportClient.readAllData(fos);
			fos.close();
			sw.stop("End.");
			time=sw.getCurrentTotal();
			System.out.println("End of data export, time="+time/1000+ " sec");
			System.out.println("Rate="+(size*loops)/time+ " kB/sec.");

			long checkOut=FileUtils.checksumCRC32(out);
			assertTrue(checkOut==checkIn);
		}
	}

	private void writeTestData(File file, int size, int loops)throws IOException{
		byte[] data=new byte[size];
		Random r=new Random();
		FileOutputStream fos=new FileOutputStream(file);
		for (int i = 0; i < loops; i++) {
			r.nextBytes(data);
			IOUtils.copy(new ByteArrayInputStream(data), fos);
		}
		fos.close();
	}

}
