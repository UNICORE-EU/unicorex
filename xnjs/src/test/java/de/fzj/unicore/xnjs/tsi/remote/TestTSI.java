/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/

package de.fzj.unicore.xnjs.tsi.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.BudgetInfo;
import de.fzj.unicore.xnjs.ems.ExecutionContext;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.idb.IDB;
import de.fzj.unicore.xnjs.idb.IDBImpl;
import de.fzj.unicore.xnjs.io.ChangePermissions;
import de.fzj.unicore.xnjs.io.ChangePermissions.Mode;
import de.fzj.unicore.xnjs.io.ChangePermissions.PermissionsClass;
import de.fzj.unicore.xnjs.io.TransferInfo;
import de.fzj.unicore.xnjs.io.XnjsFile;
import de.fzj.unicore.xnjs.io.XnjsFileWithACL;
import de.fzj.unicore.xnjs.io.XnjsStorageInfo;
import de.fzj.unicore.xnjs.io.impl.Link;
import de.fzj.unicore.xnjs.tsi.AbstractTSITest;
import de.fzj.unicore.xnjs.tsi.IExecution;
import de.fzj.unicore.xnjs.tsi.IReservation;
import de.fzj.unicore.xnjs.tsi.ReservationStatus;
import de.fzj.unicore.xnjs.tsi.ReservationStatus.Status;
import eu.unicore.security.Client;
import eu.unicore.security.Xlogin;

public class TestTSI extends LegacyTSITestCase{

	Random r=new Random();

	private String mkTmpDir(){
		File f=new File("target","xnjs_test_"+(System.currentTimeMillis()+r.nextInt(20000)));
		if(!f.exists())f.mkdirs();
		return f.getAbsolutePath();
	}

	@Test
	public void testBasicSetup()throws Exception{
		DefaultTSIConnectionFactory f = (DefaultTSIConnectionFactory)xnjs.get(TSIConnectionFactory.class);
		assertNotNull(f);
		try(TSIConnection c=f.getTSIConnection("nobody", null, null, -1)){
			System.out.println("TSI "+c.getTSIVersion()+" isAlive="+c.isAlive());
			System.out.println(c);
		}

		try(TSIConnection c=f.getTSIConnection("nobody", null,"localhost",-1)){
			InetAddress localhost=InetAddress.getByName("localhost");
			assertEquals(localhost,c.getTSIAddress());
		}
		int n = f.getNumberOfPooledConnections();
		try(TSIConnection c=f.getTSIConnection("nobody", null,"localhost",-1)){}
		assertEquals(n,f.getNumberOfPooledConnections());

		try{
			f.getTSIConnection("nobody", null, "no-such-host", -1);
			fail("expected exception here");
		}catch(IllegalArgumentException e){
			assertTrue(e.getMessage().contains("No TSI is configured at 'no-such-host'"));
		}

		RemoteTSI tsi=makeTSI();
		assertNotNull(tsi);
		String v=tsi.getFactory().getTSIVersion();
		assertTrue(TSIUtils.compareVersion(v, TSIConnection. RECOMMENDED_TSI_VERSION));
		assertEquals("UNICORE TSI at localhost:65431",tsi.getFileSystemIdentifier());
	}

	@Test
	public void testBasicTSIFunctions()throws Exception{
		String testDir=mkTmpDir();
		RemoteTSI tsi=makeTSI();
		tsi.setStorageRoot(testDir);
		new AbstractTSITest(testDir,tsi).run();
		FileUtils.deleteQuietly(new File(testDir));
	
		
		File tmp=new File("target","XNJS_testing_"+System.currentTimeMillis());
		tmp.mkdir();
		writeFile(tmp.getAbsolutePath()+"/file1", "test");
		tsi.setStorageRoot("/");
		XnjsFile[] ls=tsi.ls(tmp.getAbsolutePath());
		assertTrue("got "+ls.length, ls.length==1);

		writeFile(tmp.getAbsolutePath()+"/file2", "test");
		ls=tsi.ls(tmp.getAbsolutePath());
		assertTrue(ls.length==2);

		ls=tsi.ls(tmp.getAbsolutePath(),0,1,false);
		assertTrue(ls.length==1);
		String p1=ls[0].getPath();
		ls=tsi.ls(tmp.getAbsolutePath(),1,1,false);
		String p2=ls[0].getPath();
		assertTrue(ls.length==1);
		assertTrue(!p1.equals(p2));
		FileUtils.deleteQuietly(tmp);
		
		String test=tsi.getEnvironment("TEST");
		System.out.println(test);
		assertEquals("this is a test for the template mechanism",test);
	}

