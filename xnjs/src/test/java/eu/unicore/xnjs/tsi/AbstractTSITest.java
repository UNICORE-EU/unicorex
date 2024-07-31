package eu.unicore.xnjs.tsi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.jupiter.api.Disabled;

import eu.unicore.xnjs.ems.ExecutionContext;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.io.Permissions;
import eu.unicore.xnjs.io.SimpleFindOptions;
import eu.unicore.xnjs.io.XnjsFile;
import eu.unicore.xnjs.io.XnjsStorageInfo;
import eu.unicore.xnjs.util.IOUtils;

/**
 * abstract set of TSI tests
 * 
 * @author schuller
 */
@Disabled
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
		assertTrue(t.exists());
		tsi.chmod("XNJS_unittest", new Permissions(true,false,true));
		assertTrue(t.canExecute());
		assertTrue(t.canRead());
		assertFalse(t.canWrite());
		t.delete();
		assertFalse(t.exists());
	}
	
	public void testCp() throws Exception {
		writeTestFile("XNJS_unittest", "Hello World!");
		tsi.cp("XNJS_unittest", "copy_of_testfile");
		String r = readTestFile("copy_of_testfile");
		assertTrue("Hello World!".equals(r));
		tsi.rm("XNJS_unittest");
		File t=new File(testDir,"XNJS_unittest");
		assertFalse(t.exists());
	}
	
	public void testLink() throws Exception {
		writeTestFile("XNJS_unittest", "Hello World!");
		tsi.link("XNJS_unittest", "link_to_testfile");
		String r = readTestFile("link_to_testfile");
		assertTrue("Hello World!".equals(r));
		tsi.rm("XNJS_unittest");
		File t=new File(testDir,"XNJS_unittest");
		assertFalse(t.exists());
	}
	
	public void testRename() throws Exception {
		writeTestFile("XNJS_unittest", "Hello World!");
		File t=new File(testDir,"XNJS_unittest");
		assertTrue(t.exists());
		tsi.rename("XNJS_unittest", "XNJS_unittest_2");
		String r = readTestFile("XNJS_unittest_2");
		assertTrue("Hello World!".equals(r));
		tsi.rm("XNJS_unittest_2");
		File t2=new File(testDir,"XNJS_unittest_2");
		assertFalse(t2.exists());
	}
	
	public void testRm()throws Exception{
		writeTestFile("XNJS_unittest", "Hello World!");
		File t=new File(testDir+File.separator+"XNJS_unittest");
		assertTrue(t.exists());
		tsi.rm("XNJS_unittest");
		t=new File(testDir,"XNJS_unittest");
		assertFalse(t.exists());
	}

	public void testRmDir()throws Exception{
		writeTestFile("XNJS_unittest", "Hello World!");
		tsi.rmdir("/");
		File t=new File(testDir+File.separator+"XNJS_unittest");
		assertFalse(t.exists());
		t=new File(testDir);
		assertFalse(t.exists());	
	}

	public void testMkDir()throws Exception {
		String newdir="newdirectory"+System.currentTimeMillis();
		tsi.mkdir(newdir);
		File t=new File(testDir+File.separator+newdir);
		assertTrue(t.exists());
		assertTrue(t.canWrite());
		assertTrue(t.isDirectory());
	}

	public void testExec()throws Exception {
		try{
			String newdir="newdirectory"+System.currentTimeMillis();
			tsi.mkdir(newdir);
			ExecutionContext ec = new ExecutionContext();
			ec.setWorkingDirectory(testDir+File.separator+newdir);
			ec.setStdout("stdout");
			ec.setStderr("stderr");
			File t=new File(testDir+File.separator+newdir);
			assertTrue(t.exists());
			assertTrue(t.canWrite());
			tsi.exec("/bin/echo Hello",ec);
			//exec is async so need to wait
			Thread.sleep(1000);
			String r=readTestFile(newdir+File.separator+"stdout");
			assertTrue( r.contains("Hello"));
		}catch(Exception ex){
			ex.printStackTrace();
			fail();
		}
	}

	public void testExecAndWait()throws Exception {
		String newdir="newdirectory"+System.nanoTime();
		tsi.mkdir(newdir);
		ExecutionContext ec = new ExecutionContext();
		ec.setWorkingDirectory(testDir+File.separator+newdir);
		ec.setStdout("stdout");
		ec.setStderr("stderr");
		File t=new File(testDir+File.separator+newdir);
		assertTrue(t.exists());
		assertTrue(t.canWrite());
		tsi.execAndWait("/bin/echo Hello",ec);
		assertNotNull(ec.getExitCode());
		String r=readTestFile(newdir+File.separator+"stdout");
		assertTrue( r.contains("Hello"));
		assertEquals(Integer.valueOf(0),ec.getExitCode());
	}

	public void testExecWrongApp()throws Exception {
		String newdir="newdirectory"+System.currentTimeMillis();
		tsi.mkdir(newdir);
		ExecutionContext ec = new ExecutionContext();
		ec.setWorkingDirectory(testDir+File.separator+newdir);
		ec.setStdout("stdout");
		ec.setStderr("stderr");
		ec.setExecutable("/bin/nonexistentApplication");
		File t=new File(testDir+File.separator+newdir);
		assertTrue(t.exists());
		assertTrue(t.canWrite());
		try{
			tsi.execAndWait("/bin/nonexistentApplication Hello",ec);
			fail("Expected ExecutionException here.");
		}
		catch(ExecutionException ee){
			//OK
		}
	}

	public void testExecWithExtraSpacesInCmdLine()throws Exception {
		String newdir="newdirectory"+System.currentTimeMillis();
		tsi.mkdir(newdir);
		ExecutionContext ec = new ExecutionContext();
		ec.setWorkingDirectory(testDir+File.separator+newdir);
		ec.setStdout("stdout");
		ec.setStderr("stderr");
		File t=new File(testDir+File.separator+newdir);
		assertTrue(t.exists());
		assertTrue(t.canWrite());
		tsi.exec("/bin/echo     Hello   ",ec);
		//exec is async so need to wait
		Thread.sleep(1000);
		String r=readTestFile(newdir+File.separator+"stdout");
		assertTrue( r.contains("Hello"));
	}

	public void testExecWithEnvReplace()throws Exception {
		String newdir="newdirectory"+System.currentTimeMillis();
		tsi.mkdir(newdir);
		ExecutionContext ec = new ExecutionContext();
		ec.getEnvironment().put("TEXT","Hello World!");
		ec.getEnvironment().put("ANOTHER","Second");
		ec.setWorkingDirectory(testDir+File.separator+newdir);
		ec.setStdout("stdout");
		ec.setStderr("stderr");
		File t=new File(testDir+File.separator+newdir);
		assertTrue(t.exists());
		assertTrue(t.canWrite());
		tsi.exec("/bin/echo $TEXT ${ANOTHER}",ec);
		//exec is async so need to wait
		Thread.sleep(1000);
		String r=readTestFile(newdir+File.separator+"stdout");
		assertTrue( r.contains("Hello"));
		assertTrue( r.contains("Second"));
	}

	public void testLs()throws Exception{
		String newdir="newdirectory"+System.currentTimeMillis();
		String dir=testDir+File.separator+newdir;
		tsi.mkdir(newdir);
		writeTestFile(newdir+File.separator+"test","test123");
		System.out.println(dir);
		XnjsFile[] ls=tsi.ls(newdir);

		assertNotNull(ls);
		assertTrue(ls.length==1);
		XnjsFile f=ls[0];
		assertFalse(f.isDirectory());
		assertTrue(f.getSize()=="test123".length());
		assertNotNull(f.getLastModified().getTime());

		writeTestFile(newdir+File.separator+"test1","test456");
		ls=tsi.ls(newdir);
		assertNotNull(ls);
		assertTrue(ls.length==2);

		ls=tsi.ls(newdir,0,1,false);
		assertNotNull(ls);
		assertTrue(ls.length==1);
		String p1=ls[0].getPath();

		ls=tsi.ls(newdir,1,1,false);
		assertNotNull(ls);
		assertTrue(ls.length==1);
		String p2=ls[0].getPath();

		assertTrue(!p1.equals(p2));

		ls=tsi.ls(newdir,0,2,false);
		assertNotNull(ls);
		assertTrue(ls.length==2);
	}

	public void testFind()throws Exception{
		String newdir="newdirectory"+System.currentTimeMillis();
		String base=testDir+File.separator+newdir;
		tsi.mkdir(newdir);
		File t=new File(base);
		assertTrue(t.exists());
		assertTrue(t.isDirectory());
		writeTestFile(newdir+File.separator+"test.x","test123");

		XnjsFile[] found=tsi.find(newdir,SimpleFindOptions.suffixMatch(".x",true),-1,-1);
		assertNotNull(found);
		assertTrue(found.length==1);
		XnjsFile f=found[0];
		assertFalse(f.isDirectory());
		assertTrue(f.getSize()=="test123".length());
		assertNotNull(f.getLastModified().getTime());

		//make a subdir
		tsi.mkdir(newdir+File.separator+"sub");
		writeTestFile(newdir+File.separator+"sub"+File.separator+"second.x","test123");
		writeTestFile(newdir+File.separator+"sub"+File.separator+"some.y","test123");

		found=tsi.find(newdir,SimpleFindOptions.suffixMatch(".x",true),-1,-1);
		assertNotNull(found);
		assertTrue(found.length==2);


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
		assertTrue(f.exists());
		assertTrue(f.canRead());
		FileInputStream fis=new FileInputStream(f);
		byte[] b=new byte[(int)f.length()];
		fis.read(b);
		fis.close();
		return new String(b);
	}

	public void testGetHome()throws Exception{
		String r=tsi.getHomePath();
		assertNotNull(r);
		assertTrue(!r.isEmpty());
	}

	public void testExecWithEnvReplaceSubStringClashes()throws Exception {
		String newdir="newdirectory"+System.currentTimeMillis();
		tsi.mkdir(newdir);
		ExecutionContext ec = new ExecutionContext();
		ec.getEnvironment().put("TEXTLONG"," World!");
		ec.getEnvironment().put("TEXT","Hello");

		ec.setWorkingDirectory(testDir+File.separator+newdir);
		ec.setStdout("stdout");
		ec.setStderr("stderr");
		File t=new File(testDir+File.separator+newdir);
		assertTrue(t.exists());
		assertTrue(t.canWrite());
		tsi.exec("/bin/echo $TEXT$TEXTLONG",ec);
		//exec is async so need to wait
		Thread.sleep(1000);
		String r=readTestFile(newdir+File.separator+"stdout");
		assertTrue( r.contains("Hello World!"));
	}

	public void xtestExecWithInputRedirect()throws Exception {
		String newdir="newdirectory"+System.nanoTime();
		tsi.mkdir(newdir);
		ExecutionContext ec = new ExecutionContext();
		ec.setWorkingDirectory(testDir+File.separator+newdir);
		ec.setStdout("stdout");
		ec.setStderr("stderr");
		ec.setStdin("input");
		writeTestFile(newdir+File.separator+"input", "TEST STRING\n");
		String r1=readTestFile(newdir+File.separator+"input");
		assertTrue(r1.contains("TEST STRING"));
		tsi.exec("/usr/bin/grep TEST",ec);
		//exec is async so need to wait
		Thread.sleep(1000);
		String r=readTestFile(newdir+File.separator+"stdout");
		assertTrue( r.contains("TEST"));
		assertEquals(Integer.valueOf(0),ec.getExitCode());
	}

	public void testGetFileProperties()throws Exception{
		String testString="TEST STRING\n";
		String newdir="newdirectory"+System.nanoTime();
		tsi.mkdir(newdir);
		writeTestFile(newdir+File.separator+"input", testString);
		XnjsFile f=tsi.getProperties(newdir+File.separator+"input");
		assertNotNull(f);
		assertEquals(f.getPath(),File.separator+newdir+File.separator+"input");
		assertEquals(testString.length(),f.getSize());

		f=tsi.getProperties(newdir+File.separator+"nonexistent");
		assertNull(f);
	}

	public void testGetFreeSpace()throws Exception{
		XnjsStorageInfo s=tsi.getAvailableDiskSpace("/");
		assertTrue(s.getTotalSpace()>0);
	}

	public void testGetInputStream()throws Exception{
		writeTestFile("XNJS_unittest", "Hello World!");
		File t=new File(testDir+File.separator+"XNJS_unittest");
		assertTrue(t.exists());
		String md5Original=IOUtils.md5(t);
		InputStream is=tsi.getInputStream("XNJS_unittest");
		assertNotNull(is);
		String md5=IOUtils.md5(is);
		is.close();
		assertEquals(md5Original, md5);
		tsi.rm("XNJS_unittest");
		t=new File(testDir,"XNJS_unittest");
		assertFalse(t.exists());
	}

	public void testGetOutputStream()throws Exception{
		OutputStream os=tsi.getOutputStream("XNJS_unittest");
		os.write("Hello World!".getBytes());
		os.close();
		File t=new File(testDir+File.separator+"XNJS_unittest");
		assertTrue(t.exists());
		String md5Original=IOUtils.md5(t);
		InputStream is=tsi.getInputStream("XNJS_unittest");
		assertNotNull(is);
		String md5=IOUtils.md5(is);
		is.close();
		assertEquals(md5Original, md5);
		tsi.rm("XNJS_unittest");
		t=new File(testDir,"XNJS_unittest");
		assertFalse(t.exists());
	}
}
