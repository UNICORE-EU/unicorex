package eu.unicore.xnjs.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import eu.unicore.xnjs.BaseModule;
import eu.unicore.xnjs.ConfigurationSource;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.BudgetInfo;
import eu.unicore.xnjs.io.XnjsFile;
import eu.unicore.xnjs.tsi.local.LocalTSIModule;
import eu.unicore.xnjs.tsi.remote.TSIConnection;
import eu.unicore.xnjs.tsi.remote.TSIMessages;

public class TestUtils {

	@Test
	public void testUNICORESpecificURLs()throws Exception{
		String unEnc="BFT:https://foo:123/services/Storage?res=xx#/file with spaces";
		String enc=URIUtils.encodeAll(unEnc);
		assertEquals("BFT:https://foo:123/services/Storage?res=xx#/file%20with%20spaces",enc);
		assertEquals(unEnc,URIUtils.decode(enc));
	}
	
	@Test
	public void testPlainURLs()throws Exception{
		String unEnc="https://foo:123/services/Storage?res=xx#/file with spaces";
		String enc=URIUtils.encodeAll(unEnc);
		assertEquals("https://foo:123/services/Storage?res=xx#/file%20with%20spaces",enc);
		assertEquals(unEnc,URIUtils.decode(enc));
	}
	
	@Test
	public void testFileURLs()throws Exception{
		String unEnc="file:///file with spaces";
		String enc=URIUtils.encodeAll(unEnc);
		assertEquals("file:///file%20with%20spaces",enc);
		assertEquals(unEnc,URIUtils.decode(enc));
	}
	
	@Test
	public void testFileURL2()throws Exception{
		String unEnc="file:///${HOME}/stdout.txt";
		String enc=URIUtils.encodeAll(unEnc);
		System.out.println(enc);
	}
	
	@Test
	public void testC9MURLs()throws Exception{
		String unEnc="c9m:logical file";
		String enc=URIUtils.encodeAll(unEnc);
		assertEquals("c9m:logical%20file",enc);
		assertEquals(unEnc,URIUtils.decode(enc));
	}

	@Test
	public void testNoScheme()throws Exception{
		String unEnc="/logical file";
		String enc=URIUtils.encodeAll(unEnc);
		assertEquals("/logical%20file",enc);
		assertEquals(unEnc,URIUtils.decode(enc));
	}
	
	@Test
	public void testValid1()throws Exception{
		String file="/a/b/c/d/e";
		List<String>pathList=IOUtils.getRelativePathList(new File(file),"/");
		assertNotNull(pathList);
		assertEquals(5, pathList.size());
		assertEquals("e", pathList.get(0));
		assertEquals("a", pathList.get(4));
	}
	
	@Test
	public void testValid1NotTrivialRoot()throws Exception{
		String file="/a/b/c/d/e";
		List<String>pathList=IOUtils.getRelativePathList(new File(file),"/a/b");
		assertNotNull(pathList);
		assertEquals(3, pathList.size());
		assertEquals("e", pathList.get(0));
		assertEquals("c", pathList.get(2));
	}
	
	@Test
	public void testValid2()throws Exception{
		String file="/a/b/../c/d";
		List<String>pathList=IOUtils.getRelativePathList(new File(file),"/");
		assertNotNull(pathList);
		assertEquals(3, pathList.size());
		assertEquals("d", pathList.get(0));
		assertEquals("c", pathList.get(1));
		assertEquals("a", pathList.get(2));
	}
	
	@Test
	public void testInvalid1()throws Exception{
		String file="/../b/c/d/e";
		try{
			IOUtils.getRelativePathList(new File(file),"/");
		}catch(IllegalArgumentException e){
			assertTrue(e.getMessage().contains("too many '..'"));
		}
	}
	
	@Test
	public void testGetPathValid1(){
		String file="/a/b/c/d/e";
		String rel=IOUtils.getRelativePath(new File(file),"/");
		assertEquals(file, rel);
	}
	
	@Test
	public void testGetPathValid2(){
		String file="/a/b/c/d/e";
		String rel=IOUtils.getRelativePath(new File(file),"/a");
		assertEquals("/b/c/d/e", rel);
	}
	
	@Test
	public void testGetPathValid3(){
		String file="/a/b/c/d/e";
		String rel=IOUtils.getRelativePath(new File(file),"/a/b");
		assertEquals("/c/d/e", rel);
	}
	
	@Test
	public void testGetPathValid4(){
		String file="/a/b/c/d/e/..";
		String rel=IOUtils.getRelativePath(new File(file),"/a/b");
		assertEquals("/c/d", rel);
	}
	
	@Test
	public void testGetPathValid5(){
		String file="/a/b/";
		String rel=IOUtils.getRelativePath(new File(file),"/a/b");
		assertEquals("/", rel);
	}
	
	@Test
	public void testGetPathValid6(){
		String file="/a/b/";
		String rel=IOUtils.getRelativePath(new File(file),"/a/b/");
		assertEquals("/", rel);
	}
	
	@Test
	public void testGetPathInvalid1(){
		String file="/a/..";
		try{
			IOUtils.getRelativePath(new File(file),"/a");
		}catch(IllegalArgumentException e){
			assertTrue(e.getMessage().contains("too many '..'"));
		}
	}
	
