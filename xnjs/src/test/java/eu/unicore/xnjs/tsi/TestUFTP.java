package eu.unicore.xnjs.tsi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import eu.unicore.persist.util.UUID;
import eu.unicore.security.Client;
import eu.unicore.security.Xlogin;
import eu.unicore.uftp.dpc.Utils;
import eu.unicore.uftp.server.requests.UFTPPingRequest;
import eu.unicore.uftp.server.requests.UFTPSessionRequest;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.EMSTestBase;
import eu.unicore.xnjs.fts.IUFTPRunner;
import eu.unicore.xnjs.tsi.remote.UFTPDServerRunner;

public class TestUFTP extends EMSTestBase {

	private static UFTPDServerRunner uftpd = null;
	private static InetAddress localhost;

	@BeforeAll
	public static void startUFTPD() throws Exception {
		localhost = InetAddress.getByName("127.0.0.1");
		// use non-default ports here to avoid "Address already in use"
		uftpd = new UFTPDServerRunner(63435, 63434);
		uftpd.start();
		waitForUFTPD();
	}

	@AfterAll
	public static void stopUFTPD() {
		if(uftpd!=null)try{
			uftpd.stop();
		}catch(Exception e) {}
	}

	private static void waitForUFTPD() throws InterruptedException {
		int i=0;
		while(i<30) {
			Thread.sleep(1000);
			UFTPPingRequest ping = new UFTPPingRequest();
			try{
				ping.sendTo(localhost, uftpd.jobPort);
				return;
			}catch(IOException ioe) {}
			i++;
		}
	}

	@Test
	public void testUFTPRunnerGet() throws Exception {
		File sourceDir = mkTmpDir();
		File source1 = new File(sourceDir, "test.dat");
		FileUtils.writeStringToFile(source1, "this is some test data\n", "UTF-8");
		File targetDir = mkTmpDir();
		String secret = String.valueOf(System.currentTimeMillis());
		UFTPSessionRequest job = new UFTPSessionRequest(new InetAddress[] {localhost},
				"nobody", secret, sourceDir.getAbsolutePath());
		job.sendTo(localhost, uftpd.jobPort);
		Action parent = xnjs.makeAction(new JSONObject());
		parent.setClient(new Client());
		parent.getClient().setXlogin(new Xlogin(new String[] {"nobody"}));
		String id = parent.getUUID();
		mgr.add(parent, parent.getClient());
		waitUntilDone(id);
		IUFTPRunner runner = xnjs.get(IUFTPRunner.class);
		runner.setClient(parent.getClient());
		runner.setParentActionID(id);
		runner.setID("UFTP_GET_"+UUID.newUniqueID());
		runner.get("test.dat", "downloaded.test.dat", targetDir.getAbsolutePath(),
				"127.0.0.1", uftpd.srvPort, secret);
		assertEquals(Utils.md5(source1), Utils.md5(new File(targetDir, "downloaded.test.dat")));
	}

	@Test
	public void testUFTPRunnerPUT() throws Exception {
		File sourceDir = mkTmpDir();
		File source1 = new File(sourceDir, "test.dat");
		FileUtils.writeStringToFile(source1, "this is some test data\n", "UTF-8");
		File targetDir = mkTmpDir();
		String secret = String.valueOf(System.currentTimeMillis());
		UFTPSessionRequest job = new UFTPSessionRequest(
				new InetAddress[] {localhost}, "nobody", secret, targetDir.getAbsolutePath());
		job.sendTo(localhost, uftpd.jobPort);
		Action parent = xnjs.makeAction(new JSONObject());
		parent.setClient(new Client());
		parent.getClient().setXlogin(new Xlogin(new String[] {"nobody"}));
		String id = parent.getUUID();
		mgr.add(parent, parent.getClient());
		waitUntilDone(id);
		IUFTPRunner runner = xnjs.get(IUFTPRunner.class);
		runner.setClient(parent.getClient());
		runner.setParentActionID(id);
		runner.setID("UFTP_PUT_"+UUID.newUniqueID());
		runner.put("test.dat", "uploaded.test.dat", sourceDir.getAbsolutePath(),
				"127.0.0.1", uftpd.srvPort, secret);
		assertEquals(Utils.md5(source1), Utils.md5(new File(targetDir, "uploaded.test.dat")));
	}

	private File mkTmpDir(){
		Random r = new Random();
		File f=new File("target","xnjs_test_"+(System.currentTimeMillis()+r.nextInt(20000)));
		if(!f.exists())f.mkdirs();
		return f.getAbsoluteFile();
	}

}