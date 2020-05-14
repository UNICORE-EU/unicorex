package de.fzj.unicore.xnjs.io;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;

import org.junit.Test;

import de.fzj.unicore.xnjs.tsi.TSI;

public class TestFindOptions {

	@Test
	public void testSuffixMatch(){
		String pattern="txt";
		FileFilter fo = SimpleFindOptions.suffixMatch(pattern, false);
		assertFalse(fo.recurse());
		XnjsFileImpl test=new XnjsFileImpl();
		test.setPath("foo.txt");
		assertTrue(fo.accept(test, null));
		test.setPath("xx");
		assertFalse(fo.accept(test, null));
	}
	
	@Test
	public void testPrefixMatch(){
		String pattern="ab";
		FileFilter fo = SimpleFindOptions.prefixMatch(pattern, false);
		assertFalse(fo.recurse());
		XnjsFileImpl test=new XnjsFileImpl();
		test.setPath("abc.txt");
		assertTrue(fo.accept(test, null));
		test.setPath("xx");
		assertFalse(fo.accept(test, null));
	}
	
	@Test
	public void testNameContains(){
		String pattern="bc";
		FileFilter fo = SimpleFindOptions.nameContains(pattern, false);
		assertFalse(fo.recurse());
		XnjsFileImpl test=new XnjsFileImpl();
		test.setPath("abc.txt");
		assertTrue(fo.accept(test, null));
		test.setPath("xx");
		assertFalse(fo.accept(test, null));
	}
	
	@Test
	public void testLowerSizeBound(){
		FileFilter fo = SimpleFindOptions.lowerSizeBound(123, false);
		assertFalse(fo.recurse());
		XnjsFileImpl test=new XnjsFileImpl();
		test.setPath("abc.txt");
		test.setSize(1000);
		assertTrue(fo.accept(test, null));
		test.setSize(10);
		assertFalse(fo.accept(test, null));
	}
	
	@Test
	public void testUpperSizeBound(){
		FileFilter fo = SimpleFindOptions.upperSizeBound(123, false);
		assertFalse(fo.recurse());
		XnjsFileImpl test=new XnjsFileImpl();
		test.setPath("abc.txt");
		test.setSize(10);
		assertTrue(fo.accept(test, null));
		test.setSize(1000);
		assertFalse(fo.accept(test, null));
	}
	
	@Test
	public void testLastAccessBefore(){
		Calendar lastAccess=Calendar.getInstance();
		lastAccess.set(2012, 12, 12);
		FileFilter fo = SimpleFindOptions.lastAccessBefore(lastAccess, false);
		assertFalse(fo.recurse());
		XnjsFileImpl test=new XnjsFileImpl();
		test.setPath("abc.txt");
		Calendar lastModified=Calendar.getInstance();
		lastModified.set(2012, 12, 10);
		test.setLastModified(lastModified);
		assertTrue(fo.accept(test, null));
		lastModified.set(2012, 12, 14);
		assertFalse(fo.accept(test, null));
	}
	
	@Test
	public void testLastAccessAfter(){
		Calendar lastAccess=Calendar.getInstance();
		lastAccess.set(2012, 12, 12);
		FileFilter fo = SimpleFindOptions.lastAccessAfter(lastAccess, false);
		assertFalse(fo.recurse());
		XnjsFileImpl test=new XnjsFileImpl();
		test.setPath("abc.txt");
		Calendar lastModified=Calendar.getInstance();
		lastModified.set(2012, 12, 14);
		test.setLastModified(lastModified);
		assertTrue(fo.accept(test, null));
		lastModified.set(2012, 12, 10);
		assertFalse(fo.accept(test, null));
	}
	
	@Test
	public void testFilesOnly(){
		FileFilter fo = SimpleFindOptions.filesOnly(false);
		assertFalse(fo.recurse());
		XnjsFileImpl test=new XnjsFileImpl();
		test.setPath("abc");
		test.setDirectory(false);
		assertTrue(fo.accept(test, null));
		test.setDirectory(true);
		assertFalse(fo.accept(test, null));
	}
	
	@Test
	public void testDirectoriesOnly(){
		FileFilter fo = SimpleFindOptions.directoriesOnly(false);
		assertFalse(fo.recurse());
		XnjsFileImpl test=new XnjsFileImpl();
		test.setPath("abc");
		test.setDirectory(true);
		assertTrue(fo.accept(test, null));
		test.setDirectory(false);
		assertFalse(fo.accept(test, null));
	}
	
	@Test
	public void testStringMatch1(){
		String pattern="*test";
		FileFilter fo = SimpleFindOptions.stringMatch(pattern, false);
		XnjsFileImpl test=new XnjsFileImpl();
		test.setPath("foo_test");
		assertTrue(fo.accept(test, null));
		test.setPath("xx");
		assertFalse(fo.accept(test, null));
	}
	
	@Test
	public void testStringMatch2(){
		String pattern="test*";
		FileFilter fo = SimpleFindOptions.stringMatch(pattern, false);
		XnjsFileImpl test=new XnjsFileImpl();
		test.setPath("test_foo");
		assertTrue(fo.accept(test, null));
		test.setPath("xx");
		assertFalse(fo.accept(test, null));
	}
	