	@Test
	public void testSCPUri()throws Exception{
		String in="scp://host/path/to/file";
		assertEquals("test@host:/path/to/file", IOUtils.makeSCPAddress(in, "test"));
	}
	
	@Test
	public void testSCPUriWithPort()throws Exception{
		String in="scp://host:1234/path/to/file";
		assertEquals("test@host:/path/to/file", IOUtils.makeSCPAddress(in, "test"));
	}
	
	@Test
	public void testFormat(){
		String a1=IOUtils.format(2.11, 2);
		String a2=IOUtils.format(2.11123, 2);
		assertEquals(a1,a2);
	}
	
	@Test
	public void testFileMonitor()throws Exception{
		ConfigurationSource cs = new ConfigurationSource();
		cs.getProperties().put("XNJS.idbfile", "src/test/resources/simpleidb");
		cs.addModule(new BaseModule(cs.getProperties()));
		cs.addModule(new LocalTSIModule(cs.getProperties()));
		XNJS xnjs=new XNJS(cs);
		String name="/xnjs-test-filemonitor"+System.currentTimeMillis();
		File target=new File("target",name);
		target.deleteOnExit();
		FileOutputStream fos=new FileOutputStream(target);
		fos.write("test".getBytes());
		fos.close();
		assertTrue(target.exists());
		//create a monitor for the target file
		FileMonitor f=new FileMonitor("target",target.getName(),null,xnjs,200,TimeUnit.MILLISECONDS);
		XnjsFile info=f.getInfo();
		assertNotNull(info);
		assertEquals(name, info.getPath());
		assertEquals(target.length(), info.getSize());
		Thread.sleep(1500);
		target.setLastModified(System.currentTimeMillis());
		Thread.sleep(500);
		XnjsFile info2=f.getInfo();
		assertNotNull(info2);
		Calendar c1=info.getLastModified();
		Calendar c2=info2.getLastModified();
		assertNotNull(c1);
		assertNotNull(c2);
		assertTrue(c1.getTimeInMillis()<c2.getTimeInMillis());
		f.dispose();
		Thread.sleep(500);
	}
	
	@Test
	public void testSanitize(){
		
		String[]inputs = new String[]{
				"peter's documents",
		};
		
		String[]sanitized = new String[]{
				"peter'\\''s documents",
		};
		int i=0;
		for(String in: inputs){
			String out = TSIMessages.sanitize(in);
			assertEquals("Sanitize error", sanitized[i], out);
			i++;
		}
	}
	
	@Test
	public void testPathNormalize(){
		
		String[]inputs = new String[]{
				"//foo",
				"/foo/",
				"//foo/bar/baz/../",
		};
		
		String[]normalized = new String[]{
				"/foo",
				"/foo/",
				"/foo/bar/"
		};
		int i=0;
		for(String in: inputs){
			String out = IOUtils.getNormalizedPath(in);
			assertEquals("Normalization error", normalized[i], out);
			i++;
		}
	}
	
	@Test
	public void testFullPath(){
		
		String[]inputs = new String[]{
				"//foo",
				"/foo/",
				"//foo/bar/baz/../",
		};
		
		String[]normalized = new String[]{
				"/base/foo",
				"/base/foo/",
				"/base/foo/bar/"
		};
		int i=0;
		for(String in: inputs){
			String out = IOUtils.getFullPath("/base/", in, false);
			assertEquals("Normalization error", normalized[i], out);
			i++;
		}
	}
	
	@Test
	public void testAuthHeader(){
		assertEquals("Basic d2lraTpwZWRpYQ==", IOUtils.getBasicAuth("wiki", "pedia"));
	}

	@Test
	public void testBudgetInfo() {
		BudgetInfo b1 = new BudgetInfo("USER 100"); // old style
		assertEquals(BudgetInfo.CURRENT_PROJECT, b1.getProjectName());
		assertEquals("core-h", b1.getUnits());
		assertEquals(100, b1.getRemaining());
		
		BudgetInfo b2 = new BudgetInfo("hpc 100 10 node-h");
		assertEquals("hpc", b2.getProjectName());
		assertEquals("node-h", b2.getUnits());
		assertEquals(100, b2.getRemaining());
		assertEquals(10, b2.getPercentRemaining());
		
		try {
			new BudgetInfo("hpc 100 110 node-h");
		}catch(IllegalArgumentException e) {}
		
		String[] budgetInfos = new String[] {
				"HPC 1231 12 core-h",
				"HPC 1231 12.2 core-h"
		};
		for(String b: budgetInfos) {
			assertEquals(12, new BudgetInfo(b).getPercentRemaining());
		}
	}

	@Test
	public void testTSIVersionCompare() throws Exception {
		assertTrue(TSIConnection.doCompareVersions("9.0.0", "8.3.0"));
		assertTrue(TSIConnection.doCompareVersions("8.3.0", "8.2.0"));
		assertTrue(TSIConnection.doCompareVersions("8.3.1", "8.3.0"));
		assertFalse(TSIConnection.doCompareVersions("8.1.0", "8.3.0"));
		assertFalse(TSIConnection.doCompareVersions("8.1.0", "8.1.1"));
		assertFalse(TSIConnection.doCompareVersions("8.1.1", "8.1.2"));
	}

}
