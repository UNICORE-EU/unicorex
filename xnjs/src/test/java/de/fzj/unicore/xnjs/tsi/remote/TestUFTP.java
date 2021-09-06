package de.fzj.unicore.xnjs.tsi.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.net.InetAddress;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import eu.unicore.uftp.dpc.UFTPConstants;
import eu.unicore.uftp.dpc.Utils;
import eu.unicore.uftp.server.requests.UFTPTransferRequest;

/**
 * These tests will work only when invoked on a system with a FS having ACL enabled.
 * @author K. Benedyczak
 */
public class TestUFTP extends LegacyTSITestCase {

	private static UFTPDServerRunner uftpd = null;
	
	@BeforeClass
	public static void startUFTPD() throws Exception {
		uftpd = new UFTPDServerRunner();
		uftpd.start();
	}
	

	@AfterClass
	public static void stopUFTPD() {
		if(uftpd!=null)try{
			uftpd.stop();
		}catch(Exception e) {}
	}
	
	private File mkTmpDir(){
		Random r = new Random();
		File f=new File("target","xnjs_test_"+(System.currentTimeMillis()+r.nextInt(20000)));
		if(!f.exists())f.mkdirs();
		return f.getAbsoluteFile();
	}

	
	@Test
	public void testUftpGet() throws Exception {
		RemoteTSI tsi=(RemoteTSI)xnjs.getTargetSystemInterface(null);
		assertNotNull(tsi);
		InetAddress localhost = InetAddress.getByName("localhost");
		File sourceDir = mkTmpDir();
		File source1 = new File(sourceDir, "test.dat");
		FileUtils.writeStringToFile(source1, "this is some test data\n", "UTF-8");
		
		File targetDir = mkTmpDir();
		String secret = String.valueOf(System.currentTimeMillis());
		UFTPTransferRequest job = new UFTPTransferRequest(
				new InetAddress[] {localhost}, 
				"nobody", 
				secret, new File(sourceDir, UFTPConstants.sessionModeTag), true);
		String reply = job.sendTo(localhost, uftpd.jobPort);
		System.out.println("Sent job, UFTPD reply: "+reply);
		
		String command = TSIUtils.makeUFTPGetFileCommand("localhost", uftpd.srvPort, secret,
				"test.dat", "downloaded.test.dat", targetDir.getAbsolutePath(), 0, -1);
		
		reply = tsi.runTSICommand(command);
		System.out.println("TSI reply: "+reply);
		
		tsi.setStorageRoot(targetDir.getAbsolutePath());
		Thread.sleep(2000);
		while(tsi.getProperties("UNICORE_SCRIPT_EXIT_CODE")==null) {
			Thread.sleep(1000);
		}
		assertEquals(Utils.md5(source1), Utils.md5(new File(targetDir, "downloaded.test.dat")));
	}
	
	@Test
	public void testUftpPut() throws Exception {
		RemoteTSI tsi=(RemoteTSI)xnjs.getTargetSystemInterface(null);
		assertNotNull(tsi);
		InetAddress localhost = InetAddress.getByName("localhost");
		File sourceDir = mkTmpDir();
		File source1 = new File(sourceDir, "test.dat");
		FileUtils.writeStringToFile(source1, "this is some test data", "UTF-8");
		
		File targetDir = mkTmpDir();
		String secret = String.valueOf(System.currentTimeMillis());
		UFTPTransferRequest job = new UFTPTransferRequest(
				new InetAddress[] {localhost}, 
				"nobody", 
				secret, new File(targetDir, UFTPConstants.sessionModeTag), true);
		String reply = job.sendTo(localhost, uftpd.jobPort);
		System.out.println("Sent job, UFTPD reply: "+reply);
		
		String command = TSIUtils.makeUFTPPutFileCommand("localhost", uftpd.srvPort, secret,
				"uploaded.test.dat", "test.dat", sourceDir.getAbsolutePath(), 0, -1);
		
		reply = tsi.runTSICommand(command);
		System.out.println("TSI reply: "+reply);
		
		tsi.setStorageRoot(sourceDir.getAbsolutePath());
		Thread.sleep(2000);
		while(tsi.getProperties("UNICORE_SCRIPT_EXIT_CODE")==null) {
			Thread.sleep(1000);
		}
		assertEquals(Utils.md5(source1), Utils.md5(new File(targetDir, "uploaded.test.dat")));
	}
	
}