	@Test
	public void testDF() throws Exception {
		String tmpdir=mkTmpDir();
		File tmp=new File(tmpdir);
		RemoteTSI tsi=makeTSI();
		XnjsStorageInfo info=tsi.doDF(tmp.getAbsolutePath());
		assertNotNull(info);
		assertTrue(info.getTotalSpace()>0);
		System.out.println(info);
		FileUtils.deleteQuietly(tmp);
	}

	@Test
	public void testRead()throws Exception{
		String tmpdir=mkTmpDir();
		RemoteTSI tsi=makeTSI();
		ExecutionContext ec=new ExecutionContext(UUID.randomUUID().toString());
		ec.setWorkingDirectory(tmpdir);
		ec.setStdout("out");
		tsi.exec("echo tsi",ec);
		Thread.sleep(3000);
		tsi.setStorageRoot(tmpdir);
		InputStream is=tsi.getInputStream("out");
		assertNotNull(is);
		try(BufferedReader br=new BufferedReader(new InputStreamReader(is))){
			assertTrue(br.readLine().contains("tsi"));	
		}
		FileUtils.deleteQuietly(new File(tmpdir));
	}

	@Test
	public void testWrite()throws Exception{
		String tmpdir=mkTmpDir();
		RemoteTSI tsi=makeTSI();
		String file="out2";
		tsi.setStorageRoot(tmpdir);
		OutputStream os=tsi.getOutputStream(file);
		assertNotNull(os);
		os.write("tsi!".getBytes());
		//write some more junk
		byte[] foo=new byte[1024*100];
		Arrays.fill(foo,(byte)'a');
		int N=30;
		for(int i=0; i<N; i++){
			os.write(foo);
		}
		os.close();
		Thread.sleep(100);
		FileInputStream is=new FileInputStream(new File(tmpdir,file));
		BufferedReader br=new BufferedReader(new InputStreamReader(is));
		assertTrue(br.readLine().contains("tsi"));
		File f=new File(tmpdir,file);
		assertEquals(f.length(),N*foo.length+4);
		is.close();
		br.close();
		FileUtils.deleteQuietly(new File(tmpdir));
	}

	@Test
	public void testTemplate() throws Exception{
		String tmpdir=System.getProperty("java.io.tmpdir");
		RemoteTSI tsi=makeTSI();
		String tmp="test_"+System.currentTimeMillis()+"";
		tsi.mkdir(tmpdir+File.separator+tmp);
		ExecutionContext ec=new ExecutionContext(UUID.randomUUID().toString());
		ec.setWorkingDirectory(tmpdir+File.separator+tmp);
		//echo the TEST variable defined in the IDB execute template
		ec.setStdout("out");
		tsi.exec("echo $TEST",ec);
		File f=new File(tmpdir+"/"+tmp+"/out");
		assertTrue(f.exists());
		FileInputStream is=new FileInputStream(f);
		try(BufferedReader br=new BufferedReader(new InputStreamReader(is))){
			String line=br.readLine();
			System.out.println(line);
			assertTrue(line.contains("this is a test for the template mechanism"));
		}
		FileUtils.deleteQuietly(new File(tmpdir));
	}

	@Test
	public void testReservationModule() throws Exception {
		IReservation r= xnjs.get(IReservation.class);

		Client c=new Client();
		c.setAuthenticatedClient(null);
		Map<String, String> resources = new HashMap<>();
		Calendar startTime=Calendar.getInstance();
		startTime.add(Calendar.DAY_OF_MONTH,2);
		String id = r.makeReservation(resources, startTime, c);
		r.cancelReservation(id, new Client());
	}

	@Test
	public void testWrongTSICommand()throws Exception{
		String tmpdir=mkTmpDir();
		RemoteTSI tsi=makeTSI();
		ExecutionContext ec=new ExecutionContext(UUID.randomUUID().toString());
		ec.setWorkingDirectory(tmpdir);
		IDB idb = xnjs.get(IDB.class);
		String template = idb.getExecuteTemplate();
		((IDBImpl)idb).setExecuteTemplate("XXXX");
		try{
			tsi.execAndWait("echo tsi",ec);
			fail("Expected exception.");
		}catch(ExecutionException ee){
			//OK
		}
		((IDBImpl)idb).setExecuteTemplate(template);
		FileUtils.deleteQuietly(new File(tmpdir));
	}

