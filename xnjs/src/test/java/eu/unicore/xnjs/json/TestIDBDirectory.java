package eu.unicore.xnjs.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import eu.unicore.security.Client;
import eu.unicore.security.Xlogin;
import eu.unicore.xnjs.BaseModule;
import eu.unicore.xnjs.ConfigurationSource;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.idb.IDB;
import eu.unicore.xnjs.resources.ResourceRequest;
import eu.unicore.xnjs.resources.ResourceSet;
import eu.unicore.xnjs.tsi.local.LocalTSIModule;

/**
 * tests the IDBImpl class with json content
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
		cs.getProperties().put("XNJS.idbfile","src/test/resources/resources/jsonidb.dir");
		File ext = new File("src/test/resources/resources/per-user.idb");
		cs.getProperties().put("XNJS.idbfile.ext.1", ext.getAbsolutePath());
		cs.addModule(new BaseModule(cs.getProperties()));
		cs.addModule(new LocalTSIModule(cs.getProperties()));
		return cs;
	}

	@Test
	public void testApps(){
		assertNotNull(idb.getApplication("Date", null, null));
		assertNull(idb.getApplication("NoSuchApp", null, null));
		// user specific app
		Client c = new Client();
		c.setXlogin(new Xlogin(new String[]{"nobody"}));
		assertNotNull(idb.getApplication("MyDate", null, c));
	}

	@Test
	public void testSiteDefaultResourceSet() throws Exception {
		List<ResourceRequest> rs = idb.getDefaultPartition().getResources().getDefaults();
		assertTrue(ResourceRequest.contains(rs, ResourceSet.NODES));
		assertTrue(ResourceRequest.contains(rs, ResourceSet.CPUS_PER_NODE));
		assertFalse(ResourceRequest.contains(rs, ResourceSet.MEMORY_PER_NODE));
	}


	@Test
	public void testInfo() throws Exception {
		assertEquals("GNU/Linux", idb.getDefaultPartition().getOperatingSystem());
		assertEquals("powerpc", idb.getDefaultPartition().getCPUArchitecture());
		assertEquals("localhost", idb.getTextInfo("ssh-host"));
	}
	
	@Test
	public void testScriptHeader() throws Exception {
		System.out.println(idb.getScriptHeader());
		assertTrue(idb.getScriptHeader().startsWith("#!/bin/bash"));
		assertTrue(idb.getScriptHeader().contains("FOO=bar\n"));
	}
}