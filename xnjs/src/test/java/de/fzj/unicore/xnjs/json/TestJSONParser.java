package de.fzj.unicore.xnjs.json;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import de.fzj.unicore.xnjs.idb.ApplicationInfo;
import de.fzj.unicore.xnjs.idb.ApplicationMetadata;
import de.fzj.unicore.xnjs.idb.Partition;
import de.fzj.unicore.xnjs.io.DataStageInInfo;
import de.fzj.unicore.xnjs.io.DataStageOutInfo;
import de.fzj.unicore.xnjs.resources.IntResource;
import de.fzj.unicore.xnjs.resources.Resource;
import de.fzj.unicore.xnjs.resources.ResourceRequest;
import de.fzj.unicore.xnjs.resources.ResourceSet;
import de.fzj.unicore.xnjs.resources.ValueListResource;

public class TestJSONParser {

	@Test
	public void testParseJSONApplication() throws Exception {
		String json = "{"
				+ "Name: 'test',"
				+ "Version: '1.0',"
				+ "Description: 'some test app',"
				+ "PreCommand: 'unzip $INPUT',"
				+ "Prologue: 'module load test123',"
				+ "Executable: '/bin/test',"
				+ "Arguments: ['-p $PARAM?','2','3'],"
				+ "Environment: { 'X':'Y', 'A':'B', },"
				+ "RunOnLoginNode: true,"
				+ "FailOnNonZeroExitCode: false,"
				+ "PostCommand: 'zip $OUTPUT *.dat',"
				+ "Parameters: { INPUT: { Description: 'Input files (zip archive)', Type: FILENAME},"
				+               "OUTPUT: { Description: 'Output files archive', Type: FILENAME},"
				+               "PARAM: { Description: 'Some setting', Type: String},"
				+ "},"
				+ "Resources: { Nodes: 2},"
				+ "}";
		
		ApplicationInfo app = new JSONParser().parseApplicationInfo(new JSONObject(json));
		assertEquals("test", app.getName());
		assertEquals("1.0", app.getVersion());
		assertEquals("some test app", app.getDescription());
		assertEquals("unzip $INPUT", app.getPreCommand());
		assertEquals("module load test123", app.getPrologue());
		assertEquals("zip $OUTPUT *.dat", app.getPostCommand());
		List<String>args = app.getArguments();
		assertEquals(3, args.size());
		assertTrue(args.contains("-p $PARAM?"));assertTrue(args.contains("2"));assertTrue(args.contains("3"));
		Map<String,String> env = app.getEnvironment();
		assertEquals(2, env.size());
		assertEquals("B", env.get("A"));
		assertEquals("Y", env.get("X"));
		assertTrue(app.isRunOnLoginNode());
		assertFalse(app.ignoreNonZeroExitCode());
		ApplicationMetadata meta = app.getMetadata();
		assertEquals("INPUT:Input files (zip archive):FILENAME", meta.getOptions().get(0).toString());
		assertEquals("OUTPUT:Output files archive:FILENAME", meta.getOptions().get(1).toString());
		assertEquals("PARAM:Some setting:STRING", meta.getOptions().get(2).toString());
		System.out.println(meta);
		List<ResourceRequest>rrs = app.getResourceRequests();
		assertEquals(1, rrs.size());
		ResourceRequest rr = rrs.get(0);
		assertEquals("Nodes", rr.getName());
		assertEquals("2", rr.getRequestedValue());
	}
	
