package de.fzj.unicore.xnjs.io.git;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import de.fzj.unicore.xnjs.ems.EMSTestBase;
import de.fzj.unicore.xnjs.io.DataStageInInfo;
import de.fzj.unicore.xnjs.io.IFileTransfer;
import de.fzj.unicore.xnjs.io.TransferInfo;
import de.fzj.unicore.xnjs.io.TransferInfo.Status;
import de.fzj.unicore.xnjs.io.impl.FileTransferEngine;

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
			IFileTransfer ft = new FileTransferEngine(xnjs).createFileImport(null, wd.getAbsolutePath(), info);
			assertNotNull(ft);
			assertTrue(ft instanceof GitStageIn);
			ft.run();
			TransferInfo fti = ft.getInfo();
			assertEquals(fti.getStatusMessage(),Status.DONE,fti.getStatus());
			long transferred=fti.getTransferredBytes();
			System.out.println("Downloaded "+transferred);
			assertTrue(transferred>0);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}