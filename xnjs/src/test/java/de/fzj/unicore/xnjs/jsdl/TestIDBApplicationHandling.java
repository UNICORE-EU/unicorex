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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ApplicationType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.ggf.schemas.jsdl.x2006.x07.jsdlHpcpa.HPCProfileApplicationDocument;
import org.junit.Before;
import org.junit.Test;

import de.fzj.unicore.xnjs.BaseModule;
import de.fzj.unicore.xnjs.ConfigurationSource;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.XNJSProperties;
import de.fzj.unicore.xnjs.beans.idb.FileSystemDocument.FileSystem;
import de.fzj.unicore.xnjs.beans.idb.IDBDocument;
import de.fzj.unicore.xnjs.ems.ExecutionContext;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.idb.ApplicationInfo;
import de.fzj.unicore.xnjs.idb.ApplicationMetadata;
import de.fzj.unicore.xnjs.idb.GrounderImpl;
import de.fzj.unicore.xnjs.idb.IDB;
import de.fzj.unicore.xnjs.idb.IDBImpl;
import de.fzj.unicore.xnjs.idb.Incarnation;
import de.fzj.unicore.xnjs.idb.OptionDescription;
import de.fzj.unicore.xnjs.tsi.local.LocalTSIModule;
import eu.unicore.security.Client;

/**
 * tests handling of application incarnation and application metadata handling
 * in the IDB 
 */
public class TestIDBApplicationHandling {

	private GrounderImpl g;
	private IDB idb;
	private JSDLParser parser;
	private XNJS config;
	
	@Before
	public void setUp() throws Exception {
		config = new XNJS(getConfigSource()); 
		idb = config.get(IDB.class);
		parser=new JSDLParser();
		g = (GrounderImpl)config.get(Incarnation.class);
	}

	protected ConfigurationSource getConfigSource() throws IOException {
		ConfigurationSource cs = new ConfigurationSource();
		cs.getProperties().put("XNJS.idbfile","src/test/resources/jsdl/simpleidb");
		File fileSpace=new File("target","xnjs_filespace");
		FileUtils.deleteQuietly(fileSpace);
		cs.getProperties().put("XNJS.filespace",fileSpace.getAbsolutePath());
		cs.addModule(new BaseModule(cs.getProperties()));
		cs.addModule(new LocalTSIModule(cs.getProperties()));
		return cs;
	}

	private InputStream getResource(String name) throws Exception {
		InputStream is = getClass().getResourceAsStream(name);
		if(is==null){
			try{
				is=new FileInputStream(name);
			}catch(Exception e){}
		}
		return is;
	}

	@Test
	public void testIncarnateSimpleApp() throws Exception {
		InputStream is = getResource("src/test/resources/jsdl/date_simple.jsdl");
		assertNotNull(is);
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.parse(is);
		ApplicationInfo app=parser.parseApplicationInfo(jdd);
		ApplicationInfo pa=g.incarnateApplication(app, new Client());
		assertEquals("/bin/date",pa.getExecutable());
		assertEquals(1, pa.getArguments().size());
	}

	@Test
	public void testIgnoreWhitespace() throws Exception {
		InputStream is = getResource("src/test/resources/jsdl/date_simple.jsdl");
		assertNotNull(is);
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.parse(is);
		ApplicationInfo app=parser.parseApplicationInfo(jdd);
		ApplicationInfo pa=g.incarnateApplication(app, new Client());
		assertEquals("/bin/date",pa.getExecutable());
		assertEquals(1, pa.getArguments().size());
		assertEquals("TEST",pa.getArguments().get(0));
	}

	@Test
	public void testIncarnateApp() throws Exception {
		InputStream is = getResource("src/test/resources/jsdl/ex_posix.jsdl");
		assertNotNull(is);
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.parse(is);
		ApplicationInfo app=parser.parseApplicationInfo(jdd);
		ApplicationInfo pa=g.incarnateApplication(app, new Client());
		assertEquals("/bin/date",pa.getExecutable());
		assertEquals("bar",pa.getEnvironment().get("foo"));
	}

	@Test
	public void testIncarnateApp2() throws Exception {
		InputStream is = getResource("src/test/resources/jsdl/date.jsdl");
		assertNotNull(is);
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.parse(is);
		ApplicationInfo app=parser.parseApplicationInfo(jdd);
		ApplicationInfo pa=g.incarnateApplication(app, new Client());
		assertEquals("/bin/date",pa.getExecutable());
		assertEquals(3, pa.getArguments().size());
	}

	@Test
	public void testIncarnateAppCaseSensitive() throws Exception {
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.newInstance();
		ApplicationType app=jdd.addNewJobDefinition().addNewJobDescription().addNewApplication();
		app.setApplicationName("Python Script");
		ApplicationInfo orig=parser.parseApplicationInfo(jdd);
		ApplicationInfo pa=g.incarnateApplication(orig, new Client());
		if(pa.getExecutable()==null)fail("Application was not incarnated properly");
		assertEquals("/usr/bin/python",pa.getExecutable());
		//app names are case sensitive
		orig.setName("Python script");
		try{
			pa=g.incarnateApplication(orig, new Client());
			fail("Application was not incarnated properly");
		}catch(ExecutionException ex){
			//OK

		}
	}

