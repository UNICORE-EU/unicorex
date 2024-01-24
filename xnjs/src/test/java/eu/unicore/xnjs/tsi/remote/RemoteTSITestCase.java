package eu.unicore.xnjs.tsi.remote;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.google.inject.AbstractModule;

import eu.unicore.xnjs.BaseModule;
import eu.unicore.xnjs.ConfigurationSource;
import eu.unicore.xnjs.ems.EMSTestBase;
import eu.unicore.xnjs.tsi.local.LocalExecution.DataMover;

/**
 * this  starts a TSI server (on ports 65431/65432)
 */
public abstract class RemoteTSITestCase extends EMSTestBase {

	@After
	public void tearDown() throws Exception {
		if(xnjs!=null){
			((DefaultTSIConnectionFactory)xnjs.get(TSIConnectionFactory.class)).stop();
		}
		super.tearDown();
	}

	protected RemoteTSI makeTSI(){
		return (RemoteTSI)xnjs.getTargetSystemInterface(null);
	}

	@Override
	protected void addProperties(ConfigurationSource cs){
		super.addProperties(cs);
		String p = TSIProperties.PREFIX;
		Properties props = cs.getProperties();
		props.put(p+TSIProperties.TSI_MACHINE,getTSIMachine());
		props.put(p+TSIProperties.TSI_PORT,getTSIPort());
		props.put(p+TSIProperties.TSI_MYPORT,"65432");
		props.put(p+TSIProperties.TSI_BSSUSER,System.getProperty("user.name"));
		props.put(p+TSIProperties.BSS_UPDATE_INTERVAL,"2000");
	}

	protected void addModules(ConfigurationSource cs){
		cs.addModule(new BaseModule(cs.getProperties()));
		cs.addModule(getTSIModule(cs));
		cs.addModule(getPersistenceModule());
	}

	protected AbstractModule getTSIModule(ConfigurationSource cs){
		return new RemoteTSIModule(cs.getProperties());
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

	@BeforeClass
	public static void startTSI() throws Exception {
		ProcessBuilder pb=new ProcessBuilder();
		File tsiExec=new File("src/test/resources/tsi/bin/start.sh");
		pb.command(tsiExec.getAbsolutePath());
		Process p=pb.start();
		DataMover m=new DataMover(p.getInputStream(),System.out);
		m.run();
		int exitCode=p.waitFor();
		Thread.sleep(500);
		System.out.println("TSI started.");
		if(exitCode!=0)throw new IOException("TSI start returned non-zero exit code <"+exitCode+">");
	}

	@AfterClass
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

}
