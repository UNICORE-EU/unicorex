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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ExecutionContext;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.idb.ApplicationInfo;
import de.fzj.unicore.xnjs.idb.IDB;
import de.fzj.unicore.xnjs.idb.Incarnation;
import de.fzj.unicore.xnjs.resources.IntResource;
import de.fzj.unicore.xnjs.resources.ResourceRequest;
import de.fzj.unicore.xnjs.resources.ResourceSet;
import de.fzj.unicore.xnjs.tsi.local.LocalTSIModule;
import eu.unicore.security.Client;
import eu.unicore.security.Queue;

/**
 * tests handling of IDB resources and resource incarnation (CPUs, memory, etc)
 */
public class TestResourceIncarnation {

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
		cs.getProperties().put("XNJS.idbfile","src/test/resources/resources/jsonidb.dir");
		File fileSpace = new File("target","xnjs_filespace");
		FileUtils.deleteQuietly(fileSpace);
		cs.getProperties().put("XNJS.filespace",fileSpace.getAbsolutePath());
		cs.addModule(new BaseModule(cs.getProperties()));
		cs.addModule(new LocalTSIModule(cs.getProperties()));
		return cs;
	}
	

	@Test
	public void testIncarnateDefaultResources() throws Exception {
		List<ResourceRequest> req = new ArrayList<>();
		List<ResourceRequest> testRes = g.incarnateResources(req,null);
		System.out.println(testRes);
		
		//check against defaults from IDB
		int nodes = Integer.valueOf(ResourceRequest.find(testRes, ResourceSet.NODES).getRequestedValue());
		assertEquals(nodes,1);
		int cpusPerNode = Integer.valueOf(ResourceRequest.find(testRes, ResourceSet.CPUS_PER_NODE).getRequestedValue());
		assertEquals(cpusPerNode,2);
	}

	@Test
	public void testIncarnateResources() throws Exception {
		long memRequest = ((IntResource) ( idb.getDefaultPartition().getResources().
				getResource(ResourceSet.MEMORY_PER_NODE))).getLower();
		memRequest = (long)(1.1 * memRequest);
		List<ResourceRequest> request = new ArrayList<>();
		request.add(new ResourceRequest(ResourceSet.MEMORY_PER_NODE, String.valueOf(memRequest)));
		
		List<ResourceRequest> testRes = g.incarnateResources(request,null);
		long mem = Long.valueOf(ResourceRequest.find(testRes, ResourceSet.MEMORY_PER_NODE).getRequestedValue());
		assertEquals(mem,memRequest);
	
		//check defaults from IDB
		int nodes = Integer.valueOf(ResourceRequest.find(testRes, ResourceSet.NODES).getRequestedValue());
		assertEquals(nodes,1);
		int cpusPerNode = Integer.valueOf(ResourceRequest.find(testRes, ResourceSet.CPUS_PER_NODE).getRequestedValue());
		assertEquals(cpusPerNode,2);	
	}

	@Test
	public void testQueueResources() throws Exception {
		
		List<ResourceRequest>request = new ArrayList<>();

		// only IDB default is used
		List<ResourceRequest> testRes=g.incarnateResources(request, null);
		String incarnatedQ = ResourceRequest.find(testRes, ResourceSet.QUEUE).getRequestedValue();
		assertEquals("normal", incarnatedQ);
		
		Client c = new Client();
		Queue queue = new Queue();

		// user asked for an invalid queue, disallowed by AS
		request.clear();
		c = new Client();
		queue = new Queue();
		queue.setValidQueues(new String[] {"special", "normal", "q1"});
		c.setQueue(queue);
		request.add(new ResourceRequest(ResourceSet.QUEUE, "slow"));
		
		try
		{
			testRes=g.incarnateResources(request, c);
			fail("incarnated a queue which is not allowed by AS");
		} catch (ExecutionException e) {
			String msg=e.getMessage();
			assertTrue(msg.contains("out of range"));
		}


		// user asked for an invalid queue, disallowed by IDB
		request.clear();
		request.add(new ResourceRequest(ResourceSet.QUEUE, "superfast"));
		
		try
		{
			testRes=g.incarnateResources(request, null);
			fail("incarnated a queue which is not allowed by IDB");
		} catch (ExecutionException e) {}

	}

	@Test
	public void testProjectResource() throws Exception {
		List<ResourceRequest>request = new ArrayList<>();

		//case 1 - only default is used
		List<ResourceRequest> testRes=g.incarnateResources(request, null);
		String incarnated = ResourceRequest.find(testRes,ResourceSet.PROJECT).getRequestedValue();
		assertEquals(incarnated, "NONE");

		//case 2 - user asked for a project
		request.clear();
		request.add(new ResourceRequest(ResourceSet.PROJECT, "bioinformatics"));
		
		Client c = new Client();
		testRes=g.incarnateResources(request, c);
		incarnated = ResourceRequest.find(testRes,ResourceSet.PROJECT).getRequestedValue();
		assertEquals("bioinformatics", incarnated);

		//case 3- user asked for an invalid project
		
		c = new Client();
		request.clear();
		request.add(new ResourceRequest(ResourceSet.PROJECT, "myleastfavoriteproject"));
		
		try
		{
			testRes  = g.incarnateResources(request, c);
			fail("incarnated a project which is not allowed by AS");
		} catch (ExecutionException e) {
			String msg=e.getMessage();
			assertTrue(msg.contains("out of range"));
		}
	}

	@Test
	public void testStringResource() throws Exception {
		//case 1 - only IDB default is used
		List<ResourceRequest> request = new ArrayList<>();

		List<ResourceRequest> testRes=g.incarnateResources(request, null);
		String incarnated = ResourceRequest.find(testRes,"AStringResource").getRequestedValue();
		assertEquals("test123", incarnated);

		//case 2 - user request is used
		request.add(new ResourceRequest("AStringResource", "myvalue"));
		
		testRes=g.incarnateResources(request, null);
		incarnated = ResourceRequest.find(testRes,"AStringResource").getRequestedValue();
		assertEquals("myvalue", incarnated);
		
		incarnated = ResourceRequest.findAndRemove(testRes,"AStringResource").getRequestedValue();
		assertEquals("myvalue", incarnated);
		assertNull(ResourceRequest.find(testRes,"AStringResource"));
	}

	@Test
	public void testIncarnate_Nodes_PPN() throws Exception {
		List<ResourceRequest>request = new ArrayList<>();
		request.add(new ResourceRequest(ResourceSet.NODES, "1"));
		request.add(new ResourceRequest(ResourceSet.CPUS_PER_NODE, "1"));
		List<ResourceRequest> testRes = g.incarnateResources(request,null);

		//check correct values are available
		int nodes = Integer.valueOf(ResourceRequest.find(testRes,ResourceSet.NODES).getRequestedValue());
		assertEquals(nodes,1);
		int cpusPerNode = Integer.valueOf(ResourceRequest.find(testRes,ResourceSet.CPUS_PER_NODE).getRequestedValue());
		assertEquals(cpusPerNode,1);
	}

	@Test
	public void testIncarnate_TotalCPUs_Nodes_PPN() throws Exception {
		List<ResourceRequest>request = new ArrayList<>();
		request.add(new ResourceRequest(ResourceSet.NODES, "2"));
		request.add(new ResourceRequest(ResourceSet.CPUS_PER_NODE, "8"));
		request.add(new ResourceRequest(ResourceSet.TOTAL_CPUS, "16"));
		List<ResourceRequest> testRes=g.incarnateResources(request,null);		
		assertNotNull(ResourceRequest.find(testRes,ResourceSet.NODES));
		assertNotNull(ResourceRequest.find(testRes,ResourceSet.CPUS_PER_NODE));
		assertNotNull(ResourceRequest.find(testRes,ResourceSet.TOTAL_CPUS));
	}

	@Test
	public void testIncarnate_TotalCPUS_With_Nodes() throws Exception {
		List<ResourceRequest>request = new ArrayList<>();
		request.add(new ResourceRequest(ResourceSet.NODES, "2"));
		request.add(new ResourceRequest(ResourceSet.TOTAL_CPUS, "4"));
		List<ResourceRequest> testRes = g.incarnateResources(request,null);		
		
		assertEquals(Integer.valueOf(4), Integer.valueOf(ResourceRequest.find(testRes,ResourceSet.TOTAL_CPUS).getRequestedValue()));
		assertEquals(Integer.valueOf(2), Integer.valueOf(ResourceRequest.find(testRes,ResourceSet.NODES).getRequestedValue()));
	}

	@Test
	public void testIncarnate_TotalCPUS_With_PPN() throws Exception {
		List<ResourceRequest>request = new ArrayList<>();
		request.add(new ResourceRequest(ResourceSet.CPUS_PER_NODE, "2"));
		request.add(new ResourceRequest(ResourceSet.TOTAL_CPUS, "4"));
		List<ResourceRequest> testRes = g.incarnateResources(request,null);		
		
		assertNotNull(ResourceRequest.find(testRes,ResourceSet.CPUS_PER_NODE));
		assertNotNull(ResourceRequest.find(testRes,ResourceSet.TOTAL_CPUS));
		
		assertEquals(Integer.valueOf(4), Integer.valueOf(ResourceRequest.find(testRes,ResourceSet.TOTAL_CPUS).getRequestedValue()));
		assertEquals(Integer.valueOf(2), Integer.valueOf(ResourceRequest.find(testRes,ResourceSet.CPUS_PER_NODE).getRequestedValue()));
	}


	@Test(expected = ExecutionException.class)
	public void testResourceRequestOutOfRange() throws Exception {
		List<ResourceRequest>request = new ArrayList<>();
		request.add(new ResourceRequest(ResourceSet.TOTAL_CPUS, "1024"));
		g.incarnateResources(request,null);
	}
	
	@Test
	public void testIncarnate_Nodes_with_script() throws Exception {
		List<ResourceRequest>request = new ArrayList<>();
		request.add(new ResourceRequest(ResourceSet.NODES, "1"));
		request.add(new ResourceRequest(ResourceSet.CPUS_PER_NODE, "${2*SCALE}"));
		Action a = new Action();
		ExecutionContext ec = new ExecutionContext();
		ec.getEnvironment().put("SCALE", "32");
		ApplicationInfo app = new ApplicationInfo();
		a.setApplicationInfo(app);
		app.setResourceRequest(request);
		a.setExecutionContext(ec);
		List<ResourceRequest> testRes = g.incarnateResources(a);
		//check correct values are available
		int nodes = Integer.valueOf(ResourceRequest.find(testRes,ResourceSet.NODES).getRequestedValue());
		assertEquals(nodes,1);
		int cpusPerNode = Integer.valueOf(ResourceRequest.find(testRes,ResourceSet.CPUS_PER_NODE).getRequestedValue());
		assertEquals(cpusPerNode, 64);
	}

}