	@Test
	public void testIncarnateHPCPApplication() throws Exception {
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.newInstance();
		HPCProfileApplicationDocument pad=HPCProfileApplicationDocument.Factory.newInstance();
		pad.addNewHPCProfileApplication().addNewExecutable().setStringValue("/bin/date");
		assertTrue(pad.toString().contains("/bin/date"));
		jdd.addNewJobDefinition().addNewJobDescription().addNewApplication().set(pad);
		ApplicationInfo orig=parser.parseApplicationInfo(jdd);
		ApplicationInfo pa=g.incarnateApplication(orig, new Client());
		assertEquals("/bin/date",pa.getExecutable());
	}

	@Test
	public void testParseArgNameAndIncarnatedValue()throws Exception{
		String arg="-foo: $VISUAL?";
		OptionDescription a = IDBImpl.parseArgument(arg);
		assertNotNull(a);
		assertEquals("VISUAL",a.getName());
		
		arg=" -foo: $VISUAL?\n";
		a = IDBImpl.parseArgument(arg);
		assertNotNull(a);
		assertEquals("VISUAL",a.getName());
		
		arg="x$NOTMUCH?";
		a = IDBImpl.parseArgument(arg);
		assertNotNull(a);
		assertEquals("NOTMUCH",a.getName());
		
		arg="$LESS?";
		a = IDBImpl.parseArgument(arg);
		assertNotNull(a);
		assertEquals("LESS",a.getName());
	}

	@Test
	public void testPosixArgumentMetadataExtract(){
		String text="-v$VERBOSE?";
		OptionDescription arg = IDBImpl.parseArgument(text);
		assertNotNull(arg);
		assertEquals("VERBOSE", arg.getName());

		text="-np   ${NP}";
		arg = IDBImpl.parseArgument(text);
		assertNotNull(arg);
		assertEquals("NP", arg.getName());
	}

	@Test
	public void testForbiddenUserSpecifiedExecutable() throws Exception {
		config.getXNJSProperties().setProperty(XNJSProperties.ALLOW_USER_EXECUTABLE, "false");
		InputStream is = getResource("src/test/resources/jsdl/date2.jsdl");
		assertNotNull(is);
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.parse(is);
		try{
			ApplicationInfo app=parser.parseApplicationInfo(jdd);
			g.incarnateApplication(app, new Client());
			fail("Expected exception here");
		}catch(ExecutionException ex){
			assertTrue(ex.getMessage().contains("Cannot execute"));
		}
		config.getXNJSProperties().setProperty(XNJSProperties.ALLOW_USER_EXECUTABLE, "true");
	}


	@Test
	public void testBasicStorageProcessing() throws Exception {
		InputStream is = getResource("src/test/resources/jsdl/simpleidb");
		assertNotNull(is);
		IDBDocument idb=IDBDocument.Factory.parse(is);
		FileSystem[] fs=idb.getIDB().getTargetSystemProperties().getFileSystemArray();
		assertEquals(fs.length,2); 
		FileSystem fs1=fs[0];
		assertEquals(fs1.getName(),"Test1");
		assertEquals(fs1.getIncarnatedPath(),"/tmp/test1");
		FileSystem fs2=fs[1];
		assertEquals(fs2.getName(),"Test2");
		assertEquals(fs2.getIncarnatedPath(),"/tmp/test2");
		ExecutionContext ec=new ExecutionContext(UUID.randomUUID().toString());
		ec.setWorkingDirectory("/");
		String p1=g.incarnatePath("foo","Test1",ec,null);
		assertEquals(p1,"/tmp/test1/foo");

		try {
			g.incarnatePath("foo", "Gaga", ec,null);
			fail("Expected Exception here: filesystem exists");
		} catch (ExecutionException e) {
			//OK
		}
	}

	@Test
	public void testNullFilesystem() throws Exception {
		InputStream is = getResource("src/test/resources/jsdl/simpleidb");
		assertNotNull(is);
		ExecutionContext ec=new ExecutionContext(UUID.randomUUID().toString());
		ec.setWorkingDirectory("/");
		String p=g.incarnatePath("foo", null, ec,null);
		assertEquals(p,"/foo");
	}

