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


package de.fzj.unicore.xnjs.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
import de.fzj.unicore.xnjs.idb.IDB;
import de.fzj.unicore.xnjs.resources.ResourceRequest;
import de.fzj.unicore.xnjs.resources.ResourceSet;
import de.fzj.unicore.xnjs.tsi.local.LocalTSIModule;

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
		cs.addModule(new BaseModule(cs.getProperties()));
		cs.addModule(new LocalTSIModule(cs.getProperties()));
		return cs;
	}

	@Test
	public void testApps(){
		assertNotNull(idb.getApplication("Date", null, null));
	}

	@Test
	public void testSiteDefaultResourceSet() throws Exception {
		List<ResourceRequest> rs = idb.getDefaultPartition().getResources().getDefaults();
		assertTrue(ResourceRequest.contains(rs, ResourceSet.NODES));
		assertTrue(ResourceRequest.contains(rs, ResourceSet.CPUS_PER_NODE));
	}


	@Test
	public void testInfo() throws Exception {
		assertEquals("GNU/Linux", idb.getDefaultPartition().getOperatingSystem());
		assertEquals("powerpc", idb.getDefaultPartition().getCPUArchitecture());
		assertEquals("localhost", idb.getTextInfo("ssh-host"));
	}
	
	@Test
	public void testScriptTemplates() throws Exception {
		System.out.println(idb.getExecuteTemplate());
		assertTrue(idb.getExecuteTemplate().startsWith("#!/bin/bash"));
		System.out.println(idb.getSubmitTemplate());
		assertTrue(idb.getExecuteTemplate().contains("FOO=bar\n#SCRIPT"));
		assertTrue(idb.getSubmitTemplate().contains("FOO=bar\n#SCRIPT"));
		
	}
}