package de.fzj.unicore.xnjs.json;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
import de.fzj.unicore.xnjs.io.impl.AuthToken;
import de.fzj.unicore.xnjs.io.impl.BearerToken;
import de.fzj.unicore.xnjs.io.impl.UsernamePassword;
import de.fzj.unicore.xnjs.resources.BooleanResource;
import de.fzj.unicore.xnjs.resources.DoubleResource;
import de.fzj.unicore.xnjs.resources.IntResource;
import de.fzj.unicore.xnjs.resources.Resource;
import de.fzj.unicore.xnjs.resources.ResourceRequest;
import de.fzj.unicore.xnjs.resources.ResourceSet;
import de.fzj.unicore.xnjs.resources.ValueListResource;
import de.fzj.unicore.xnjs.util.JSONUtils;

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
		
		ApplicationInfo app = JSONParser.parseApplicationInfo(new JSONObject(json));
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
	public void testParseJSONApplicationWithEmptyFields() throws Exception {
		String json = "{"
				+ "Name: 'test',"
				+ "Version: '1.0',"
				+ "Description: 'some test app',"
				+ "PreCommand: 'unzip $INPUT',"
				+ "Prologue: 'module load test123',"
				+ "Executable: '/bin/test',"
				+ "Arguments: [],"
				+ "Environment: {},"
				+ "Parameters: {},"
				+ "Resources: {},"
				+ "}";
		ApplicationInfo app = JSONParser.parseApplicationInfo(new JSONObject(json));
		assertEquals("test", app.getName());
		assertEquals("1.0", app.getVersion());
		assertEquals("some test app", app.getDescription());
		assertEquals("unzip $INPUT", app.getPreCommand());
		assertEquals("module load test123", app.getPrologue());
		List<String>args = app.getArguments();
		assertEquals(0, args.size());
		Map<String,String> env = app.getEnvironment();
		assertEquals(0, env.size());
		ApplicationMetadata meta = app.getMetadata();
		System.out.println(meta);
		List<ResourceRequest>rrs = app.getResourceRequests();
		assertEquals(0, rrs.size());
	}

	@Test
	public void testParseJSONApplication2() throws Exception {
		String json = "{"
				+ "Name: 'test',"
				+ "Version: '1.0',"
				+ "Prologue: ['module load test123', 'module load abc'],"
				+ "Executable: '/bin/test',"
				+ "PostCommand: ['zip $OUTPUT *.dat', 'md5sum $OUTPUT > $OUTPUT.md5'],"
				+ "}";
		
		ApplicationInfo app = JSONParser.parseApplicationInfo(new JSONObject(json));
		assertEquals("module load test123\nmodule load abc\n", app.getPrologue());
		assertEquals("zip $OUTPUT *.dat\nmd5sum $OUTPUT > $OUTPUT.md5\n", app.getPostCommand());
	}

	@Test
	public void testParseSubmittedApplication() throws Exception {
		String json = "{"
				+ "ApplicationName: 'test',"
				+ "ApplicationVersion: '1.0',"
				+ "User precommand: 'unzip $INPUT',"
				+ "Arguments: ['-p $PARAM?','2','3'],"
				+ "Environment: { 'X':'Y', 'A':'B', },"
				+ "Job type: ON_LOGIN_NODE,"
				+ "FailOnNonZeroExitCode: false,"
				+ "User postcommand: 'zip $OUTPUT *.dat',"
				+ "Parameters: { INPUT: 'input.zip',"
				+               "OUTPUT: 'output.zip',"
				+               "PARAM: 'lax',"
				+ "},"
				+ "Resources: { Nodes: 2},"
				+ "}";
		ApplicationInfo app = JSONParser.parseSubmittedApplication(new JSONObject(json));
		assertEquals("test", app.getName());
		assertEquals("1.0", app.getVersion());
		assertEquals("unzip $INPUT", app.getUserPreCommand());
		assertEquals("zip $OUTPUT *.dat", app.getUserPostCommand());
		List<String>args = app.getArguments();
		assertEquals(3, args.size());
		assertTrue(args.contains("-p $PARAM?"));assertTrue(args.contains("2"));assertTrue(args.contains("3"));
		Map<String,String> env = app.getEnvironment();
		assertEquals(2 + 3, env.size());
		assertEquals("B", env.get("A"));
		assertEquals("Y", env.get("X"));
		assertTrue(app.isRunOnLoginNode());
		assertFalse(app.ignoreNonZeroExitCode());
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
				"				GPUsPerNode: {" + 
				"		    		'Range'  : '0-16'," + 
				"				}," +
				"				CPUsPerNode: '0-16'," +
				"				GarglesPerCPU: '1-2:2'," +
				
				"               Runtime: '1-24h:30m',"+
				"               MemoryPerNode: '0-1024M',"+
				"               QoS: {"+
				"		    		'Type'  : 'CHOICE'," + 
				"		    		'AllowedValues' : ['gold','silver','bronze']" + 
				"               },"+
				"               FloatyThing: {"+
				"		    		'Type'  : 'DOUBLE'," + 
				"		    		'Range' : '0-2.718'" + 
				"               },"+
				"               ValuedUser: {"+
				"		    		'Type'  : 'BOOLEAN'," +
				"		    		'Description'  : 'Can write decent bug reports'," +
				"		    		'Default'  : 'true'" +
				"               },"+
				"               NodeType: {"+
				"		    		'Type'  : 'CHOICE'," + 
				"		    		'AllowedValues' : ['intel','amd','nvidia']" + 
				"               },"+
				"				}," + 
				"	    	}," + 
				"		}," + 
				"    },}";
		JSONObject o = new JSONObject(json);
		Partition p = JSONParser.parsePartition("normal", o.getJSONObject("Partitions").getJSONObject("normal"));
		System.out.println(p);
		Resource r = p.getResources().getResource("Nodes");
		assertEquals(Long.valueOf(1),((IntResource)r).getLower());
		System.out.println(r);
		r = p.getResources().getResource("GPUsPerNode");
		assertNotNull(r);
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
		System.out.println(r);
		assertEquals(Long.valueOf(1024*1024*1024),((IntResource)r).getUpper());
		assertEquals("Debian",p.getOperatingSystem());
		r = p.getResources().getResource("QoS");
		assertTrue( ((ValueListResource)r).isInRange("gold"));
		r = p.getResources().getResource("FloatyThing");
		assertTrue( ((DoubleResource)r).isInRange("1.23"));
		System.out.println(r);
		r = p.getResources().getResource("ValuedUser");
		assertTrue( ((BooleanResource)r).isInRange("false"));
		assertNotNull(r.getDescription());
		assertNotNull(r.getCategory());
		System.out.println(r);
		r = p.getResources().getResource("NodeType");
		assertTrue( ((ValueListResource)r).isInRange("nvidia"));
		assertNotNull(r.getCategory());
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
		
		String t1 = JSONUtils.readMultiLine("singleLine", null, idb);
		assertEquals("test123", t1);
		String t2 = JSONUtils.readMultiLine("multiLine", null, idb);
		assertEquals("foo\nbar\n", t2);
				
	}

	@Test
	public void testParseResourceRequest() throws Exception {
		JSONObject jrr = new JSONObject();
		jrr.put("Runtime", "2h");
		jrr.put("Memory", "1k");
		jrr.put("Project", "project123");
		jrr.put("Queue", "test");
		jrr.put("Reservation", "123");
		jrr.put("GPUsPerNode", "2");
		jrr.put("Exclusive", "True");
		List<ResourceRequest> rr = JSONParser.parseResourceRequest(jrr);
		System.out.println(rr);
		assertEquals("7200", ResourceRequest.find(rr, ResourceSet.RUN_TIME).getRequestedValue());
		assertEquals("1024", ResourceRequest.find(rr, ResourceSet.MEMORY_PER_NODE).getRequestedValue());
		assertEquals("test", ResourceRequest.find(rr, ResourceSet.QUEUE).getRequestedValue());
		assertEquals("project123", ResourceRequest.find(rr, ResourceSet.PROJECT).getRequestedValue());
		assertEquals("123", ResourceRequest.find(rr, ResourceSet.RESERVATION_ID).getRequestedValue());	
		assertEquals("2", ResourceRequest.find(rr, ResourceSet.GPUS_PER_NODE).getRequestedValue());	
		assertEquals("true", ResourceRequest.find(rr, ResourceSet.EXCLUSIVE).getRequestedValue());	
	}
	
	@Test
	public void testParseStageIn() throws Exception {
		JSONObject spec = new JSONObject();
		spec.put("From", "http://some-url");
		spec.put("To", "file.txt");
		JSONObject p = new JSONObject();
		p.put("test", "123");
		spec.put("ExtraParameters", p);
		DataStageInInfo dsi = JSONParser.parseStageIn(spec);
		assertEquals("file.txt", dsi.getFileName());
		assertEquals(1, dsi.getSources().length);
		assertEquals(1, dsi.getExtraParameters().size());
		assertEquals("123", dsi.getExtraParameters().get("test"));
	}
	
	@Test
	public void testParseStageInCredentials() throws Exception {
		JSONObject spec = new JSONObject();
		spec.put("From", "http://some-url");
		spec.put("To", "file.txt");
		JSONObject cred = new JSONObject();
		cred.put("Username", "demo");
		cred.put("Password", "foo");
		spec.put("Credentials", cred);
		DataStageInInfo dsi = JSONParser.parseStageIn(spec);
		assertEquals("file.txt", dsi.getFileName());
		assertEquals(1, dsi.getSources().length);
		assertTrue(dsi.getCredentials() instanceof UsernamePassword);

		cred = new JSONObject();
		cred.put("Token", "foo");
		spec.put("Credentials", cred);
		dsi = JSONParser.parseStageIn(spec);
		assertTrue(dsi.getCredentials() instanceof AuthToken);

		cred = new JSONObject();
		cred.put("BearerToken", "foo");
		spec.put("Credentials", cred);
		dsi = JSONParser.parseStageIn(spec);
		assertTrue(dsi.getCredentials() instanceof BearerToken);
	}

	@Test
	public void testParseInlineStageIn() throws Exception {
		JSONObject spec = new JSONObject();
		spec.put("Data", "some inline data");
		spec.put("To", "file.txt");
		DataStageInInfo dsi = JSONParser.parseStageIn(spec);
		assertEquals("file.txt", dsi.getFileName());
		assertEquals(1, dsi.getSources().length);
		assertTrue(dsi.getSources()[0].toString().startsWith("inline:"));
	}

	@Test
	public void testParseStageOut() throws Exception {
		JSONObject spec = new JSONObject();
		spec.put("To", "http://some-url");
		spec.put("From", "file.txt");
		JSONObject p = new JSONObject();
		p.put("test", "123");
		spec.put("ExtraParameters", p);
		DataStageOutInfo dso = JSONParser.parseStageOut(spec);
		assertEquals("file.txt", dso.getFileName());
		assertEquals(new URI("http://some-url"), dso.getTarget());
		assertEquals(1, dso.getExtraParameters().size());
		assertEquals("123", dso.getExtraParameters().get("test"));
	}

	@Test
	public void testParseNotificationURL() throws Exception {
		JSONObject job = new JSONObject();
		assertTrue(JSONParser.parseNotificationURLs(job).isEmpty());
		job.put("Notification", "http://some-url");
		assertEquals("http://some-url", JSONParser.parseNotificationURLs(job).get(0));
		job.clear();
		JSONObject spec = new JSONObject();
		spec.put("URL", "http://some-url");
		job.put("NotificationSettings", spec);
		assertEquals("http://some-url", JSONParser.parseNotificationURLs(job).get(0));
	}

	@Test
	public void testParseNotificationStates() throws Exception {
		JSONObject job = new JSONObject();
		List<String>s = JSONParser.parseNotificationTriggers(job);
		assertNull(s);
		JSONObject spec = new JSONObject();
		JSONArray states = new JSONArray();
		states.put("RUNNING");
		states.put("POSTPROCESSING");
		spec.put("status", states);
		job.put("NotificationSettings", spec);
		s = JSONParser.parseNotificationTriggers(job);
		assertTrue(s.contains("RUNNING"));
		assertTrue(s.contains("POSTPROCESSING"));
		assertFalse(s.contains("DONE"));
	}

	@Test
	public void testParseNotificationBSSStates() throws Exception {
		JSONObject job = new JSONObject();
		List<String>s = JSONParser.parseNotificationBSSTriggers(job);
		assertEquals(s.size(), 0);
		
		JSONObject spec = new JSONObject();
		JSONArray states = new JSONArray();
		states.put("CONFIGURING");
		spec.put("bssStatus", states);
		job.put("NotificationSettings", spec);
		s = JSONParser.parseNotificationBSSTriggers(job);
		assertNull(JSONParser.parseNotificationTriggers(job));
		assertEquals(s.size(), 1);
		assertTrue(s.contains("CONFIGURING"));
	}
}
