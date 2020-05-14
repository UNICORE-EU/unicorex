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


package de.fzj.unicore.xnjs.jsdl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import de.fzj.unicore.xnjs.BaseModule;
import de.fzj.unicore.xnjs.ConfigurationSource;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.idb.ApplicationInfo;
import de.fzj.unicore.xnjs.idb.IDB;
import de.fzj.unicore.xnjs.idb.IDBImpl;
import de.fzj.unicore.xnjs.idb.IDBParser;
import de.fzj.unicore.xnjs.json.JsonIDB;
import de.fzj.unicore.xnjs.resources.ResourceRequest;
import de.fzj.unicore.xnjs.resources.ResourceSet;
import de.fzj.unicore.xnjs.tsi.local.LocalTSIModule;
import eu.unicore.bugsreporter.annotation.FunctionalTest;
import eu.unicore.security.Client;
import eu.unicore.security.Xlogin;

/**
 * tests the IDBImpl class if configured to read from a directory (with mixed XML/JSON content)
 */
public class TestIDBDirectory {

	private IDB idb;

	@Before
	public void setUp() throws Exception {
		XNJS config = new XNJS(getConfigSource()); 
		idb = config.get(IDB.class);
	}

	protected ConfigurationSource getConfigSource() throws IOException {
		ConfigurationSource cs = new ConfigurationSource();
		File fileSpace=new File("target","xnjs_filespace");
		FileUtils.deleteQuietly(fileSpace);
		cs.getProperties().put("XNJS.filespace",fileSpace.getAbsolutePath());
		cs.getProperties().put("XNJS.idbfile","src/test/resources/resources/simpleidb.dir");
		cs.getProperties().put("XNJS.idbfile.ext.1", new File("src/test/resources/resources/per-user.idb").getAbsolutePath());
		cs.getProperties().put("XNJS.idbfile.ext.2", new File("src/test/resources/resources/*.idb").getAbsolutePath());
		cs.getProperties().put("XNJS.idbfile.ext.3", new File("src/test/resources/resources/*.json").getAbsolutePath());
		cs.addModule(new BaseModule(cs.getProperties()));
		cs.addModule(new LocalTSIModule(cs.getProperties()));
		return cs;
	}

	
	@Test
	public void testReadFile() {
		IDBParser parser = ((IDBImpl)idb).getParser(new File("src/test/resources/resources/simpleidb.dir/app.json"));
		assertEquals(JsonIDB.class, parser.getClass());
	}
	
	@Test
	public void testApps(){
		assertNotNull(idb.getApplication("Extra-Date-One", null, null));
		assertNotNull(idb.getApplication("Extra-Date-Two", null, null));
		assertNotNull(idb.getApplication("json-date", null, null));
		assertNull("Per-user app present in generic list",idb.getApplication("MyOwnDate", null, null));
		// per-user apps
		Client c = new Client();
		c.setXlogin(new Xlogin(new String[]{"test"}));
		assertNotNull("Per-user app missing",idb.getApplication("MyOwnDate", null, c));
		assertNotNull("Per-user app missing",idb.getApplication("MyOwnDate2", null, c));
	}

	@Test
	public void testSiteDefaultResourceSet() throws Exception {
		List<ResourceRequest> rs = idb.getDefaultPartition().getResources().getDefaults();
		assertTrue(ResourceRequest.contains(rs, ResourceSet.CPUS_PER_NODE));
		assertTrue(ResourceRequest.contains(rs, ResourceSet.NODES));
		assertTrue(ResourceRequest.contains(rs, ResourceSet.MEMORY_PER_NODE));
		assertTrue(ResourceRequest.contains(rs, "GPUsPerNode"));
	}

	@Test
	public void testOSSettings() throws Exception {
		assertTrue("LINUX".equals(idb.getDefaultPartition().getOperatingSystem()));
	}

	@Test
	@FunctionalTest(id="testIDBDirectory",description="Tests the IDB configured from a directory")
	public void testIDBDirectory() throws Exception {
		ApplicationInfo app=idb.getApplication("test", null, new Client());
		assertNotNull(app);
		ApplicationInfo app2=idb.getApplication("test2", "2.0", new Client());
		assertNotNull(app2);
	}

}
