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
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ResourcesDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ResourcesType;
import org.junit.Before;
import org.junit.Test;

import de.fzj.unicore.xnjs.BaseModule;
import de.fzj.unicore.xnjs.ConfigurationSource;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.idb.IDB;
import de.fzj.unicore.xnjs.idb.Incarnation;
import de.fzj.unicore.xnjs.resources.ResourceRequest;
import de.fzj.unicore.xnjs.resources.ResourceSet;
import de.fzj.unicore.xnjs.resources.StringResource;
import de.fzj.unicore.xnjs.tsi.local.LocalTSIModule;
import eu.unicore.bugsreporter.annotation.RegressionTest;
import eu.unicore.security.Client;
import eu.unicore.security.Queue;

/**
 * tests handling of IDB resources and resource incarnation (CPUs, memory, etc)
 */
public class TestResourceIncarnation {

	private Incarnation g;
	private IDB idb;
	private JSDLParser parser;

	@Before
	public void setUp() throws Exception {
		XNJS config = new XNJS(getConfigSource()); 
		idb = config.get(IDB.class);
		parser = new JSDLParser();
		g = config.get(Incarnation.class);
	}

	protected ConfigurationSource getConfigSource() throws IOException {
		ConfigurationSource cs = new ConfigurationSource();
		cs.getProperties().put("XNJS.idbfile","src/test/resources/resources/simpleidb");
		File fileSpace=new File("target","xnjs_filespace");
		FileUtils.deleteQuietly(fileSpace);
		cs.getProperties().put("XNJS.filespace",fileSpace.getAbsolutePath());
		cs.addModule(new BaseModule(cs.getProperties()));
		cs.addModule(new LocalTSIModule(cs.getProperties()));
		return cs;
	}

	@Test
	public void testIncarnateDefaultResources() throws Exception {
		List<ResourceRequest> req=new ArrayList<ResourceRequest>();
		List<ResourceRequest> testRes=g.incarnateResources(req,null);
		System.out.println(testRes);
		ResourceRequest.removeQuietly(testRes, ResourceSet.QUEUE);
		assertNull(ResourceRequest.find(testRes,  ResourceSet.QUEUE));
		
		//check against defaults from IDB
		double cputime=Double.valueOf(ResourceRequest.find(testRes,JSDLResourceSet.RUN_TIME).getRequestedValue());
		assertEquals(cputime,3600.0,1);
		double nodes=Double.valueOf(ResourceRequest.find(testRes,JSDLResourceSet.NODES).getRequestedValue());
		assertEquals(nodes,1.0,1);
		double cpusPerNode=Double.valueOf(ResourceRequest.find(testRes,JSDLResourceSet.CPUS_PER_NODE).getRequestedValue());
		assertEquals(cpusPerNode,1.0,1);
		double mem=Double.valueOf(ResourceRequest.find(testRes,JSDLResourceSet.MEMORY_PER_NODE).getRequestedValue());
		assertEquals(mem,268435456.0,1);
	}

	@Test
	public void testIncarnateResources() throws Exception {
		ResourcesDocument doc=ResourcesDocument.Factory.newInstance();
		ResourcesType request=doc.addNewResources();
		List<ResourceRequest> defaults = idb.getDefaultPartition().getResources().getDefaults();
		int memRequest = 2* Integer.parseInt(ResourceRequest.find(defaults, ResourceSet.MEMORY_PER_NODE).getRequestedValue());
		System.out.println(memRequest);
		request.addNewIndividualPhysicalMemory().addNewExact().setDoubleValue(memRequest);

		List<ResourceRequest> testReq=parser.parseRequestedResources(request); 
		List<ResourceRequest> testRes = g.incarnateResources(testReq,null);
		double mem=Double.valueOf(ResourceRequest.find(testRes,JSDLResourceSet.MEMORY_PER_NODE).getRequestedValue());
		assertEquals(mem,memRequest,0.01);
		//check others, these should be default
		double cputime=Double.valueOf(ResourceRequest.find(testRes,JSDLResourceSet.RUN_TIME).getRequestedValue());
		assertEquals(cputime,3600.0, 0.01);
		double nodes=Double.valueOf(ResourceRequest.find(testRes,JSDLResourceSet.NODES).getRequestedValue());
		assertEquals(nodes,1.0,1);
		double cpusPerNode=Double.valueOf(ResourceRequest.find(testRes,JSDLResourceSet.CPUS_PER_NODE).getRequestedValue());
		assertEquals(cpusPerNode,1.0, 0.01);
	}