	@Test
	public void testParsePartition() throws Exception {
		String json = "{ 'Partitions' : {" + 
				"		'normal': {" + 
				"	    	'Description' : 'Default partition'," + 
				"	    	'OperatingSystem' : 'Debian'," + 
				"	    	'Resources' : {" + 
				"				Nodes: {" + 
				"		    		'Range'  : '1-8'," + 
				"		    		'Default' : '1'," + 
				"				}," + 
				"				GPUS: {" + 
				"		    		'Range'  : '0-16'," + 
				"				}," +
				"				CPUsPerNode: '0-16'," +
				"				GarglesPerCPU: '1-2:2'," +
				
				"               Runtime: '0-24h:30m',"+
				"               MemoryPerNode: '0-1024M',"+
				"               QoS: {"+
				"		    		'Type'  : 'CHOICE'," + 
				"		    		'AllowedValues' : ['gold','silver','bronze']" + 
				"               },"+
				"				}," + 
				"	    	}," + 
				"		}," + 
				"    },}";
		JSONObject o = new JSONObject(json);
		Partition p = new JSONParser().parsePartition("normal", o.getJSONObject("Partitions").getJSONObject("normal"));
		System.out.println(p);
		Resource r = p.getResources().getResource("Nodes");
		assertEquals(Long.valueOf(1),((IntResource)r).getLower());
		System.out.println(r);
		r = p.getResources().getResource("GPUS");
		System.out.println(r);
		r = p.getResources().getResource("CPUsPerNode");
		assertEquals(Long.valueOf(16),((IntResource)r).getUpper());
		System.out.println(r);
		r = p.getResources().getResource("GarglesPerCPU");
		assertEquals("2",r.getStringValue());
		System.out.println(r);
		r = p.getResources().getResource("Runtime");
		assertEquals(String.valueOf(30*60),r.getStringValue());
		System.out.println(r);
		r = p.getResources().getResource("MemoryPerNode");
		assertNotNull(r);
		System.out.println(r);
		assertEquals(Long.valueOf(1024*1024*1024),((IntResource)r).getUpper());
		assertEquals("Debian",p.getOperatingSystem());
		r = p.getResources().getResource("QoS");
		assertNotNull(r);
		assertTrue( ((ValueListResource)r).isInRange("gold"));
		System.out.println(r);
	}
	
	@Test
	public void testTemplates() throws Exception {
		JSONObject idb = new JSONObject();
		idb.put("singleLine", "test123");
		JSONArray array = new JSONArray();
		array.put("foo");
		array.put("bar");
		idb.put("multiLine", array);
		
		String t1 = new JSONParser().parseScriptTemplate("singleLine", idb);
		assertEquals("test123", t1);
	}

	@Test
	public void testParseResourceRequest() throws Exception {
		JSONObject jrr = new JSONObject();
		jrr.put("Runtime", "2h");
		jrr.put("Memory", "1k");
		jrr.put("Project", "project123");
		jrr.put("Queue", "test");
		jrr.put("Reservation", "123");
		List<ResourceRequest> rr = new JSONParser().parseResourceRequest(jrr);
		System.out.println(rr);
		assertEquals("7200", ResourceRequest.find(rr, ResourceSet.RUN_TIME).getRequestedValue());
		assertEquals("1024", ResourceRequest.find(rr, ResourceSet.MEMORY_PER_NODE).getRequestedValue());
		assertEquals("test", ResourceRequest.find(rr, ResourceSet.QUEUE).getRequestedValue());
		assertEquals("project123", ResourceRequest.find(rr, ResourceSet.PROJECT).getRequestedValue());
		assertEquals("123", ResourceRequest.find(rr, ResourceSet.RESERVATION_ID).getRequestedValue());	
	}
	
	@Test
	public void testParseStageIn() throws Exception {
		JSONObject spec = new JSONObject();
		spec.put("From", "http://some-url");
		spec.put("To", "file.txt");
		DataStageInInfo dsi = new JSONParser().parseStageIn(spec);
		assertEquals("file.txt", dsi.getFileName());
		assertEquals(1, dsi.getSources().length);
	}
	
	@Test
	public void testParseStageOut() throws Exception {
		JSONObject spec = new JSONObject();
		spec.put("To", "http://some-url");
		spec.put("From", "file.txt");
		DataStageOutInfo dso = new JSONParser().parseStageOut(spec);
		assertEquals("file.txt", dso.getFileName());
		assertEquals(new URI("http://some-url"), dso.getTarget());
	}
}
