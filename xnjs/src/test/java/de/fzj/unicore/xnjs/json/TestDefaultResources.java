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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import de.fzj.unicore.xnjs.BaseModule;
import de.fzj.unicore.xnjs.ConfigurationSource;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.idb.IDB;
import de.fzj.unicore.xnjs.idb.Incarnation;
import de.fzj.unicore.xnjs.idb.Partition;
import de.fzj.unicore.xnjs.resources.ResourceRequest;
import de.fzj.unicore.xnjs.resources.ResourceSet;
import de.fzj.unicore.xnjs.tsi.local.LocalTSIModule;

/**
 * tests handling of incarnation (CPUs, memory, etc) when IDB does not define any queues
 */
public class TestDefaultResources {

	private Incarnation g;

	private IDB idb;

	@Before
	public void setUp() throws Exception {
		XNJS config = new XNJS(getConfigSource()); 
		idb = config.get(IDB.class);
		g = config.get(Incarnation.class);
	}

	protected ConfigurationSource getConfigSource() throws IOException {
		ConfigurationSource cs = new ConfigurationSource();
		cs.getProperties().put("XNJS.idbfile","src/test/resources/resources/minimalidb");
		File fileSpace = new File("target","xnjs_filespace");
		FileUtils.deleteQuietly(fileSpace);
		cs.getProperties().put("XNJS.filespace",fileSpace.getAbsolutePath());
		cs.addModule(new BaseModule(cs.getProperties()));
		cs.addModule(new LocalTSIModule(cs.getProperties()));
		return cs;
	}

	@Test
	public void testPartitions() throws Exception {
		List<Partition> l = idb.getPartitions();
		System.out.println(l);
	}

	@Test
	public void testIncarnateResources() throws Exception {
		List<ResourceRequest> req = new ArrayList<>();
		req.add(new ResourceRequest(ResourceSet.QUEUE, "myqueue"));
		req.add(new ResourceRequest(ResourceSet.NODES, "16"));
		List<ResourceRequest> testRes = g.incarnateResources(req,null);
		System.out.println(testRes);
		String queue = ResourceRequest.find(testRes, ResourceSet.QUEUE).getRequestedValue();
		assertEquals("myqueue", queue);
		int nodes = Integer.valueOf(ResourceRequest.find(testRes, ResourceSet.NODES).getRequestedValue());
		assertEquals(16, nodes);
	}

}