	@Test
	public void testQueueResources() throws Exception {
		
		List<ResourceRequest>request = new ArrayList<>();

		// only IDB default is used
		List<ResourceRequest> testRes=g.incarnateResources(request, null);
		String incarnatedQ = ResourceRequest.find(testRes,ResourceSet.QUEUE).getRequestedValue();
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
			testRes=g.incarnateResources(request, c);
			fail("incarnated a project which is not allowed by AS");
		} catch (ExecutionException e) {
			String msg=e.getMessage();
			assertTrue(msg.contains("out of range"));
		}
	}

	@Test
	@RegressionTest(url="https://sourceforge.net/tracker/?func=detail&aid=3323089&group_id=102081&atid=633902")
	public void testStringResource() throws Exception {
		ResourcesDocument doc;
		ResourcesType rt;
		List<ResourceRequest>request;

		//case 1 - only IDB default is used
		doc=ResourcesDocument.Factory.newInstance();
		rt=doc.addNewResources();
		request = parser.parseRequestedResources(rt);

		List<ResourceRequest> testRes=g.incarnateResources(request, null);
		String incarnated = ResourceRequest.find(testRes,"AStringResource").getRequestedValue();
		assertEquals("test123", incarnated);

		//case 2 - user request is used
		ResourceSet req=new ResourceSet();
		req.putResource(new StringResource("AStringResource", "myvalue"));
		doc=new JSDLRenderer().render(req);
		rt = doc.getResources();
		request = parser.parseRequestedResources(rt);

		testRes=g.incarnateResources(request, null);
		incarnated = ResourceRequest.find(testRes,"AStringResource").getRequestedValue();
		assertEquals("myvalue", incarnated);
	}

	@Test
	public void testIncarnate_Nodes_PPN() throws Exception {
		ResourcesDocument doc=ResourcesDocument.Factory.newInstance();
		ResourcesType rt=doc.addNewResources();

		rt.addNewTotalResourceCount().addNewExact().setDoubleValue(1.0);
		rt.addNewIndividualCPUCount().addNewExact().setDoubleValue(1.0);
		List<ResourceRequest>request;
		request = parser.parseRequestedResources(rt);

		List<ResourceRequest> testRes=g.incarnateResources(request,null);

		//check correct values are available
		double nodes=Double.valueOf(ResourceRequest.find(testRes,JSDLResourceSet.NODES).getRequestedValue());
		assertEquals(nodes,1.0, 0.1);
		double cpusPerNode=Double.valueOf(ResourceRequest.find(testRes,JSDLResourceSet.CPUS_PER_NODE).getRequestedValue());
		assertEquals(cpusPerNode,1.0, 0.1);
	}

	@Test
	public void testIncarnate_TotalCPUs_Nodes_PPN() throws Exception {
		ResourcesDocument doc=ResourcesDocument.Factory.newInstance();
		ResourcesType rt=doc.addNewResources();

		rt.addNewTotalCPUCount().addNewExact().setDoubleValue(1.0);
		rt.addNewTotalResourceCount().addNewExact().setDoubleValue(1.0);
		rt.addNewIndividualCPUCount().addNewExact().setDoubleValue(1.0);
		List<ResourceRequest> request = parser.parseRequestedResources(rt);
		List<ResourceRequest> testRes=g.incarnateResources(request,null);

		assertNotNull(ResourceRequest.find(testRes,JSDLResourceSet.NODES));
		assertNotNull(ResourceRequest.find(testRes,JSDLResourceSet.CPUS_PER_NODE));
		assertNotNull(ResourceRequest.find(testRes,JSDLResourceSet.TOTAL_CPUS));
	}

	@Test
	public void testIncarnate_TotalCPUS_With_Nodes() throws Exception {
		//create resource request
		ResourcesDocument doc=ResourcesDocument.Factory.newInstance();
		ResourcesType rt=doc.addNewResources();
		rt.addNewTotalCPUCount().addNewExact().setDoubleValue(4.0);
		rt.addNewTotalResourceCount().addNewExact().setDoubleValue(2.0);
		List<ResourceRequest> request = parser.parseRequestedResources(rt);

		List<ResourceRequest> testRes=g.incarnateResources(request,null);
		assertEquals(4.0,Double.valueOf(ResourceRequest.find(testRes,JSDLResourceSet.TOTAL_CPUS).getRequestedValue()),0.1);
		assertEquals(2.0,Double.valueOf(ResourceRequest.find(testRes,JSDLResourceSet.NODES).getRequestedValue()),0.1);
	}

	@Test
	public void testIncarnate_TotalCPUS_With_PPN() throws Exception {
		ResourcesDocument doc=ResourcesDocument.Factory.newInstance();
		ResourcesType rt=doc.addNewResources();

		rt.addNewTotalCPUCount().addNewExact().setDoubleValue(4.0);
		rt.addNewIndividualCPUCount().addNewExact().setDoubleValue(2.0);

		List<ResourceRequest> request = parser.parseRequestedResources(rt);

		List<ResourceRequest> testRes=g.incarnateResources(request,null);

		assertNotNull(ResourceRequest.find(testRes,JSDLResourceSet.CPUS_PER_NODE));
		assertNotNull(ResourceRequest.find(testRes,JSDLResourceSet.TOTAL_CPUS));
		
		assertEquals(4.0,Double.valueOf(ResourceRequest.find(testRes,JSDLResourceSet.TOTAL_CPUS).getRequestedValue()),0.01);
		assertEquals(2.0,Double.valueOf(ResourceRequest.find(testRes,JSDLResourceSet.CPUS_PER_NODE).getRequestedValue()),0.01);
	}

	@Test
	public void testIncarnate_Inconsistent_CPUs_and_Nodes() throws Exception {
		ResourcesDocument doc=ResourcesDocument.Factory.newInstance();
		ResourcesType rt=doc.addNewResources();

		rt.addNewTotalCPUCount().addNewExact().setDoubleValue(4.0);
		rt.addNewIndividualCPUCount().addNewExact().setDoubleValue(3.0);
		rt.addNewTotalResourceCount().addNewExact().setDoubleValue(2.0);

			try{
			parser.parseRequestedResources(rt);
			fail("Expected exception due to inconsistent resource spec.");
		}catch(IllegalArgumentException iae){}
	}

	@Test
	public void testResourceRequestOutOfRange() throws Exception {
		ResourcesType rt=ResourcesType.Factory.newInstance();
		double cpuRequest=200;
		rt.addNewIndividualCPUCount().addNewExact().setDoubleValue(cpuRequest);
		List<ResourceRequest> request = parser.parseRequestedResources(rt);
		try{
			g.incarnateResources(request,null);
			fail();
		}catch(ExecutionException e){}
	}

}
