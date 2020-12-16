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
 *********************************************************************************/


package de.fzj.unicore.xnjs.tsi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Assert;
import org.junit.Ignore;

import de.fzj.unicore.xnjs.ems.ExecutionContext;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.io.Permissions;
import de.fzj.unicore.xnjs.io.SimpleFindOptions;
import de.fzj.unicore.xnjs.io.XnjsFile;
import de.fzj.unicore.xnjs.io.XnjsStorageInfo;
import de.fzj.unicore.xnjs.util.IOUtils;

/**
 * abstract set of TSI tests
 * 
 * @author schuller
 */
@Ignore
public class AbstractTSITest  {
	
	protected final String testDir;
	
	protected final TSI tsi;

	/**
	 * @param testingDir - the base directory used
	 * @param tsi - TSI to be used for testing, with storageRoot set to testingDir
	 * @throws Exception
	 */
	public AbstractTSITest(String testingDir, TSI tsi)throws Exception{
		this.tsi=tsi;
		this.testDir=testingDir;
	}
	
	public boolean run()throws Exception{
		testChmod();
		testCp();
		testLink();
		testRm();
		testRmDir();
		testMkDir();
		testExec();
		testExecAndWait();
		testExecWithEnvReplace();
		testExecWithExtraSpacesInCmdLine();
		testLs();
		testFind();
		testGetHome();
		testGetFileProperties();
		testGetFreeSpace();
		testGetInputStream();
		testGetOutputStream();
		
		return true;
	}
	
	public void testChmod() throws Exception {
		writeTestFile("XNJS_unittest", "Hello World!");
		File t=new File(testDir,"XNJS_unittest");
		Assert.assertTrue(t.exists());
		tsi.chmod("XNJS_unittest", new Permissions(true,false,true));
		Assert.assertTrue(t.canExecute());
		Assert.assertTrue(t.canRead());
		Assert.assertFalse(t.canWrite());
		t.delete();
		Assert.assertFalse(t.exists());
	}
	
	public void testCp() throws Exception {
		writeTestFile("XNJS_unittest", "Hello World!");
		tsi.cp("XNJS_unittest", "copy_of_testfile");
		String r = readTestFile("copy_of_testfile");
		Assert.assertTrue("Hello World!".equals(r));
		tsi.rm("XNJS_unittest");
		File t=new File(testDir,"XNJS_unittest");
		Assert.assertFalse(t.exists());
	}
	
	public void testLink() throws Exception {
		writeTestFile("XNJS_unittest", "Hello World!");
		tsi.link("XNJS_unittest", "link_to_testfile");
		String r = readTestFile("link_to_testfile");
		Assert.assertTrue("Hello World!".equals(r));
		tsi.rm("XNJS_unittest");
		File t=new File(testDir,"XNJS_unittest");
		Assert.assertFalse(t.exists());
	}
	
	public void testRename() throws Exception {
		writeTestFile("XNJS_unittest", "Hello World!");
		File t=new File(testDir,"XNJS_unittest");
		Assert.assertTrue(t.exists());
		tsi.rename("XNJS_unittest", "XNJS_unittest_2");
		String r = readTestFile("XNJS_unittest_2");
		Assert.assertTrue("Hello World!".equals(r));
		tsi.rm("XNJS_unittest_2");
		File t2=new File(testDir,"XNJS_unittest_2");
		Assert.assertFalse(t2.exists());
	}
	
	public void testRm()throws Exception{
		writeTestFile("XNJS_unittest", "Hello World!");
		File t=new File(testDir+File.separator+"XNJS_unittest");
		Assert.assertTrue(t.exists());
		tsi.rm("XNJS_unittest");
		t=new File(testDir,"XNJS_unittest");
		Assert.assertFalse(t.exists());
	}

	public void testRmDir()throws Exception{
		writeTestFile("XNJS_unittest", "Hello World!");
		tsi.rmdir("/");
		File t=new File(testDir+File.separator+"XNJS_unittest");
		Assert.assertFalse(t.exists());
		t=new File(testDir);
		Assert.assertFalse(t.exists());	
	}

