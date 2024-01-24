package eu.unicore.xnjs.tsi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import eu.unicore.persist.util.UUID;
import eu.unicore.xnjs.ems.EMSTestBase;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.ems.Processor;
import eu.unicore.xnjs.io.XnjsFile;
import eu.unicore.xnjs.json.JSONJobProcessor;
import eu.unicore.xnjs.tsi.local.LocalTS;

/**
 * tests the {@link LocalTS} impl of the TSI interface
 * 
 * @author schuller
 */
public class TestLocalTS extends EMSTestBase {

	protected TSI tsi;

	protected final String tmpDir=new File("target", "xnjs_test-"+System.currentTimeMillis()).getPath();

	@Before
	public void setUp3()throws Exception{
		File t=new File(tmpDir);
		if(!t.exists()){
			boolean ok=new File(tmpDir).mkdirs();
			assertTrue(ok);
		}
		initTSI();
	}

	protected void initTSI()throws Exception{
		tsi=xnjs.getTargetSystemInterface(null);
		tsi.setStorageRoot(tmpDir);
	}

	@Test
	public void testCreateProcessor() throws Exception{
		Processor p=xnjs.createProcessor("JSON");
		assertNotNull(p);
		assertTrue(p.toString().contains(JSONJobProcessor.class.getName()));
	}

	@Test
	public void testBasicTSIFunctions()throws Exception{
		new AbstractTSITest(tmpDir,tsi).run();
		FileUtils.deleteQuietly(new File(tmpDir));
	}

	@Test
	public void testMkdirsTwice()throws ExecutionException{
		tsi.mkdir("/foo");
		tsi.mkdir("/foo");
	}

	@Test
	public void testUmask() {
		tsi.setUmask("0133");
		tsi.setUmask("0744");
		assertEquals("0744", tsi.getUmask());
		tsi.setUmask("0000");
		assertEquals("00", tsi.getUmask());
		tsi.setUmask("0633");
		assertEquals("0633", tsi.getUmask());
	}
	
	@Test
	public void testPermissions() throws ExecutionException {
		String baseDir = "/" + UUID.newUniqueID();
		String dir2 = baseDir + "/" + UUID.newUniqueID();
		try {
			tsi.mkdir(baseDir);
			tsi.setUmask("0022");
			tsi.mkdir(dir2);
			final XnjsFile file1 = tsi.ls(baseDir)[0];
			System.out.println(file1);
			assertTrue(dir2 + " is not a directory.", file1.isDirectory());
			assertTrue(dir2 + " is not readable.", file1.getPermissions().isReadable());
			assertTrue(dir2 + " is not writable.", file1.getPermissions().isWritable());
			assertTrue(dir2 + " is not executable.", file1.getPermissions().isExecutable());
			
			tsi.rmdir(dir2);
			
			tsi.setUmask("0722");
			tsi.mkdir(dir2);
			XnjsFile file2 = tsi.ls(baseDir)[0];
			System.out.println(file2);
			assertTrue(dir2 + " is not a directory.", file2.isDirectory());
			assertFalse(dir2 + " is readable.", file2.getPermissions().isReadable());
			assertFalse(dir2 + " is writable.", file2.getPermissions().isWritable());
			assertFalse(dir2 + " is executable.", file2.getPermissions().isExecutable());
			
			tsi.rmdir(dir2);
			
		} finally {
			tsi.rmdir(baseDir);
		}
	}

}
