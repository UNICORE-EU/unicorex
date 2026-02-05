package eu.unicore.xnjs.io.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import eu.unicore.xnjs.ems.EMSTestBase;
import eu.unicore.xnjs.io.DataStageInInfo;
import eu.unicore.xnjs.io.IFileTransfer;
import eu.unicore.xnjs.io.TransferInfo;
import eu.unicore.xnjs.io.TransferInfo.Status;
import eu.unicore.xnjs.io.impl.FileTransferEngine;

public class TestGitStageIn extends EMSTestBase {
	
	@Test
	public void testStageIn1(){
		try{
			File wd = new File("target/git-test");
			FileUtils.deleteQuietly(wd);
			wd.mkdir();
			URI source=new URI("git:https://github.com/github/testrepo.git");
			DataStageInInfo info = new DataStageInInfo();
			info.setSources(new URI[]{source});
			info.setFileName("testrepo");
			Map<String,String>params = new HashMap<>();
			params.put("commit", "26fc7091");
			info.setExtraParameters(params);
			IFileTransfer ft = new FileTransferEngine(xnjs).createFileImport(createClient(),
					wd.getAbsolutePath(), info);
			assertNotNull(ft);
			assertTrue(ft instanceof GitStageIn);
			ft.run();
			TransferInfo fti = ft.getInfo();
			assertEquals(Status.DONE,fti.getStatus());
			long transferred=fti.getTransferredBytes();
			System.out.println("Downloaded "+transferred);
			assertTrue(transferred>0);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	@Test
	public void testStageInSubrepos(){
		try{
			File wd = new File("target/git-test");
			FileUtils.deleteQuietly(wd);
			wd.mkdir();
			URI source=new URI("git:https://github.com/BerndSchuller/testrepo2.git");
			DataStageInInfo info = new DataStageInInfo();
			info.setSources(new URI[]{source});
			info.setFileName("testrepo2");
			IFileTransfer ft = new FileTransferEngine(xnjs).createFileImport(createClient(),
					wd.getAbsolutePath(), info);
			assertNotNull(ft);
			assertTrue(ft instanceof GitStageIn);
			ft.run();
			TransferInfo fti = ft.getInfo();
			assertEquals(Status.DONE,fti.getStatus());
			long transferred=fti.getTransferredBytes();
			System.out.println("Downloaded "+transferred);
			assertTrue(transferred>0);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}