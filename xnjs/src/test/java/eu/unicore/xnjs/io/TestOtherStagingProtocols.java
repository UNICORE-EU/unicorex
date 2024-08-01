package eu.unicore.xnjs.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URI;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;

import eu.unicore.security.Client;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.Xlogin;
import eu.unicore.xnjs.ems.EMSTestBase;
import eu.unicore.xnjs.io.IFileTransfer.OverwritePolicy;
import eu.unicore.xnjs.io.TransferInfo.Status;
import eu.unicore.xnjs.io.impl.DefaultTransferCreator;
import eu.unicore.xnjs.io.impl.FTPDownload;
import eu.unicore.xnjs.io.impl.FTPUpload;
import eu.unicore.xnjs.io.impl.FileTransferEngine;
import eu.unicore.xnjs.io.impl.ScpDownload;
import eu.unicore.xnjs.io.impl.ScpUpload;
import eu.unicore.xnjs.io.impl.UsernamePassword;
import eu.unicore.xnjs.json.JSONParser;
import eu.unicore.xnjs.util.IOUtils;
import eu.unicore.xnjs.util.ResultHolder;

public class TestOtherStagingProtocols extends EMSTestBase {

	static final int wait_time=1000;

	private FakeFtpServer ftpServer;
	private File testDir;

	@BeforeEach
	public void startHelperServers()throws Exception{
		setupFTPServer();
		testDir=new File("target","xnjs_test"+System.currentTimeMillis());
		testDir.mkdirs();
	}

	private void setupFTPServer(){
		//if(ftpServer!=null)ftpServer.stop();
		ftpServer = new FakeFtpServer();
		ftpServer.setServerControlPort(0);
		UserAccount anonymous=new UserAccount("anonymous", null, "/");
		anonymous.setPasswordRequiredForLogin(false);
		anonymous.setPasswordCheckedDuringValidation(false);
		anonymous.setAccountRequiredForLogin(false);
		ftpServer.addUserAccount(anonymous);
		ftpServer.addUserAccount(new UserAccount("user", "password", "/"));
		FileSystem fileSystem = new UnixFakeFileSystem();
		fileSystem.add(new DirectoryEntry("/data"));
		FileEntry fe=new FileEntry("/data/infile");
		fe.setContents("whatever");
		fileSystem.add(fe);
		ftpServer.setFileSystem(fileSystem);
		ftpServer.start();
	}

	@AfterEach
	public void stopServers()throws Exception{
		ftpServer.stop();
	}

	@Test
	public void testSCPDownload()throws Exception{
		xnjs.setProperty("XNJS.localtsi.useShell", "false");
		String dummyParent=createDummyParent();
		String wd=getWorkingDir(dummyParent);
		Client client=createClient();
		xnjs.getIOProperties().setProperty(IOProperties.SCP_WRAPPER, "/bin/echo");
		UsernamePassword creds=new UsernamePassword("test", "test123");
		ScpDownload d=new ScpDownload(client,wd,new URI("scp://host/some_file"),"test",xnjs,creds);
		d.getInfo().setParentActionID(dummyParent);
		String cmd=d.makeCommandline();
		assertEquals("/bin/echo 'test@host:/some_file' '"+wd+"/test' 'test123'",cmd);
		assertEquals(Status.CREATED, d.getInfo().getStatus());

		d.run();
		assertEquals(Integer.valueOf(0), d.getResult().getExitCode());
		assertEquals(Status.DONE,d.getInfo().getStatus());
	}

	@Test
	public void testSCPUpload()throws Exception{
		xnjs.setProperty("XNJS.localtsi.useShell", "false");
		String dummyParent=createDummyParent();
		String wd=getWorkingDir(dummyParent);
		Client client=createClient();
		xnjs.getIOProperties().setProperty(IOProperties.SCP_WRAPPER, "/bin/echo");
		UsernamePassword creds=new UsernamePassword("test", "test123");
		ScpUpload d=new ScpUpload(client,wd,"test",new URI("scp://host/some_file"),xnjs,creds);
		d.getInfo().setParentActionID(dummyParent);
		String cmd=d.makeCommandline();
		assertEquals("/bin/echo '"+wd+"/test' 'test@host:/some_file' 'test123'",cmd);
		assertEquals(Status.CREATED, d.getInfo().getStatus());
		d.run();
		assertEquals(Integer.valueOf(0), d.getResult().getExitCode());
		assertEquals(Status.DONE,d.getInfo().getStatus());
	}

	@Test
	public void testFTPCredentialExtract()throws Exception{
		JSONObject j = loadJSONObject("src/test/resources/json/staging_credentials.json");
		URL source=new URL("ftp://zam935:21/sampledata/fzj_unicore.txt");
		JSONObject jCredentials = j.getJSONArray("Imports").getJSONObject(0).getJSONObject("Credentials");
		UsernamePassword cred  = (UsernamePassword)JSONParser.extractCredentials(jCredentials);
		assertNotNull(cred);
		URL urlWithCreds=IOUtils.addFTPCredentials(source, cred.getUser(), cred.getPassword());
		assertTrue(urlWithCreds.toString().contains("interop:IshQ@zam935"));
	}

