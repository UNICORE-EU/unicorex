package de.fzj.unicore.xnjs.tsi.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.InetAddress;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ExecutionContext;
import de.fzj.unicore.xnjs.ems.processors.AsyncCommandProcessor.SubCommand;
import de.fzj.unicore.xnjs.util.AsyncCommandHelper;
import de.fzj.unicore.xnjs.util.UFTPUtils;
import eu.unicore.security.Client;
import eu.unicore.security.Xlogin;
import eu.unicore.uftp.dpc.UFTPConstants;
import eu.unicore.uftp.dpc.Utils;
import eu.unicore.uftp.server.requests.UFTPTransferRequest;

public class TestUFTP extends RemoteTSITestCase {

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

	@Test
	public void testUftpGet() throws Exception {
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
		job.sendTo(localhost, uftpd.jobPort);
		ExecutionContext ec = new ExecutionContext();
		ec.setWorkingDirectory(targetDir.getAbsolutePath());
		String command = TSIUtils.makeUFTPGetFileCommand("localhost", uftpd.srvPort, secret,
				"test.dat", "downloaded.test.dat", targetDir.getAbsolutePath(), 0, -1, false, ec);
		runCommand(command, targetDir.getAbsolutePath());
		assertEquals(Utils.md5(source1), Utils.md5(new File(targetDir, "downloaded.test.dat")));
	}

	@Test
	public void testUftpGetWithRestart() throws Exception {
		InetAddress localhost = InetAddress.getByName("localhost");
		File sourceDir = mkTmpDir();
		File source1 = new File(sourceDir, "test.dat");
		FileUtils.writeStringToFile(source1, "this is some test data\n", "UTF-8");
		long offset = 13;
		long length = -1;
		File targetDir = mkTmpDir();
		String secret = String.valueOf(System.currentTimeMillis());
		UFTPTransferRequest job = new UFTPTransferRequest(
				new InetAddress[] {localhost}, 
				"nobody", 
				secret, new File(sourceDir, UFTPConstants.sessionModeTag), true);
		job.sendTo(localhost, uftpd.jobPort);
		ExecutionContext ec = new ExecutionContext();
		ec.setWorkingDirectory(targetDir.getAbsolutePath());
		String command = TSIUtils.makeUFTPGetFileCommand("localhost", uftpd.srvPort, secret,
				"test.dat", "downloaded.test.dat", targetDir.getAbsolutePath(), offset, length, false, ec);
		runCommand(command, targetDir.getAbsolutePath());
		assertEquals(Utils.md5("test data\n".getBytes("UTF-8")), Utils.md5(new File(targetDir, "downloaded.test.dat")));
	}

	@Test
	public void testUftpGetInTwoParts() throws Exception {
		InetAddress localhost = InetAddress.getByName("localhost");
		File sourceDir = mkTmpDir();
		File source1 = new File(sourceDir, "test.dat");
		FileUtils.writeStringToFile(source1, "this is some test data\n", "UTF-8");
		File targetDir = mkTmpDir();

		long offset = 0;
		long length = 13;
		String secret = String.valueOf(System.currentTimeMillis());
		UFTPTransferRequest job = new UFTPTransferRequest(
				new InetAddress[] {localhost}, 
				"nobody", 
				secret, new File(sourceDir, UFTPConstants.sessionModeTag), true);
		job.sendTo(localhost, uftpd.jobPort);
		ExecutionContext ec = new ExecutionContext();
		ec.setWorkingDirectory(targetDir.getAbsolutePath());
		String command = TSIUtils.makeUFTPGetFileCommand("localhost", uftpd.srvPort, secret,
				"test.dat", "downloaded.test.dat", targetDir.getAbsolutePath(), offset, length, true, ec);
		runCommand(command, targetDir.getAbsolutePath());
		File downloadedFile = new File(targetDir, "downloaded.test.dat");
		//check that only first part was transferred
		assertFileLength(downloadedFile, 13);

		// part 2
		offset = 13;
		length = -1;
		secret = String.valueOf(System.currentTimeMillis());
		job = new UFTPTransferRequest(
				new InetAddress[] {localhost}, 
				"nobody", 
				secret, new File(sourceDir, UFTPConstants.sessionModeTag), true);
		job.sendTo(localhost, uftpd.jobPort);
		ec = new ExecutionContext();
		ec.setWorkingDirectory(targetDir.getAbsolutePath());
		command = TSIUtils.makeUFTPGetFileCommand("localhost", uftpd.srvPort, secret,
				"test.dat", "downloaded.test.dat", targetDir.getAbsolutePath(), offset, length, true, ec);
		runCommand(command, targetDir.getAbsolutePath());

		assertEquals(Utils.md5(source1), Utils.md5(new File(targetDir, "downloaded.test.dat")));
	}