	public void testMkDir()throws Exception {
		String newdir="newdirectory"+System.currentTimeMillis();
		tsi.mkdir(newdir);
		File t=new File(testDir+File.separator+newdir);
		Assert.assertTrue(t.exists());
		Assert.assertTrue(t.canWrite());
		Assert.assertTrue(t.isDirectory());
	}

	public void testExec()throws Exception {
		try{
			String newdir="newdirectory"+System.currentTimeMillis();
			tsi.mkdir(newdir);
			ExecutionContext ec=new ExecutionContext("test");
			ec.setWorkingDirectory(testDir+File.separator+newdir);
			ec.setStdout("stdout");
			ec.setStderr("stderr");
			File t=new File(testDir+File.separator+newdir);
			Assert.assertTrue(t.exists());
			Assert.assertTrue(t.canWrite());
			tsi.exec("/bin/echo Hello",ec);
			//exec is async so need to wait
			Thread.sleep(1000);
			String r=readTestFile(newdir+File.separator+"stdout");
			Assert.assertTrue( r.contains("Hello"));
		}catch(Exception ex){
			ex.printStackTrace();
			Assert.fail();
		}
	}

	public void testExecAndWait()throws Exception {
		String newdir="newdirectory"+System.nanoTime();
		tsi.mkdir(newdir);
		ExecutionContext ec=new ExecutionContext("test");
		ec.setWorkingDirectory(testDir+File.separator+newdir);
		ec.setStdout("stdout");
		ec.setStderr("stderr");
		File t=new File(testDir+File.separator+newdir);
		Assert.assertTrue(t.exists());
		Assert.assertTrue(t.canWrite());
		tsi.execAndWait("/bin/echo Hello",ec);
		Assert.assertNotNull(ec.getExitCode());
		String r=readTestFile(newdir+File.separator+"stdout");
		Assert.assertTrue( r.contains("Hello"));
		Assert.assertEquals(Integer.valueOf(0),ec.getExitCode());
	}

	public void testExecWrongApp()throws Exception {
		String newdir="newdirectory"+System.currentTimeMillis();
		tsi.mkdir(newdir);
		ExecutionContext ec=new ExecutionContext("test");
		ec.setWorkingDirectory(testDir+File.separator+newdir);
		ec.setStdout("stdout");
		ec.setStderr("stderr");
		ec.setExecutable("/bin/nonexistentApplication");
		File t=new File(testDir+File.separator+newdir);
		Assert.assertTrue(t.exists());
		Assert.assertTrue(t.canWrite());
		try{
			tsi.execAndWait("/bin/nonexistentApplication Hello",ec);
			Assert.fail("Expected ExecutionException here.");
		}
		catch(ExecutionException ee){
			//OK
		}
	}

	public void testExecWithExtraSpacesInCmdLine()throws Exception {
		String newdir="newdirectory"+System.currentTimeMillis();
		tsi.mkdir(newdir);
		ExecutionContext ec=new ExecutionContext("test");
		ec.setWorkingDirectory(testDir+File.separator+newdir);
		ec.setStdout("stdout");
		ec.setStderr("stderr");
		File t=new File(testDir+File.separator+newdir);
		Assert.assertTrue(t.exists());
		Assert.assertTrue(t.canWrite());
		tsi.exec("/bin/echo     Hello   ",ec);
		//exec is async so need to wait
		Thread.sleep(1000);
		String r=readTestFile(newdir+File.separator+"stdout");
		Assert.assertTrue( r.contains("Hello"));
	}

