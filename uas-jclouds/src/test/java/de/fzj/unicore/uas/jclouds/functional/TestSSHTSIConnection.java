package de.fzj.unicore.uas.jclouds.functional;

import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.google.inject.AbstractModule;

import de.fzj.unicore.uas.sshtsi.SSHTSIModule;
import de.fzj.unicore.uas.sshtsi.SSHTSIProperties;
import de.fzj.unicore.xnjs.BaseModule;
import de.fzj.unicore.xnjs.ConfigurationSource;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.persistence.IActionStoreFactory;
import de.fzj.unicore.xnjs.persistence.JDBCActionStoreFactory;
import de.fzj.unicore.xnjs.tsi.TSI;
import de.fzj.unicore.xnjs.tsi.remote.TSIConnectionFactory;
import de.fzj.unicore.xnjs.tsi.remote.TSIProperties;
import eu.unicore.security.Client;
import eu.unicore.security.Xlogin;

/**
 * Testing the SSH tunneling. Requires a running TSI and sshd
 * 
 * @author schuller
 *
 */
public class TestSSHTSIConnection {
	
	@Test
	public void test() throws Exception {
		XNJS xnjs = new XNJS(getConfigSource());
		xnjs.start();
		TSIConnectionFactory cf = xnjs.get(TSIConnectionFactory.class);
		assertNotNull(cf);
		Client client = new Client();
		client.setXlogin(new Xlogin(new String[]{"unicore"}));
		TSI tsi = xnjs.getTargetSystemInterface(client);
		tsi.setStorageRoot("/");
		System.out.println(Arrays.asList(tsi.ls("/tmp")));
		System.out.println(tsi.resolve("$HOME"));
	}
	
	protected ConfigurationSource getConfigSource() throws Exception {
		ConfigurationSource cs = new ConfigurationSource();
		addProperties(cs);
		cs.addModule(new BaseModule(cs.getProperties()));
		cs.addModule(getPersistenceModule());
		cs.addModule(new SSHTSIModule(cs.getProperties()));
		return cs;
	}	
	
	protected AbstractModule getPersistenceModule(){
		return new AbstractModule() {
			
			@Override
			protected void configure() {
				bind(IActionStoreFactory.class).to(JDBCActionStoreFactory.class);
			}
		};
	}

	protected void addProperties(ConfigurationSource cs) throws Exception {
		Properties props = cs.getProperties();
		props.put("XNJS.idbfile", "src/test/resources/simpleidb");
		File fileSpace=new File("target","xnjs_filespace");
		FileUtils.deleteQuietly(fileSpace);
		props.put("XNJS.filespace",fileSpace.getAbsolutePath());
		File state=new File("target","xnjs_state");
		FileUtils.deleteQuietly(state);
		props.put("XNJS.statedir",state.getAbsolutePath());
		props.put("persistence.directory",state.getAbsolutePath());
		
		props.put(SSHTSIProperties.PREFIX+SSHTSIProperties.REMOTE_TSI_KEY_PATH, "/home/schuller/.ssh/id_rsa");
		props.put(SSHTSIProperties.PREFIX+SSHTSIProperties.REMOTE_TSI_KEY_PASS, ask());
		props.put(SSHTSIProperties.PREFIX+SSHTSIProperties.REMOTE_TSI_USERNAME, "bschuller");
		props.put(TSIProperties.PREFIX+TSIProperties.TSI_MACHINE, "134.94.199.49");
		props.put(TSIProperties.PREFIX+TSIProperties.TSI_MYPORT, "7654");
		props.put(TSIProperties.PREFIX+TSIProperties.TSI_PORT, "4433");
	}
	
	protected String ask() throws IOException {
		System.out.println("Enter SSH passphrase:");
		String x = new BufferedReader(new InputStreamReader(System.in)).readLine().trim();
		System.out.println("\""+x+"\"");
		return x;
	}
}
