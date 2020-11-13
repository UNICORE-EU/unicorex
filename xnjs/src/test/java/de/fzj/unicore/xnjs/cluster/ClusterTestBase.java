package de.fzj.unicore.xnjs.cluster;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.h2.tools.Server;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;

import com.google.inject.AbstractModule;

import de.fzj.unicore.persist.PersistenceProperties;
import de.fzj.unicore.xnjs.BaseModule;
import de.fzj.unicore.xnjs.ConfigurationSource;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.XNJSProperties;
import de.fzj.unicore.xnjs.persistence.IActionStoreFactory;
import de.fzj.unicore.xnjs.persistence.JDBCActionStoreFactory;
import de.fzj.unicore.xnjs.tsi.local.LocalTSIModule;
import de.fzj.unicore.xnjs.util.IOUtils;

/**
 * Base class for testing clustered operation.<br/>
 * Sets up two xnjs instances and a shared database
 * 
 * @author schuller
 */
@Ignore
public class ClusterTestBase {

	protected static XNJS xnjs1;
    
	protected static XNJS xnjs2;
    
	protected static String databaseDir;
	
	protected static File dataDir;
			
	//h2 url
	protected static String url="tcp://localhost:9092";
	
	@BeforeClass
	public static void setUp() throws Exception {
		databaseDir=new File("target","xnjs_state").getAbsolutePath();

		dataDir = new File("target/xnjs_test_"+System.currentTimeMillis());
		
		FileUtils.deleteQuietly(new File(databaseDir));
		FileUtils.deleteQuietly(dataDir);
		new File(databaseDir).mkdirs();
		startH2();
		setUp1();
		setUp2();
	}

	public static void setUp1() throws Exception {
		xnjs1 = new XNJS(getConfigurationSource1());
		xnjs1.start();
	}
	
	public static void setUp2() throws Exception {
		xnjs2 = new XNJS(getConfigurationSource2());
		xnjs2.start();
	}
	
	@AfterClass
	public static void shutDown()throws Exception{
		xnjs1.stop();
		if(xnjs2!=null)xnjs2.stop();
		Server.shutdownTcpServer(url, "", true, false);
		FileUtils.deleteQuietly(dataDir);
	}

	protected static ConfigurationSource getConfigurationSource1() throws IOException {
		ConfigurationSource cs = new ConfigurationSource();
		addProperties1(cs);
		addModules(cs);
		return cs;
	}

	protected static ConfigurationSource getConfigurationSource2() throws IOException {
		ConfigurationSource cs = new ConfigurationSource();
		addProperties2(cs);
		addModules(cs);
		return cs;
	}
	
	protected static void addModules(ConfigurationSource cs){
		cs.addModule(new BaseModule(cs.getProperties()));
		cs.addModule(new LocalTSIModule(cs.getProperties()));
		cs.addModule(getPersistenceModule());
	}
	
	protected static AbstractModule getPersistenceModule(){
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(IActionStoreFactory.class).to(JDBCActionStoreFactory.class);
			}
		};
	}
	
	/**
	 * provide configuration for the XNJS-1 instance
	 */
	protected static void addProperties1(ConfigurationSource cs){
		addCommonProperties(cs);
		Properties props = cs.getProperties();
		props.put(PersistenceProperties.PREFIX+PersistenceProperties.DB_CLUSTER_CONFIG, 
				"src/test/resources/cluster/cluster1.xml");
	}

	/**
	 * provide configuration for the XNJS-2 instance
	 */
	protected static void addProperties2(ConfigurationSource cs){
		addCommonProperties(cs);
		Properties props = cs.getProperties();
		props.put(PersistenceProperties.PREFIX+PersistenceProperties.DB_CLUSTER_CONFIG, 
				"src/test/resources/cluster/cluster2.xml");
	}
	
	protected static void addCommonProperties(ConfigurationSource cs){
		Properties props = cs.getProperties();
		props.put("XNJS.idbfile","src/test/resources/ems/simpleidb");
		String dir=dataDir.getAbsolutePath();
		props.put("XNJS.filespace",dir);
		props.put(PersistenceProperties.PREFIX+PersistenceProperties.H2_SERVER_MODE,"true");
		props.put("persistence.directory",databaseDir);
		props.put("persistence.port","9092");
		props.put(PersistenceProperties.PREFIX+PersistenceProperties.DB_LOCKS_DISTRIBUTED,"true");
		props.put("XNJS."+XNJSProperties.XNJSWORKERS,"1");
	}

	protected static void startH2()throws Exception{
		String[]args=new String[]{"-tcp"};
		Server.main(args);
	}
	
	/**
	 * build a JobDefinition document from an XML file on the file system
	 * @param name
	 */
	protected JobDefinitionDocument getJSDLDoc(String name){
		JobDefinitionDocument jdd;
		InputStream is=null;
		try {
			is=getResource(name);
			assertNotNull(is);
			jdd=JobDefinitionDocument.Factory.parse(is);
			return jdd;
		} catch (Exception e) {
			fail(e.getMessage());
			return null;
		}finally{
			IOUtils.closeQuietly(is);
		} 
	}
	
	/**
	 * finds a resource (either on the classpath or as a file) 
	 * and returns an input stream for reading it
	 * @param name
	 * @return
	 */
	protected InputStream getResource(String name){
		InputStream is = getClass().getResourceAsStream(name);
		if(is==null){
			try{
				is=new FileInputStream(name);
			}catch(Exception e){}
		}
		return is;
	}
	
}
