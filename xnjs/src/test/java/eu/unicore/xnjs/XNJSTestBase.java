package eu.unicore.xnjs;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;

import com.google.inject.AbstractModule;

import eu.unicore.xnjs.ems.ActionStateChangeListener;
import eu.unicore.xnjs.ems.MockChangeListener;
import eu.unicore.xnjs.persistence.BasicActionStoreFactory;
import eu.unicore.xnjs.persistence.IActionStoreFactory;
import eu.unicore.xnjs.tsi.local.LocalTSIModule;

/**
 * base for XNJS tests
 */
public abstract class XNJSTestBase {
   
	protected XNJS xnjs;
    
	@Before
	public void setUp() throws Exception {
		File fileSpace=new File("target","xnjs_filespace");
		FileUtils.deleteQuietly(fileSpace);
		File state=new File("target","xnjs_state");
		FileUtils.deleteQuietly(state);
		xnjs = new XNJS(getConfigurationSource());
		preStart();
		xnjs.start();
	}
	
    
	@After
	public void tearDown() throws Exception {
		if(xnjs!=null)xnjs.stop();
	}
	
	protected ConfigurationSource getConfigurationSource() throws IOException {
		ConfigurationSource cs = new ConfigurationSource();
		addProperties(cs);
		addModules(cs);
		addProcessing(cs);
		return cs;
	}

	protected void addProperties(ConfigurationSource cs){
		Properties p = cs.getProperties();
		p.put("XNJS.idbfile", "src/test/resources/resources/simpleidb");
		File fileSpace=new File("target","xnjs_filespace");
		p.put("XNJS.filespace",fileSpace.getAbsolutePath());
		File state=new File("target","xnjs_state");
		p.put("persistence.directory",state.getAbsolutePath());
		p.put("XNJS."+XNJSProperties.XNJSWORKERS, "2");
	}
	
	protected void addProcessing(ConfigurationSource cs){
	}
	
	protected void addModules(ConfigurationSource cs){
		cs.addModule(new BaseModule(cs.getProperties()));
		cs.addModule(getPersistenceModule());
		cs.addModule(getTSIModule(cs.getProperties()));
		cs.addModule(getNotificationModule());
	}
	
	protected AbstractModule getTSIModule(Properties properties){
		return new LocalTSIModule(properties);
	};
	
	protected AbstractModule getPersistenceModule(){
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(IActionStoreFactory.class).to(BasicActionStoreFactory.class);
			}
		};
	}
	
	
	protected AbstractModule getNotificationModule(){
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(ActionStateChangeListener.class).to(MockChangeListener.class);
			}
		};
	};
	
	protected void preStart() throws Exception {}
	
}
