package de.fzj.unicore.xnjs.tsi.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import de.fzj.unicore.xnjs.ConfigurationSource;
import de.fzj.unicore.xnjs.tsi.TSIUnavailableException;
import eu.unicore.util.Log;

public class TestTSIConnectionPool extends RemoteTSITestCase {
	
	@Test
	public void testConnectionFactory()throws Exception{
		DefaultTSIConnectionFactory f = (DefaultTSIConnectionFactory)xnjs.get(TSIConnectionFactory.class);
		assertNotNull(f);
		
		List<TSIConnection>connections = new ArrayList<>();
		
		for(int i = 0; i<8; i++){
			TSIConnection c=f.getTSIConnection("nobody", null, null, -1);
			connections.add(c);
		}
		System.out.println("Connections : "+f.getLiveConnections());
		assertEquals(8, f.getLiveConnections());
		try{
			// check limit is respected
			f.getTSIConnection("nobody", null, null, -1);
			fail("Should hit limit");
		}catch(TSIUnavailableException te){}
		
		// put back into pool
		for(TSIConnection c: connections){
			c.close();
		}
		System.out.println("Pooled connections : "+f.getNumberOfPooledConnections());
		assertEquals(4, f.getNumberOfPooledConnections());
		
	}
	
	@Test
	public void testConnectorRestart() throws Exception {
		DefaultTSIConnectionFactory f = (DefaultTSIConnectionFactory)xnjs.get(TSIConnectionFactory.class);
		assertNotNull(f);
		TSIConnection c=f.getTSIConnection("nobody", null, null, -1);
		c.close();
		f.getTSISocketFactory().reInit();
		c=f.getTSIConnection("nobody", null, null, -1);
		c.close();
	}

	@Test
	public void testHostnameMatch() throws Exception {
		String[] requests = new String[] { "*", "node*", "node01*", "node??.cluster.org",
				"node01", "node01.cluster.org" };
		String actual = "node01.cluster.org";
		for(int i=0; i<requests.length; i++) {
			String want = requests[i];
			assertTrue(DefaultTSIConnectionFactory.matches(want, actual));
		}
		
		requests = new String[] { "node??.cluster.org" };
		actual = "node-special1.cluster.org";
		for(int i=0; i<requests.length; i++) {
			String want = requests[i];
			assertFalse(DefaultTSIConnectionFactory.matches(want, actual));
		}
		assertTrue(DefaultTSIConnectionFactory.matches(null, actual));
	}
	
	@Test
	public void testIOError() throws Exception {
		DefaultTSIConnectionFactory f = (DefaultTSIConnectionFactory)xnjs.get(TSIConnectionFactory.class);
		f.changeSetting(null, "fail_io", "1");
		RemoteTSI t  = makeTSI();
		File p = new File("./target/x.dat");
		try(OutputStream os = t.getOutputStream(p.getAbsolutePath())){
			IOUtils.write("test", os, "UTF-8");
		}
		catch(IOException ioe) {
			System.out.println("Expected ERROR: "+Log.createFaultMessage("", ioe));
		}
		f.changeSetting(null, "fail_io", "0");
	}

	@Test
	public void testRefreshConfig() throws Exception {
		DefaultTSIConnectionFactory f = (DefaultTSIConnectionFactory)xnjs.get(TSIConnectionFactory.class);
		assertNotNull(f);
		System.out.println("Original TSI hosts:");
		assertEquals(1, f.getTSIHosts().size());
		Arrays.asList(f.getTSIHosts()).forEach( h ->System.out.println(h) );
		System.out.println(f.getConnectionStatus());
		Properties originals = new Properties();
		originals.putAll(xnjs.getRawProperties());
		originals.put(TSIProperties.PREFIX+TSIProperties.TSI_MACHINE, "127.0.0.1, localhost");
		TSIProperties tsiProps = f.tsiProperties;
		tsiProps.setProperties(originals);
		f.configure();
		System.out.println("NEW TSI hosts:");
		assertEquals(2, f.getTSIHosts().size());
		Arrays.asList(f.getTSIHosts()).forEach( h ->System.out.println(h) );
		System.out.println(f.getConnectionStatus());
		originals.put(TSIProperties.PREFIX+TSIProperties.TSI_MACHINE, "127.0.0.1");
		tsiProps.setProperties(originals);
		f.configure();
		System.out.println("Reset TSI hosts:");
		assertEquals(1, f.getTSIHosts().size());
		Arrays.asList(f.getTSIHosts()).forEach( h ->System.out.println(h) );
		System.out.println(f.getConnectionStatus());
	}

	@Override
	protected void addProperties(ConfigurationSource cs){
		super.addProperties(cs);
		String p = TSIProperties.PREFIX;
		Properties props = cs.getProperties();
		props.put(p+TSIProperties.TSI_POOL_SIZE,"4");
		props.put(p+TSIProperties.TSI_WORKER_LIMIT,"8");
		props.put(p+TSIProperties.BSS_UPDATE_INTERVAL,"30000");
	}

}
