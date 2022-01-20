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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import de.fzj.unicore.xnjs.ems.EMSTestBase;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.ems.Processor;
import de.fzj.unicore.xnjs.io.XnjsFile;
import de.fzj.unicore.xnjs.json.JSONJobProcessor;
import de.fzj.unicore.xnjs.tsi.local.LocalTS;

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
		String baseDir = "/" + UUID.randomUUID().toString();
		String dir2 = baseDir + "/" + UUID.randomUUID().toString();
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
