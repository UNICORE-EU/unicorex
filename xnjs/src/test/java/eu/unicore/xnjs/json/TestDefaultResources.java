package eu.unicore.xnjs.json;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.unicore.xnjs.BaseModule;
import eu.unicore.xnjs.ConfigurationSource;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.idb.IDB;
import eu.unicore.xnjs.idb.Incarnation;
import eu.unicore.xnjs.idb.Partition;
import eu.unicore.xnjs.resources.ResourceRequest;
import eu.unicore.xnjs.resources.ResourceSet;
import eu.unicore.xnjs.tsi.local.LocalTSIModule;

/**
 * tests handling of incarnation (CPUs, memory, etc) when IDB does not define any queues
 */
public class TestDefaultResources {

	private Incarnation g;

	private IDB idb;

	@BeforeEach
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