	@Test
	public void testUftpPut() throws Exception {
		InetAddress localhost = InetAddress.getByName("localhost");
		File sourceDir = mkTmpDir();
		File source1 = new File(sourceDir, "test.dat");
		FileUtils.writeStringToFile(source1, "this is some test data\n", "UTF-8");
		File targetDir = mkTmpDir();
		String secret = String.valueOf(System.currentTimeMillis());
		UFTPTransferRequest job = new UFTPTransferRequest(
				new InetAddress[] {localhost}, 
				"nobody", 
				secret, new File(targetDir, UFTPConstants.sessionModeTag), true);
		job.sendTo(localhost, uftpd.jobPort);
		ExecutionContext ec = new ExecutionContext();
		ec.setWorkingDirectory(targetDir.getAbsolutePath());
		String command = TSIUtils.makeUFTPPutFileCommand("localhost", uftpd.srvPort, secret,
				"uploaded.test.dat", "test.dat", sourceDir.getAbsolutePath(), 0, -1, false, ec);
		runCommand(command, targetDir.getAbsolutePath());
		assertEquals(Utils.md5(source1), Utils.md5(new File(targetDir, "uploaded.test.dat")));
	}

	@Test
	public void testUftpPutWithRestart() throws Exception {
		InetAddress localhost = InetAddress.getByName("localhost");
		File sourceDir = mkTmpDir();
		File source1 = new File(sourceDir, "test.dat");
		FileUtils.writeStringToFile(source1, "this is some test data\n", "UTF-8");
		long offset = 13;
		long length = -1;

		File targetDir = mkTmpDir();
		String secret = String.valueOf(System.currentTimeMillis());
		UFTPTransferRequest job = new UFTPTransferRequest(
				new InetAddress[] {localhost}, 
				"nobody", 
				secret, new File(targetDir, UFTPConstants.sessionModeTag), true);
		job.sendTo(localhost, uftpd.jobPort);
		ExecutionContext ec = new ExecutionContext();
		ec.setWorkingDirectory(targetDir.getAbsolutePath());
		String command = TSIUtils.makeUFTPPutFileCommand("localhost", uftpd.srvPort, secret,
				"uploaded.test.dat", "test.dat", sourceDir.getAbsolutePath(), offset, length, false, ec);

		runCommand(command, targetDir.getAbsolutePath());
		assertEquals(Utils.md5("test data\n".getBytes("UTF-8")), Utils.md5(new File(targetDir, "uploaded.test.dat")));
	}

	@Test
	public void testUftpPutInTwoParts() throws Exception {
		InetAddress localhost = InetAddress.getByName("localhost");
		File sourceDir = mkTmpDir();
		File source1 = new File(sourceDir, "test.dat");
		FileUtils.writeStringToFile(source1, "this is some test data\n", "UTF-8");
		long offset = 0;
		long length = 13;
		File targetDir = mkTmpDir();
		String secret = String.valueOf(System.currentTimeMillis());
		UFTPTransferRequest job = new UFTPTransferRequest(
				new InetAddress[] {localhost}, 
				"nobody", 
				secret, new File(targetDir, UFTPConstants.sessionModeTag), true);
		job.sendTo(localhost, uftpd.jobPort);
		ExecutionContext ec = new ExecutionContext();
		ec.setWorkingDirectory(targetDir.getAbsolutePath());
		String command = TSIUtils.makeUFTPPutFileCommand("localhost", uftpd.srvPort, secret,
				"uploaded.test.dat", "test.dat", sourceDir.getAbsolutePath(), offset, length, true, ec);
		runCommand(command, targetDir.getAbsolutePath());
		File uploadedFile = new File(targetDir, "uploaded.test.dat");
		assertFileLength(uploadedFile, 13);

		//part 2
		offset = 13;
		length = -1;
		secret = String.valueOf(System.currentTimeMillis());
		job = new UFTPTransferRequest(
				new InetAddress[] {localhost}, 
				"nobody", 
				secret, new File(targetDir, UFTPConstants.sessionModeTag), true);
		job.sendTo(localhost, uftpd.jobPort);
		ec = new ExecutionContext();
		ec.setWorkingDirectory(targetDir.getAbsolutePath());
		command = TSIUtils.makeUFTPPutFileCommand("localhost", uftpd.srvPort, secret,
				"uploaded.test.dat", "test.dat", sourceDir.getAbsolutePath(), offset, length, true, ec);
		runCommand(command, targetDir.getAbsolutePath());

		assertEquals(Utils.md5(source1), Utils.md5(new File(targetDir, "uploaded.test.dat")));
	}