	@Test
	public void testChmod2() throws ExecutionException, IOException {
		RemoteTSI tsi=makeTSI();
		assertNotNull(tsi);
		File tst = new File("target" + File.separator + "chmodTestFile.tmp");
		tst.createNewFile();
		ChangePermissions []changePerms = new ChangePermissions[3];
		changePerms[0] = new ChangePermissions(Mode.SET, PermissionsClass.OWNER, "rw-");
		changePerms[1] = new ChangePermissions(Mode.SET, PermissionsClass.GROUP, "---");
		changePerms[2] = new ChangePermissions(Mode.SET, PermissionsClass.OTHER, "---");
		tsi.chmod2(tst.getAbsolutePath(), changePerms, false);
		XnjsFileWithACL f = tsi.getProperties(tst.getAbsolutePath());
		assertEquals("rw-------", f.getUNIXPermissions());

		changePerms[0] = new ChangePermissions(Mode.ADD, PermissionsClass.OWNER, "--x");
		changePerms[1] = new ChangePermissions(Mode.ADD, PermissionsClass.GROUP, "r--");
		changePerms[2] = new ChangePermissions(Mode.ADD, PermissionsClass.OTHER, "--x");
		tsi.chmod2(tst.getAbsolutePath(), changePerms, false);
		f = tsi.getProperties(tst.getAbsolutePath());
		assertEquals("rwxr----x", f.getUNIXPermissions());

		changePerms[0] = new ChangePermissions(Mode.SUBTRACT, PermissionsClass.OWNER, "--x");
		changePerms[1] = new ChangePermissions(Mode.SUBTRACT, PermissionsClass.GROUP, "r--");
		changePerms[2] = new ChangePermissions(Mode.SUBTRACT, PermissionsClass.OTHER, "--x");
		tsi.chmod2(tst.getAbsolutePath(), changePerms, false);
		f = tsi.getProperties(tst.getAbsolutePath());
		assertEquals("rw-------", f.getUNIXPermissions());
		FileUtils.deleteQuietly(tst);
	}

	/**
	 * This test is very limited. It only checks if chgrp works when changing 
	 * to the same group as was already present (as we have no guarantee nor knowledge
	 * of other groups that the current user is member of). So the test is checking
	 * only if operation is executed successfully.
	 * @throws IOException 
	 * @throws ExecutionException 
	 */
	@Test
	public void testChgrp()throws IOException, ExecutionException {
		RemoteTSI tsi=makeTSI();
		assertNotNull(tsi);

		File tst = new File("target" + File.separator + "chgrpTestFile.tmp");
		tst.createNewFile();
		XnjsFileWithACL f = tsi.getProperties(tst.getAbsolutePath());
		String group = f.getGroup();
		tsi.chgrp(tst.getAbsolutePath(), group, false);

		//tsi.chgrp(tst.getAbsolutePath(), "users");
		//XnjsFileWithACL f2 = tsi.getProperties(tst.getAbsolutePath());
		//assertEquals("users", f2.getGroup());
		FileUtils.deleteQuietly(tst);

		String[]groups=tsi.getGroups();
		assertNotNull(groups);
		System.out.println(Arrays.asList(groups));

		tst = new File("target" + File.separator + "lstTestFile.tmp");
		tst.createNewFile();
		f = tsi.getProperties(tst.getAbsolutePath());
		assertNotNull(f.getGroup());
		assertNotNull(f.getOwner());
		assertNotNull(f.getUNIXPermissions());
		FileUtils.deleteQuietly(tst);


		String tmpdir=System.getProperty("java.io.tmpdir");
		String tmp="test_"+System.currentTimeMillis();
		tsi.setUmask("0025");
		String dir = tmpdir+File.separator+tmp;
		tsi.mkdir(dir);
		String dirPerms = tsi.getProperties(dir).getUNIXPermissions();
		assertEquals("rwxr-x-w-", dirPerms);

		tsi.setUmask("11");
		String file=dir+File.separator+"outUmask-1";
		OutputStream os=tsi.getOutputStream(file);
		assertNotNull(os);
		os.write("tsi!".getBytes());
		os.close();
		String filePerms = tsi.getProperties(file).getUNIXPermissions();
		assertEquals("rw-rw-rw-", filePerms);

		tsi.setUmask("276");
		tsi.cp(file, file+"-cp");
		String filePerms2 = tsi.getProperties(file+"-cp").getUNIXPermissions();
		assertEquals("r--------", filePerms2);
		FileUtils.deleteQuietly(new File(dir));
	}

	@Test
	public void testGetInfo()throws Exception{
		Execution e=(Execution)xnjs.get(IExecution.class);
		Action job=new Action();
		job.setBSID("1234");
		Client c=new Client();
		c.setXlogin(new Xlogin(new String[]{"tst"}));
		job.setClient(c);
		assertNotNull(e.getBSSJobDetails(job));

		List<BudgetInfo>details = e.getComputeTimeBudget(c);
		assertNotNull(details);
		System.out.println(details);

		// test caching
		c.setXlogin(new Xlogin(new String[]{"tst"}));
		List<BudgetInfo> details2 = e.getComputeTimeBudget(c);
		assertTrue( details == details2 );
		// test caching
		c.setXlogin(new Xlogin(new String[]{"tst2"}));
		List<BudgetInfo> details3 = e.getComputeTimeBudget(c);
		assertTrue( details != details3 );
	}

