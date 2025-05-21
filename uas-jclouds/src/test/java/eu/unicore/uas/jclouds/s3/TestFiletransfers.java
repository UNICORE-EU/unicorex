package eu.unicore.uas.jclouds.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import eu.unicore.security.Client;
import eu.unicore.uas.Base;
import eu.unicore.uas.jclouds.CloudFileTransferCreator;
import eu.unicore.uas.xnjs.XNJSFacade;
import eu.unicore.uftp.dpc.Utils;
import eu.unicore.xnjs.fts.FTSTransferInfo;
import eu.unicore.xnjs.fts.IFTSController;
import eu.unicore.xnjs.io.DataStageInInfo;
import eu.unicore.xnjs.io.DataStageOutInfo;
import eu.unicore.xnjs.io.IFileTransfer;
import eu.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.xnjs.io.impl.FileTransferEngine;

public class TestFiletransfers extends Base {

	static byte[] td = "test data".getBytes();
	
	@BeforeAll
	public static void createTestFile() throws Exception{
		try{
			FileUtils.write(new File("target/unicorex-test/test1.txt"), "test data", "UTF-8");
			IStorageAdapter s3 = getInMemoryS3();
			s3.mkdir("/");
			try(OutputStream os = s3.getOutputStream("test.txt", false, td.length)){
				os.write(td);
			}
			Thread.sleep(500);
		}catch(IOException e){throw new RuntimeException(e);}
	}

	@Test
	public void testS3URIs() throws Exception {
		String u = "S3:https://foo/testing/path/to/file";
		assertTrue(CloudFileTransferCreator.isS3(u));
		Map<String,String>s3Params = CloudFileTransferCreator.extractUrlInfo(u);
		System.out.println(s3Params);
		assertEquals("testing", s3Params.get("bucket"));
		assertEquals("https://foo", s3Params.get("endpoint"));
		assertEquals("path/to/file", s3Params.get("file"));
	}

	@Test
	public void testS3Import() throws Exception {
		FileTransferEngine e = XNJSFacade.get(null, kernel).getXNJS().get(FileTransferEngine.class);
		DataStageInInfo in = new DataStageInInfo();
		String url = "S3:https://foo/testing/test.txt";
		in.setSources(new URI[] {new URI(url)});
		in.setFileName("imported.txt");
		Map<String, String>params = new HashMap<>();
		params.put("provider", "transient");
		in.setExtraParameters(params);
		Client c = new Client();
		IFileTransfer ft = e.createFileImport(c, new File("./target/unicorex-test/").getAbsolutePath(), in);
		assertNotNull(ft);
		assertTrue(ft instanceof S3FileImport);
		ft.run();
		System.out.println("FT status: "+ft.getInfo().getStatusMessage());
		File i1 = new File("./target/unicorex-test/imported.txt");
		assertEquals(Utils.md5(i1), Utils.md5(td));
	}
	
	@Test
	public void testS3ImportsController() throws Exception {
		FileTransferEngine e = XNJSFacade.get(null, kernel).getXNJS().get(FileTransferEngine.class);
		DataStageInInfo in = new DataStageInInfo();
		String url = "S3:https://foo/testing/test.txt";
		in.setSources(new URI[] {new URI(url)});
		in.setFileName("imported.txt");
		Map<String, String>params = new HashMap<>();
		params.put("provider", "transient");
		in.setExtraParameters(params);
		Client c = new Client();
		IFTSController fts = e.createFTSImport(c, "./target/unicorex-test/", in);
		assertNotNull(fts);
		assertTrue(fts instanceof S3ImportsController);
		List<FTSTransferInfo> fileList = new ArrayList<>();
		fts.collectFilesForTransfer(fileList);
		assertEquals(1, fileList.size());
		IFileTransfer ft = fts.createTransfer(fileList.get(0).getSource(), "imported.txt");
		assertNotNull(ft);
		assertTrue(ft instanceof S3FileImport);
		ft.run();
		System.out.println("FT status: "+ft.getInfo().getStatusMessage());
		File i1 = new File("./target/unicorex-test/imported.txt");
		assertEquals(Utils.md5(i1), Utils.md5(td));
	}

	@Test
	public void testS3Export() throws Exception {
		FileTransferEngine e = XNJSFacade.get(null, kernel).getXNJS().get(FileTransferEngine.class);
		DataStageOutInfo out = new DataStageOutInfo();
		String url = "S3:https://foo/testing/exported.txt";
		out.setTarget(new URI(url));
		out.setFileName("test1.txt");
		Map<String, String>params = new HashMap<>();
		params.put("provider", "transient");
		out.setExtraParameters(params);
		Client c = new Client();
		IFileTransfer ft = e.createFileExport(c, new File("./target/unicorex-test/").getAbsolutePath(), out);
		assertNotNull(ft);
		assertTrue(ft instanceof S3FileExport);
		ft.run();
		System.out.println("FT status: "+ft.getInfo().getStatusMessage());
		Thread.sleep(500);
		byte[] exported = getInMemoryS3().getInputStream("exported.txt").readAllBytes();
		assertEquals(Utils.md5(exported), Utils.md5(td));
	}

	@Test
	public void testS3ExportsController() throws Exception {
		FileTransferEngine e = XNJSFacade.get(null, kernel).getXNJS().get(FileTransferEngine.class);
		DataStageOutInfo out = new DataStageOutInfo();
		String url = "S3:https://foo/testing/exported.txt";
		out.setTarget(new URI(url));
		out.setFileName("test1.txt");
		Map<String, String>params = new HashMap<>();
		params.put("provider", "transient");
		out.setExtraParameters(params);
		Client c = new Client();
		IFTSController fts = e.createFTSExport(c, new File("./target/unicorex-test/").getAbsolutePath(), out);
		assertNotNull(fts);
		assertTrue(fts instanceof S3ExportsController);
		List<FTSTransferInfo> fileList = new ArrayList<>();
		fts.collectFilesForTransfer(fileList);
		assertEquals(1, fileList.size());
		IFileTransfer ft = fts.createTransfer(fileList.get(0).getSource(), "exported.txt");
		assertNotNull(ft);
		assertTrue(ft instanceof S3FileExport);
		ft.run();
		System.out.println("FT status: "+ft.getInfo().getStatusMessage());
		Thread.sleep(500);
		byte[] exported = getInMemoryS3().getInputStream("exported.txt").readAllBytes();
		assertEquals(Utils.md5(exported), Utils.md5(td));
	}

	private static IStorageAdapter getInMemoryS3() throws Exception {
		String access = "none";
		String secret = "none";
		String endpoint = "none";
		String provider = "transient";
		String bucket = "testing";
		return new S3StorageAdapterFactory().createStorageAdapter(kernel,access,secret,endpoint,provider,bucket,null,false);
	}
}