	@Test
	public void testStringMatch3(){
		String pattern="*test*";
		FileFilter fo = SimpleFindOptions.stringMatch(pattern, false);
		XnjsFileImpl test=new XnjsFileImpl();
		test.setPath("foo_test_foo");
		assertTrue(fo.accept(test, null));
		test.setPath("xx");
		assertFalse(fo.accept(test, null));
	}
	
	@Test
	public void testStringMatch4(){
		String pattern="foo*bar";
		FileFilter fo = SimpleFindOptions.stringMatch(pattern, false);
		XnjsFileImpl test=new XnjsFileImpl();
		test.setPath("foo_test_bar");
		assertTrue(fo.accept(test, null));
		test.setPath("foo_xx");
		assertFalse(fo.accept(test, null));
	}
	
	@Test
	public void testStringMatch5(){
		String pattern="foo?bar";
		FileFilter fo = SimpleFindOptions.stringMatch(pattern, false);
		XnjsFileImpl test=new XnjsFileImpl();
		test.setPath("foo1bar");
		assertTrue(fo.accept(test, null));
		test.setPath("foo123bar");
		assertFalse(fo.accept(test, null));
	}
	
	@Test
	public void testStringMatch6(){
		String pattern="foo??bar";
		FileFilter fo = SimpleFindOptions.stringMatch(pattern, false);
		XnjsFileImpl test=new XnjsFileImpl();
		test.setPath("foo12bar");
		assertTrue(fo.accept(test, null));
		test.setPath("foo123bar");
		assertFalse(fo.accept(test, null));
	}
	
	@Test
	public void testStringMatch7(){
		String pattern="*foo??bar";
		FileFilter fo = SimpleFindOptions.stringMatch(pattern, false);
		XnjsFileImpl test=new XnjsFileImpl();
		test.setPath("test_foo12bar");
		assertTrue(fo.accept(test, null));
		test.setPath("foo12bar");
		assertTrue(fo.accept(test, null));
		test.setPath("xx_foo123bar");
		assertFalse(fo.accept(test, null));
	}
	
	@Test
	public void testStringMatch8(){
		String pattern="?*foo";
		FileFilter fo = SimpleFindOptions.stringMatch(pattern, false);
		XnjsFileImpl test=new XnjsFileImpl();
		test.setPath("test_foo");
		assertTrue(fo.accept(test, null));
		test.setPath("1foo");
		assertTrue(fo.accept(test, null));
		test.setPath("foo");
		assertFalse(fo.accept(test, null));
		test.setPath("_foo");
		assertTrue(fo.accept(test, null));
		test.setPath("alonger1bar2foo");
		assertTrue(fo.accept(test, null));
	}
	
	@Test
	public void testComposite1(){
		CompositeFindOptions cfo=new CompositeFindOptions();
		cfo.match(new MockFindOptions("test"));
		XnjsFile xf=new XnjsFileImpl("testfile",0,false,0,null,false);
		assertTrue(cfo.accept(xf, null));
	}
	
	@Test
	public void testComposite2(){
		CompositeFindOptions cfo=new CompositeFindOptions();
		cfo.match(new MockFindOptions("test")).and(new MockFindOptions("t"));
		XnjsFile xf=new XnjsFileImpl("testfile",0,false,0,null,false);
		assertTrue(cfo.accept(xf, null));
	}
	
	@Test
	public void testComposite3(){
		CompositeFindOptions cfo=new CompositeFindOptions();
		cfo.match(new MockFindOptions("test")).or(new MockFindOptions("file"));
		XnjsFile xf=new XnjsFileImpl("testfile",0,false,0,null,false);
		assertTrue(cfo.accept(xf, null));
	}
	
	@Test
	public void testComposite4(){
		CompositeFindOptions cfo=new CompositeFindOptions();
		cfo.match(new MockFindOptions("test"))
		    .or(new MockFindOptions("file"))
		    .or(new MockFindOptions("stf"));
		XnjsFile xf=new XnjsFileImpl("testfile",0,false,0,null,false);
		assertTrue(cfo.accept(xf, null));
	}
	
	@Test
	public void testComposite5(){
		CompositeFindOptions cfo=new CompositeFindOptions();
		cfo.match(new MockFindOptions("test")).and(new MockFindOptions("x"));
		XnjsFile xf=new XnjsFileImpl("testfile",0,false,0,null,false);
		assertFalse(cfo.accept(xf, null));
	}
	
	@Test
	public void testComposite6(){
		CompositeFindOptions cfo=new CompositeFindOptions();
		cfo.match(new MockFindOptions("xx")).or(new MockFindOptions("x"));
		XnjsFile xf=new XnjsFileImpl("testfile",0,false,0,null,false);
		assertFalse(cfo.accept(xf, null));
	}
	
	@Test
	public void testComposite7(){
		CompositeFindOptions cfo=new CompositeFindOptions();
		cfo.match(new MockFindOptions("x"));
		cfo.or(new MockFindOptions("test"));
		XnjsFile xf=new XnjsFileImpl("testfile",0,false,0,null,false);
		assertTrue(cfo.accept(xf, null));
	}
	
	private static class MockFindOptions implements FileFilter{

		private final String pattern;
		
		public MockFindOptions(String pattern){
			this.pattern=pattern;
		}
		
		public boolean accept(XnjsFile file, TSI tsi) {
			return file.getPath().contains(pattern);
		}

		public boolean recurse() {
			return false;
		}
	}

}