	@Test
	public void testConditionalArguments() throws Exception {
		//test job contains app name + some environment variables
		InputStream is = getResource("src/test/resources/jsdl/testConditional.jsdl");
		assertNotNull(is);
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.parse(is);
		ApplicationInfo app=parser.parseApplicationInfo(jdd);
		ApplicationInfo pa=g.incarnateApplication(app, new Client());
		assertTrue(pa.getArguments().contains("foo$Conditional"));
		assertTrue(pa.getArguments().contains("$Conditional2"));
		assertFalse(pa.getArguments().contains("Conditional3"));

		assertTrue(pa.getArguments().contains("$FixedArg"));

		//do again to assert we do not modify the applications from the IDB

		//this job contains just the app name
		is = getResource("src/test/resources/jsdl/testConditional2.jsdl");
		assertNotNull(is);
		jdd=JobDefinitionDocument.Factory.parse(is);
		app=parser.parseApplicationInfo(jdd);
		pa=g.incarnateApplication(app, new Client());

		assertFalse(pa.getArguments().contains("$Conditional2"));
		assertFalse(pa.getArguments().contains("foo$Conditional"));
		assertFalse(pa.getArguments().contains("Conditional3"));
		assertTrue(pa.getArguments().contains("$FixedArg"));
	}

	@Test
	public void testArgExtract1(){
		String t1="foo${ARG1}";
		assertTrue(GrounderImpl.extractArgumentName(t1).equals("ARG1"));
		t1="foo${ARG1}bar";
		assertTrue(GrounderImpl.extractArgumentName(t1).equals("ARG1"));
		t1="foo$ARG1";
		assertTrue(GrounderImpl.extractArgumentName(t1).equals("ARG1"));
		t1="foo%ARG1%bar";
		assertTrue(GrounderImpl.extractArgumentName(t1).equals("ARG1"));
		t1="-Dbla.blubb=$ARG1";
		assertTrue(GrounderImpl.extractArgumentName(t1).equals("ARG1"));
		t1="-Dbla.blubb=%ARG1%?";
		assertTrue(GrounderImpl.extractArgumentName(t1).equals("ARG1"));
		t1="-f$ARG1,100";
		assertTrue(GrounderImpl.extractArgumentName(t1).equals("ARG1"));
		t1="-f%ARG1%,100";
		assertTrue(GrounderImpl.extractArgumentName(t1).equals("ARG1"));
		t1="-f${ARG1},100";
		assertTrue(GrounderImpl.extractArgumentName(t1).equals("ARG1"));

	}

	@Test
	public void testTextInfos(){
		assertEquals("/usr/share/lib/cpmd.so",idb.getTextInfo("CPMD_LIBRARY_PATH"));
		assertEquals("/opt/visit/proxy",idb.getTextInfo("VISIT_PROXY"));						
	}

	@Test
	public void testInteractiveFlag() throws Exception {
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.newInstance();
		ApplicationType app=jdd.addNewJobDefinition().addNewJobDescription().addNewApplication();
		app.setApplicationName("TestInteractive");
		ApplicationInfo orig=parser.parseApplicationInfo(jdd);
		ApplicationInfo pa=g.incarnateApplication(orig, new Client());
		assertEquals("true",pa.getEnvironment().get("UC_PREFER_INTERACTIVE_EXECUTION"));
	}

	@Test
	public void t()throws Exception{
		Collection<ApplicationInfo> apps=idb.getApplications(null);
		String name="TestAppWithMetadata";
		ApplicationInfo app=null;
		for(ApplicationInfo a: apps){
			if(a.getName().equals(name)){
				app=a;
				break;
			}
		}
		if(app==null)fail("Expected application <"+name+"> in idb");
	
		ApplicationMetadata o=app.getMetadata();
		assertNotNull(o);
		
		assertEquals(3,o.getOptions().size());

		OptionDescription v=o.getOptions().get(0);
		assertEquals("VERBOSE",v.getName());
		
//		ArgumentMetadata vMeta=v.getArgumentMetadata();
//		assertEquals("boolean",vMeta.getType().toString());
//		assertEquals("Verbose Execution",vMeta.getDescription());
//		assertNull(vMeta.getDefault());
//		assertEquals(3, vMeta.getDependsOnArray().length);
//		assertEquals(1, vMeta.getExcludesArray().length);
//		assertEquals(2, vMeta.getValidValueArray().length);
//		assertFalse(vMeta.getIsMandatory());
//		assertTrue(vMeta.getIsEnabled());
//
//		//check "visual" argument
//		Argument v2=md.getArgumentArray()[1];
//		assertEquals("VISUAL",v2.getName());
//		assertEquals("-combo: ",v2.getIncarnatedValue());
//		ArgumentMetadata v2Meta=v2.getArgumentMetadata();
//		assertEquals("choice",v2Meta.getType().toString());
//		assertEquals(6,v2Meta.getValidValueArray().length);
//		assertTrue(v2Meta.getIsMandatory());
//		assertEquals("test",v2Meta.getMimeType());
//
//		//check "other" argument
//		Argument v3=md.getArgumentArray()[2];
//		assertEquals("OTHER",v3.getName());
//		assertEquals("",v3.getIncarnatedValue());
//		assertEquals("string",v3.getArgumentMetadata().getType().toString());
//
//
//		System.out.println(v3);
	}

}