	@Test
	public void testBatchMode() throws Exception {
		RemoteTSI tsi=makeTSI();
		assertNotNull(tsi);
		String tmpDir="test"+System.currentTimeMillis();
		tsi.setStorageRoot(new File("target").getAbsolutePath());
		// non-batch
		tsi.mkdir(tmpDir);

		// batch
		tsi.startBatch();
		tsi.mkdir(tmpDir);
		tsi.rmdir(tmpDir+"/f1");
		tsi.rmdir(tmpDir+"/f2");
		tsi.rmdir(tmpDir+"/f3");
		tsi.commitBatch();

		// check illegal state when trying to use exec and wait 
		tsi.startBatch();
		try{
			tsi.execAndWait("test", null);
			fail();
		}
		catch(IllegalStateException ex){/* OK as expected */}
		FileUtils.deleteQuietly(new File("target", tmpDir));
		
	}

	@Test
	// test this here because Link requires a UNICORE TSI
	public void testLink() throws Exception {
		Client c = new Client();
		c.setXlogin(new Xlogin(new String[]{"nobody"}));
		String dir = mkTmpDir();
		String source = "/tmp";
		String target = "this_is_the_link";
		Link link = new Link(xnjs, c, dir, source, target);
		link.run();
		assertEquals(TransferInfo.Status.DONE,link.getInfo().getStatus());
		RemoteTSI tsi = makeTSI();
		tsi.setStorageRoot(dir);
		XnjsFile f = tsi.getProperties(target);
		assertNotNull(f);
		System.out.println("Created link: " + f);
		FileUtils.deleteQuietly(new File(dir));
	}

	@Test
	public void testReservationParseReply()throws Exception{
		String date="2012-09-26T22:00:00+0200";
		SimpleDateFormat sf=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		Calendar c=Calendar.getInstance();
		c.setTime(sf.parse(date));
		String desc="All is fine";
		String rep="TSI_OK\nWAITING "+date+"\n"+desc;
		ReservationStatus rs=new Reservation().parseTSIReply(rep);
		Assert.assertEquals(Status.WAITING, rs.getStatus());
		Assert.assertEquals(c.getTimeInMillis(), rs.getStartTime().getTimeInMillis());
		Assert.assertEquals(desc, rs.getDescription());
	
		date="2012-09-26T22:00:00+0200";
		sf=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		c=Calendar.getInstance();
		c.setTime(sf.parse(date));
		rep="TSI_OK\nWAITING "+date+"\n";
		rs=new Reservation().parseTSIReply(rep);
		Assert.assertEquals(Status.WAITING, rs.getStatus());
		Assert.assertEquals(c.getTimeInMillis(), rs.getStartTime().getTimeInMillis());
		Assert.assertNull(rs.getDescription());
	}
	
	@Test
	public void testTimeoutHandling()throws Exception {
		DefaultTSIConnectionFactory f = (DefaultTSIConnectionFactory)xnjs.get(TSIConnectionFactory.class);
		assertNotNull(f);
		TSIConnection c = f.getTSIConnection("nobody", null,"localhost",-1);
		try {
			InetAddress localhost=InetAddress.getByName("localhost");
			assertEquals(localhost,c.getTSIAddress());
			c.setSocketTimeouts(3000, false);
			ExecutionContext ec = new ExecutionContext("123");
			IDB idb = xnjs.get(IDB.class);
			String message = TSIUtils.makeExecuteScript("sleep 10", ec, idb, null);
			c.sendNoUser(message);
		}catch(IOException ex) {
			assertTrue(ex.getMessage().contains("TSI <localhost>"));
			assertTrue(c.isShutdown());
			assertFalse(c.getConnector().isOK());
		}finally {
			c.close();
		}
	}

	@Test
	public void testTSIErrors() {
		String testDir=mkTmpDir();
		RemoteTSI tsi=makeTSI();
		try{
			tsi.cp("/no_such_file__", testDir+"/test123");
		}catch(ExecutionException ee1) {
			assertTrue(ee1.getMessage().contains("TSI ERROR:"));
		}
		try{
			tsi.link("/no_such_file__", testDir+"/test123");
		}catch(ExecutionException ee1) {
			assertTrue(ee1.getMessage().contains("TSI ERROR:"));
		}
	}
	
	private void writeFile(String path, String content)throws Exception{
		try(OutputStreamWriter osw=new OutputStreamWriter(makeTSI().getOutputStream(path))){
			osw.write(content);
		}
	}

}
