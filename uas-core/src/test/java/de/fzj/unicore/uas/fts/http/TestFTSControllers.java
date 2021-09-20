package de.fzj.unicore.uas.fts.http;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import de.fzj.unicore.uas.Base;
import de.fzj.unicore.uas.fts.ExportsController;
import de.fzj.unicore.uas.fts.ImportsController;
import de.fzj.unicore.uas.util.Pair;
import de.fzj.unicore.uas.xnjs.XNJSFacade;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.fts.FTSTransferInfo;
import de.fzj.unicore.xnjs.io.DataStageInInfo;
import de.fzj.unicore.xnjs.io.DataStageOutInfo;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.core.StorageFactoryClient;
import eu.unicore.security.Client;
import eu.unicore.security.SecurityTokens;
import eu.unicore.services.rest.client.UsernamePassword;
/**
 * tests sendFile() and receiveFile()
 * 
 * @author schuller
 */
public class TestFTSControllers extends Base {

	StorageFactoryClient sfc;
	StorageClient source, target;
	String sourceURL,targetURL;

	@Test
	public void testCollectImportFileList()throws Exception{
		String url = kernel.getContainerProperties().getContainerURL()+"/rest";
		Endpoint sfcEndpoint = new Endpoint(url+"/core/storagefactories/default_storage_factory");
		sfc = new StorageFactoryClient(sfcEndpoint, kernel.getClientConfiguration(),
				new UsernamePassword("demouser", "test123"));
		Pair<Integer, Long> numberOfFiles = initSource();
		XNJS xnjs = XNJSFacade.get(null, kernel).getXNJS();
		Client client = new Client();
		SecurityTokens t = new SecurityTokens();
		t.setUserName("CN=Demo User, O=UNICORE, C=EU");
		t.setConsignorTrusted(true);
		client.setAuthenticatedClient(t);
		File wd = new File("target/test_imports");
		DataStageInInfo dsi = new DataStageInInfo();
		String sources = source.getEndpoint().getUrl()+"/files/";
		dsi.setSources(new URI[] {new URI(sources)});
		dsi.setFileName("/");
		ImportsController bft = new ImportsController(xnjs, client, source.getEndpoint(), 
				dsi, 
				wd.getAbsolutePath());
		List<FTSTransferInfo> fileList = new ArrayList<>();
		long size = bft.collectFilesForTransfer(fileList);
		assertEquals(numberOfFiles.getM1(), (Integer)fileList.size());
		assertEquals(numberOfFiles.getM2(), (Long)size);
	}

	
	@Test
	public void testCollectExportFileList()throws Exception{
		String url = kernel.getContainerProperties().getContainerURL()+"/rest";
		Endpoint sfcEndpoint = new Endpoint(url+"/core/storagefactories/default_storage_factory");
		sfc = new StorageFactoryClient(sfcEndpoint, kernel.getClientConfiguration(),
				new UsernamePassword("demouser", "test123"));
		Pair<Integer, Long> numberOfFiles = initSource();
		target = sfc.createStorage();
		XNJS xnjs = XNJSFacade.get(null, kernel).getXNJS();
		Client client = new Client();
		SecurityTokens t = new SecurityTokens();
		t.setUserName("CN=Demo User, O=UNICORE, C=EU");
		t.setConsignorTrusted(true);
		client.setAuthenticatedClient(t);
		DataStageOutInfo dso = new DataStageOutInfo();
		String workingDirectory = source.getMountPoint();
		dso.setTarget(new URI(target.getEndpoint().getUrl()+"/files/"));
		dso.setFileName("/");
		ExportsController bft = new ExportsController(xnjs, client, source.getEndpoint(), 
				dso, 
				workingDirectory);
		List<FTSTransferInfo> fileList = new ArrayList<>();
		long size = bft.collectFilesForTransfer(fileList);
		assertEquals(numberOfFiles.getM1(), (Integer)fileList.size());
		assertEquals(numberOfFiles.getM2(), (Long)size);
	}
	
	protected Pair<Integer, Long> initSource() throws Exception {
		source = sfc.createStorage();
		source.mkdir("folder1/folder11");
		source.mkdir("folder2");
		source.upload("folder1/test11").write("test11".getBytes());
		source.upload("folder1/test12").write("test12".getBytes());
		source.upload("folder1/zeros").write("".getBytes());
		source.upload("folder1/folder11/test111").write("test111".getBytes());
		source.upload("folder2/test21").write("test12".getBytes());
		source.upload("test.txt").write("this is a test".getBytes());
		source.upload("test1.txt").write("this is a test".getBytes());
		sourceURL = source.getEndpoint().getUrl();
		return new Pair<Integer, Long>(7, 3*6l + 7 + 2*14);
	}

}
