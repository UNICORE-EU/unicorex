package eu.unicore.xnjs.tsi.remote;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import eu.unicore.security.Client;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.httpclient.ClientProperties;
import eu.unicore.util.httpclient.IClientConfiguration;
import eu.unicore.util.httpclient.IPlainClientConfiguration;
import eu.unicore.xnjs.ConfigurationSource;
import eu.unicore.xnjs.ems.BasicManager;
import eu.unicore.xnjs.ems.EMSTestBase;
import eu.unicore.xnjs.ems.InternalManager;
import eu.unicore.xnjs.ems.Manager;
import eu.unicore.xnjs.io.http.IConnectionFactory;
import eu.unicore.xnjs.io.http.SimpleConnectionFactory;
import eu.unicore.xnjs.tsi.local.LocalExecution.DataMover;
import eu.unicore.xnjs.tsi.remote.server.ServerTSIModule;

/**
 * this  starts a TSI server (on ports 65431/65432)
 *
 * which uses SSL
 *
 */
public abstract class RemoteTSISSLTestCase extends EMSTestBase {

	@AfterEach
	public void tearDown() throws Exception {
		if(xnjs!=null){
			xnjs.get(TSIConnectionFactory.class).stop();
		}
		super.tearDown();
	}

	protected RemoteTSI makeTSI(){
		Client cl = TSIMessages.createMinimalClient("nobody");
		return (RemoteTSI)xnjs.getTargetSystemInterface(cl);
	}

	@Override
	protected void addProperties(ConfigurationSource cs){
		super.addProperties(cs);
		String p = TSIProperties.PREFIX;
		Properties props = cs.getProperties();
		props.put(p+TSIProperties.TSI_MACHINE,getTSIMachine());
		props.put(p+TSIProperties.TSI_PORT,getTSIPort());
		props.put(p+TSIProperties.TSI_MYPORT,"65432");
		props.put(p+TSIProperties.TSI_DISABLE_SSL,"false");
		props.put(p+TSIProperties.TSI_BSSUSER,System.getProperty("user.name"));
		props.put(p+TSIProperties.BSS_UPDATE_INTERVAL,"2000");
	}

	@Override
	protected void addModules(ConfigurationSource cs){
		cs.addModule(new SecureBaseModule(cs.getProperties()));
		cs.addModule(getTSIModule(cs));
		cs.addModule(getPersistenceModule());
	}

	protected AbstractModule getTSIModule(ConfigurationSource cs){
		return new ServerTSIModule(cs.getProperties());
	}


	protected String getFileSpace(){
		File f=new File("target/xnjs_test_"+System.currentTimeMillis());
		return f.getAbsolutePath();
	}

	protected String getTSIMachine(){
		return "127.0.0.1";
	}

	protected String getTSIPort(){
		return "65431";
	}

	@BeforeAll
	public static void startTSI() throws Exception {
		ProcessBuilder pb=new ProcessBuilder();
		File tsiExec=new File("src/test/resources/tsi/bin/start.sh");
		pb.command(tsiExec.getAbsolutePath(), "src/test/resources/tsi/conf-ssl/tsi.properties");
		Process p=pb.start();
		DataMover m=new DataMover(p.getInputStream(),System.out);
		m.run();
		int exitCode=p.waitFor();
		Thread.sleep(500);
		System.out.println("TSI started.");
		if(exitCode!=0)throw new IOException("TSI start returned non-zero exit code <"+exitCode+">");
	}

	@AfterAll
	public static void stopTSI() throws Exception {
		ProcessBuilder pb=new ProcessBuilder();
		File tsiExec=new File("src/test/resources/tsi/bin/stop.sh");
		pb.command(tsiExec.getAbsolutePath());
		Process p=pb.start();
		DataMover m=new DataMover(p.getInputStream(),System.out);
		m.run();
		int exitCode=p.waitFor();
		if(exitCode!=0)throw new IOException("TSI stop returned non-zero exit code");
	}

	public static class SecureBaseModule extends AbstractModule {
		
		protected final Properties properties;
		
		public SecureBaseModule(Properties properties){
			this.properties = properties;
		}
		
		@Override
		protected void configure(){
			bind(InternalManager.class).to(BasicManager.class);
			bind(Manager.class).to(BasicManager.class);
		}
		
		@Provides
		public IConnectionFactory getConnectionFactory(){
			return new SimpleConnectionFactory();
		}
		
		@Provides
		public IClientConfiguration getSecurityConfiguration(){
			try {
				return new ClientProperties(new File("src/test/resources/xnjs-credential.properties"));
			}catch(Exception ex) {
				throw new ConfigurationException("", ex);
			}
		}
		
		@Provides
		public IPlainClientConfiguration getSecurityConfiguration2(){
			return getSecurityConfiguration();
		}
	}
}