	public void testExecWithEnvReplace()throws Exception {
		String newdir="newdirectory"+System.currentTimeMillis();
		tsi.mkdir(newdir);
		ExecutionContext ec=new ExecutionContext("test");
		ec.getEnvironment().put("TEXT","Hello World!");
		ec.getEnvironment().put("ANOTHER","Second");
		ec.setWorkingDirectory(testDir+File.separator+newdir);
		ec.setStdout("stdout");
		ec.setStderr("stderr");
		File t=new File(testDir+File.separator+newdir);
		Assert.assertTrue(t.exists());
		Assert.assertTrue(t.canWrite());
		tsi.exec("/bin/echo $TEXT ${ANOTHER}",ec);
		//exec is async so need to wait
		Thread.sleep(1000);
		String r=readTestFile(newdir+File.separator+"stdout");
		Assert.assertTrue( r.contains("Hello"));
		Assert.assertTrue( r.contains("Second"));
	}

	public void testLs()throws Exception{
		String newdir="newdirectory"+System.currentTimeMillis();
		String dir=testDir+File.separator+newdir;
		tsi.mkdir(newdir);
		writeTestFile(newdir+File.separator+"test","test123");
		System.out.println(dir);
		XnjsFile[] ls=tsi.ls(newdir);

		Assert.assertNotNull(ls);
		Assert.assertTrue(ls.length==1);
		XnjsFile f=ls[0];
		Assert.assertFalse(f.isDirectory());
		Assert.assertTrue(f.getSize()=="test123".length());
		Assert.assertNotNull(f.getLastModified().getTime());

		writeTestFile(newdir+File.separator+"test1","test456");
		ls=tsi.ls(newdir);
		Assert.assertNotNull(ls);
		Assert.assertTrue(ls.length==2);

		ls=tsi.ls(newdir,0,1,false);
		Assert.assertNotNull(ls);
		Assert.assertTrue(ls.length==1);
		String p1=ls[0].getPath();

		ls=tsi.ls(newdir,1,1,false);
		Assert.assertNotNull(ls);
		Assert.assertTrue(ls.length==1);
		String p2=ls[0].getPath();

		Assert.assertTrue(!p1.equals(p2));

		ls=tsi.ls(newdir,0,2,false);
		Assert.assertNotNull(ls);
		Assert.assertTrue(ls.length==2);
	}

	public void testFind()throws Exception{
		String newdir="newdirectory"+System.currentTimeMillis();
		String base=testDir+File.separator+newdir;
		tsi.mkdir(newdir);
		File t=new File(base);
		Assert.assertTrue(t.exists());
		Assert.assertTrue(t.isDirectory());
		writeTestFile(newdir+File.separator+"test.x","test123");

		XnjsFile[] found=tsi.find(newdir,SimpleFindOptions.suffixMatch(".x",true),-1,-1);
		Assert.assertNotNull(found);
		Assert.assertTrue(found.length==1);
		XnjsFile f=found[0];
		Assert.assertFalse(f.isDirectory());
		Assert.assertTrue(f.getSize()=="test123".length());
		Assert.assertNotNull(f.getLastModified().getTime());

		//make a subdir
		tsi.mkdir(newdir+File.separator+"sub");
		writeTestFile(newdir+File.separator+"sub"+File.separator+"second.x","test123");
		writeTestFile(newdir+File.separator+"sub"+File.separator+"some.y","test123");

		found=tsi.find(newdir,SimpleFindOptions.suffixMatch(".x",true),-1,-1);
		Assert.assertNotNull(found);
		Assert.assertTrue(found.length==2);


	}

	//helpers
	protected void writeTestFile(String n, String test) throws Exception {
		File f=new File(testDir,n);
		FileOutputStream fos=new FileOutputStream(f);
		fos.write(test.getBytes());
		fos.flush();
		fos.close();
	}

	protected String readTestFile(String n) throws Exception {
		File f=new File(testDir,n);
		Assert.assertTrue(f.exists());
		Assert.assertTrue(f.canRead());
		FileInputStream fis=new FileInputStream(f);
		byte[] b=new byte[(int)f.length()];
		fis.read(b);
		fis.close();
		return new String(b);
	}

	public void testGetHome()throws Exception{
		String r=tsi.getHomePath();
		Assert.assertNotNull(r);
		Assert.assertTrue(!r.isEmpty());
	}

