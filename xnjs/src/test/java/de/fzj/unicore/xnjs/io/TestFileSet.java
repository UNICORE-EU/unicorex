package de.fzj.unicore.xnjs.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestFileSet {

	@Test
	public void testSimpleWildcards(){
		// case 1 - collect specified toplevel files
		String expr = "*.pdf";
		FileSet fs = new FileSet(expr);
		assertEquals("/*.pdf", fs.getIncludes()[0]);
		assertTrue(fs.isMultifile());
		assertTrue(fs.matches("foo.pdf"));
		assertFalse(fs.matches("no.txt"));
		assertTrue(fs.isMultifile());
		assertFalse(fs.isRecurse());
		
		// case 2 - recurse and collect specified files
		expr = "/base/**/fo*.pdf";
		fs = new FileSet(expr);
		assertTrue(fs.matches("/base/x/foo.pdf"));
		assertFalse(fs.matches("/base/x/no.txt"));
		assertFalse(fs.matches("/no/foo.pdf"));
		assertTrue(fs.isMultifile());
		assertTrue(fs.isRecurse());
		
		// case 3 - single file
		expr = "/test.txt";
		fs = new FileSet(expr);
		assertFalse(fs.matches("foo.pdf"));
		assertFalse(fs.matches("no.txt"));
		assertTrue(fs.matches("test.txt"));
		assertFalse(fs.isMultifile());
		assertFalse(fs.isRecurse());
		
		// case 4 - single directory
		expr = "/somedir";
		fs = new FileSet(expr,true);
		assertTrue(fs.matches("/somedir/foo.pdf"));
		assertTrue(fs.matches("/somedir/test.txt"));
		assertTrue(fs.matches("/somedir/sub/test.txt"));
		assertFalse(fs.matches("/test.txt"));
		assertTrue(fs.isMultifile());
		assertTrue(fs.isRecurse());
		
		// case 5 - wildcards in directory name and file name
		expr = "/dir*/*.txt";
		fs = new FileSet(expr,false);
		assertTrue(fs.matches("dir1/foo.txt"));
		assertTrue(fs.matches("/dir1/foo.txt"));
		assertTrue(fs.isMultifile());
		assertTrue(fs.isRecurse());
		assertFalse(fs.matches("dir1/subdir/foo.txt"));
		
		expr = "/dir*/output*/*.txt";
		fs = new FileSet(expr,false);
		assertTrue(fs.isRecurse());
	}

}
