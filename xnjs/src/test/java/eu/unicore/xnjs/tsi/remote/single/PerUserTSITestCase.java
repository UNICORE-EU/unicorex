package eu.unicore.xnjs.tsi.remote.single;

import java.io.File;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;

import com.google.inject.AbstractModule;

import eu.unicore.security.Client;
import eu.unicore.security.Xlogin;
import eu.unicore.xnjs.BaseModule;
import eu.unicore.xnjs.ConfigurationSource;
import eu.unicore.xnjs.ems.EMSTestBase;
import eu.unicore.xnjs.tsi.remote.RemoteTSI;
import eu.unicore.xnjs.tsi.remote.TSIConnectionFactory;
import eu.unicore.xnjs.tsi.remote.TSIProperties;

/**
 * this uses request-response style TSI processing
 */
public abstract class PerUserTSITestCase extends EMSTestBase {

	@AfterEach
	public void tearDown() throws Exception {
		if(xnjs!=null){
			xnjs.get(TSIConnectionFactory.class).stop();
		}
		super.tearDown();
	}

	protected RemoteTSI makeTSI() {
		return makeTSI("nobody");
	}

	protected RemoteTSI makeTSI(String user){
		Client c = null;
		if(user!=null) {
			c = new Client();
			c.setXlogin(new Xlogin(new String[] {user}));
		}
		return (RemoteTSI)xnjs.getTargetSystemInterface(c);
	}

	@Override
	protected void addProperties(ConfigurationSource cs){
		super.addProperties(cs);
		String p = TSIProperties.PREFIX;
		Properties props = cs.getProperties();
		props.put(p+TSIProperties.TSI_MACHINE,getTSIMachine());
		props.put(p+TSIProperties.TSI_PORT, "22");
		props.put(p+TSIProperties.TSI_BSSUSER,System.getProperty("user.name"));
		props.put(p+TSIProperties.BSS_UPDATE_INTERVAL,"2000");

		props.put(PerUserTSIProperties.PREFIX+"executable","src/test/resources/tsi/bin/process.sh");
		props.put(PerUserTSIProperties.PREFIX+"identityResolver.1.class", FileIdentityResolver.class.getName());
		props.put(PerUserTSIProperties.PREFIX+"identityResolver.1.file", "src/test/resources/certs/identities.json");
		
		props.put(PerUserTSIProperties.PREFIX+"unittesting","true");

	}

	protected void addModules(ConfigurationSource cs){
		cs.addModule(new BaseModule(cs.getProperties()));
		cs.addModule(getTSIModule(cs));
		cs.addModule(getPersistenceModule());
	}

	protected AbstractModule getTSIModule(ConfigurationSource cs){
		return new PerUserTSIModule(cs.getProperties());
	}

	protected String getFileSpace(){
		File f=new File("target/xnjs_test_"+System.currentTimeMillis());
		return f.getAbsolutePath();
	}

	protected String getTSIMachine(){
		return "127.0.0.1";
	}

	protected String getTSIPort(){
		return "22";
	}

}