	public void testExecWithEnvReplaceSubStringClashes()throws Exception {
		String newdir="newdirectory"+System.currentTimeMillis();
		tsi.mkdir(newdir);
		ExecutionContext ec=new ExecutionContext("test");
		ec.getEnvironment().put("TEXTLONG"," World!");
		ec.getEnvironment().put("TEXT","Hello");

		ec.setWorkingDirectory(testDir+File.separator+newdir);
		ec.setStdout("stdout");
		ec.setStderr("stderr");
		File t=new File(testDir+File.separator+newdir);
		Assert.assertTrue(t.exists());
		Assert.assertTrue(t.canWrite());
		tsi.exec("/bin/echo $TEXT$TEXTLONG",ec);
		//exec is async so need to wait
		Thread.sleep(1000);
		String r=readTestFile(newdir+File.separator+"stdout");
		Assert.assertTrue( r.contains("Hello World!"));
	}

	public void xtestExecWithInputRedirect()throws Exception {
		String newdir="newdirectory"+System.nanoTime();
		tsi.mkdir(newdir);
		ExecutionContext ec=new ExecutionContext("test");
		ec.setWorkingDirectory(testDir+File.separator+newdir);
		ec.setStdout("stdout");
		ec.setStderr("stderr");
		ec.setStdin("input");
		writeTestFile(newdir+File.separator+"input", "TEST STRING\n");
		String r1=readTestFile(newdir+File.separator+"input");
		Assert.assertTrue(r1.contains("TEST STRING"));
		tsi.exec("/usr/bin/grep TEST",ec);
		//exec is async so need to wait
		Thread.sleep(1000);
		String r=readTestFile(newdir+File.separator+"stdout");
		Assert.assertTrue( r.contains("TEST"));
		Assert.assertEquals(Integer.valueOf(0),ec.getExitCode());
	}

	public void testGetFileProperties()throws Exception{
		String testString="TEST STRING\n";
		String newdir="newdirectory"+System.nanoTime();
		tsi.mkdir(newdir);
		writeTestFile(newdir+File.separator+"input", testString);
		XnjsFile f=tsi.getProperties(newdir+File.separator+"input");
		Assert.assertNotNull(f);
		Assert.assertEquals(f.getPath(),File.separator+newdir+File.separator+"input");
		Assert.assertEquals(testString.length(),f.getSize());

		f=tsi.getProperties(newdir+File.separator+"nonexistent");
		Assert.assertNull(f);
	}

	public void testGetFreeSpace()throws Exception{
		XnjsStorageInfo s=tsi.getAvailableDiskSpace("/");
		Assert.assertTrue(s.getTotalSpace()>0);
	}

	public void testGetInputStream()throws Exception{
		writeTestFile("XNJS_unittest", "Hello World!");
		File t=new File(testDir+File.separator+"XNJS_unittest");
		Assert.assertTrue(t.exists());
		String md5Original=IOUtils.md5(t);
		InputStream is=tsi.getInputStream("XNJS_unittest");
		Assert.assertNotNull(is);
		String md5=IOUtils.md5(is);
		is.close();
		Assert.assertEquals(md5Original, md5);
		tsi.rm("XNJS_unittest");
		t=new File(testDir,"XNJS_unittest");
		Assert.assertFalse(t.exists());
	}

	public void testGetOutputStream()throws Exception{
		OutputStream os=tsi.getOutputStream("XNJS_unittest");
		os.write("Hello World!".getBytes());
		os.close();
		File t=new File(testDir+File.separator+"XNJS_unittest");
		Assert.assertTrue(t.exists());
		String md5Original=IOUtils.md5(t);
		InputStream is=tsi.getInputStream("XNJS_unittest");
		Assert.assertNotNull(is);
		String md5=IOUtils.md5(is);
		is.close();
		Assert.assertEquals(md5Original, md5);
		tsi.rm("XNJS_unittest");
		t=new File(testDir,"XNJS_unittest");
		Assert.assertFalse(t.exists());
	}
}