	@Test
	public void testCommandProcessingGET() throws Exception {
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
		job.sendTo(localhost, uftpd.jobPort);

		Action parent = xnjs.makeAction(new JSONObject());
		parent.setClient(new Client());
		parent.getClient().setXlogin(new Xlogin(new String[] {"nobody"}));
		String id = parent.getUUID();
		mgr.add(parent, parent.getClient());
		JSONObject cmd = UFTPUtils.jsonBuilder().
				host("localhost").
				port(uftpd.srvPort).
				secret(secret).
				get().
				from("test.dat").
				to(targetDir.getAbsolutePath()+"/downloaded.test.dat").
				workdir(targetDir.getAbsolutePath()).
				build();
		AsyncCommandHelper ach = new AsyncCommandHelper(xnjs, cmd.toString(), "uftp_1", id, parent.getClient());
		ach.getSubCommand().type = SubCommand.UFTP;
		ach.submit();
		while(!ach.isDone())Thread.sleep(1000);
		assertTrue(ach.getResult().getResult().isSuccessful());
		assertTrue(mgr.getAction(ach.getActionID()).getLog().toString().contains("#TSI_UFTP"));
		assertEquals(Utils.md5(source1), Utils.md5(new File(targetDir, "downloaded.test.dat")));
	}

	@Test
	public void testCommandProcessingPUT() throws Exception {
		InetAddress localhost = InetAddress.getByName("localhost");
		File sourceDir = mkTmpDir();
		File source1 = new File(sourceDir, "test.dat");
		FileUtils.writeStringToFile(source1, "this is some test data\n", "UTF-8");
		File targetDir = mkTmpDir();
		String secret = String.valueOf(System.currentTimeMillis());
		UFTPTransferRequest job = new UFTPTransferRequest(
				new InetAddress[] {localhost}, 
				"nobody", 
				secret, new File(targetDir, UFTPConstants.sessionModeTag), true);
		job.sendTo(localhost, uftpd.jobPort);

		Action parent = xnjs.makeAction(new JSONObject());
		parent.setClient(new Client());
		parent.getClient().setXlogin(new Xlogin(new String[] {"nobody"}));
		String id = parent.getUUID();
		mgr.add(parent, parent.getClient());
		JSONObject cmd = UFTPUtils.jsonBuilder().
				host("localhost").
				port(uftpd.srvPort).
				secret(secret).
				put().
				from(sourceDir.getAbsolutePath()+"/test.dat").
				to("uploaded.test.dat").
				workdir(targetDir.getAbsolutePath()).
				offset(0).
				length(-1).
				partial().
				build();
		AsyncCommandHelper ach = new AsyncCommandHelper(xnjs, cmd.toString(), "uftp_1", id, parent.getClient());
		ach.getSubCommand().type = SubCommand.UFTP;
		ach.submit();
		while(!ach.isDone())Thread.sleep(1000);
		assertTrue(ach.getResult().getResult().isSuccessful());
		assertTrue(mgr.getAction(ach.getActionID()).getLog().toString().contains("#TSI_UFTP"));
		assertEquals(Utils.md5(source1), Utils.md5(new File(targetDir, "uploaded.test.dat")));
	}

	private void runCommand(String command, String outcomeDir) throws Exception {
		RemoteTSI tsi=(RemoteTSI)xnjs.getTargetSystemInterface(null);
		assertNotNull(tsi);
		tsi.runTSICommand(command);
		tsi.setStorageRoot(outcomeDir);
		Thread.sleep(1000);
		while(tsi.getProperties("UNICORE_SCRIPT_EXIT_CODE")==null)Thread.sleep(1000);
		tsi.rm("UNICORE_SCRIPT_EXIT_CODE");
		tsi.rm("UNICORE_SCRIPT_PID");
	}

	private File mkTmpDir(){
		Random r = new Random();
		File f=new File("target","xnjs_test_"+(System.currentTimeMillis()+r.nextInt(20000)));
		if(!f.exists())f.mkdirs();
		return f.getAbsoluteFile();
	}

	private void assertFileLength(File file, long length) throws Exception {
		RemoteTSI tsi=(RemoteTSI)xnjs.getTargetSystemInterface(null);
		assertEquals(length, tsi.getProperties(file.getAbsolutePath()).getSize());
	}
}