	@Test
	public void testPublicFTPDownload(){
		try{
			File localFile=File.createTempFile("xnjs", "ftp");
			localFile.deleteOnExit();
			xnjs.getIOProperties().setProperty(IOProperties.CURL, null);
			URI source=new URI("ftp://localhost:"+ftpServer.getServerControlPort()+"/data/infile");

			DataStageInInfo info = new DataStageInInfo();
			info.setSources(new URI[]{source});
			info.setFileName(localFile.getAbsolutePath());
			info.setOverwritePolicy(OverwritePolicy.OVERWRITE);

			IFileTransfer ft = new FileTransferEngine(xnjs).createFileImport(null, "/", info);
			assertNotNull(ft);
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
	public void testPublicFTPDownloadCurl(){
		try{
			File localFile=File.createTempFile("xnjs", "ftp");
			localFile.deleteOnExit();
			//check if we have curl on the path
			File curl=new File("/usr/bin/curl");
			if(!curl.exists() || !curl.canExecute()){
				System.out.println("No 'curl' found, skipping test");
				return;
			}
			xnjs.getIOProperties().setProperty(IOProperties.CURL, "/usr/bin/curl");
			URI source=new URI("ftp://sunsite.informatik.rwth-aachen.de/README");

			DataStageInInfo info = new DataStageInInfo();
			info.setSources(new URI[]{source});
			info.setFileName(localFile.getAbsolutePath());
			info.setOverwritePolicy(OverwritePolicy.OVERWRITE);

			IFileTransfer ft=new FileTransferEngine(xnjs).
					createFileImport(null, "/", info);
			assertNotNull(ft);
			ft.run();
			TransferInfo fti = ft.getInfo();
			//check result
			ResultHolder r=((FTPDownload)ft).getResult();
			Integer exit=r.getExitCode();
			if(Integer.valueOf(0).equals(exit)){
				long transferred=fti.getTransferredBytes();
				System.out.println("Downloaded "+transferred);
				assertTrue(transferred>0);
			}
			else{
				System.out.println("Error occurred running FTP: "+r.getErrorMessage());
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	@Test
	public void testFTPUpload() throws Exception {

		File localFile=new File("target", "ftpupload-"+System.currentTimeMillis());
		localFile.deleteOnExit();
		FileUtils.writeStringToFile(localFile, "testdata", "UTF-8");
		xnjs.getIOProperties().setProperty(IOProperties.CURL, null);
		URI t=new URI("ftp://localhost:"+ftpServer.getServerControlPort()+"/data/outfile");

		DataStageOutInfo info = new DataStageOutInfo();
		info.setTarget(t);
		info.setFileName(localFile.getName());

		FTPUpload ft=(FTPUpload)new FileTransferEngine(xnjs).
				createFileExport(null, "target", info);
		assertNotNull(ft);
		ft.run();
		TransferInfo fti = ft.getInfo();
		assertEquals(Status.DONE,fti.getStatus());
	}

	@Test
	public void testFileTransferCreator()throws Exception{
		DefaultTransferCreator fc=new DefaultTransferCreator(xnjs);

		Client client=createClient();

		// exports
		String[]protocols=fc.getStageOutProtocol().split(",");
		for(String p: protocols){
			String protocol=p.trim();
			URI target=new URI(protocol+"://foo");

			DataStageOutInfo info = new DataStageOutInfo();
			info.setTarget(target);
			info.setFileName("xx");

			IFileTransfer tr=fc.createFileExport(client, ".",info);
			assertNotNull(tr);
			assertEquals(protocol,tr.getInfo().getProtocol());
		}

		// imports
		protocols=fc.getStageInProtocol().split(",");
		for(String p: protocols){
			String protocol=p.trim();
			URI source=new URI(protocol+"://foo");
			DataStageInInfo info = new DataStageInInfo();
			info.setSources(new URI[]{source});
			info.setFileName("xx");
			IFileTransfer tr=fc.createFileImport(client, ".",info);
			assertNotNull(tr);
			assertEquals(protocol,tr.getInfo().getProtocol());
		}

	}

	@Test
	public void testInline() throws Exception {
		
		File localFile = new File(testDir, "xnjs/inline-test");
		localFile.deleteOnExit();

		URI source=new URI("inline://foo");

		DataStageInInfo info = new DataStageInInfo();
		info.setSources(new URI[]{source});
		info.setFileName("xnjs/inline-test");
		info.setOverwritePolicy(OverwritePolicy.OVERWRITE);
		String testdata = "this is some test data";
		info.setInlineData(testdata);

		IFileTransfer ft=new FileTransferEngine(xnjs).
				createFileImport(null, testDir.getAbsolutePath(), info);
		assertNotNull(ft);
		ft.run();
		TransferInfo fti = ft.getInfo();
		long transferred=fti.getTransferredBytes();
		assertTrue(transferred>0);
		assertEquals(Status.DONE,fti.getStatus());
		assertEquals(testdata,FileUtils.readFileToString(localFile, "UTF-8"));
	}

	private String createDummyParent()throws Exception{
		JSONObject jD = new JSONObject("{'ApplicationName': 'Date'}");
		String id = (String)mgr.add(xnjs.makeAction(jD), null);
		waitUntilReady(id);
		return id;
	}

	private String getWorkingDir(String actionID)throws Exception{
		return internalMgr.getAction(actionID).getExecutionContext().getWorkingDirectory();
	}

	private Client createClient(){
		Client c=new Client();
		c.setXlogin(new Xlogin(new String[] {"nobody"}));
		SecurityTokens st=new SecurityTokens();
		st.setUserName("CN=test");
		c.setAuthenticatedClient(st);
		return c;
	}
}
