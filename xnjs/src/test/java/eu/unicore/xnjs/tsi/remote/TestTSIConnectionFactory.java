package eu.unicore.xnjs.tsi.remote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import eu.unicore.util.Log;
import eu.unicore.xnjs.ConfigurationSource;
import eu.unicore.xnjs.tsi.TSIUnavailableException;
import eu.unicore.xnjs.tsi.remote.DefaultTSIConnectionFactory.RollingIndex;

public class TestTSIConnectionFactory extends RemoteTSITestCase {
	
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
		// check limit is respected
		assertThrows(TSIUnavailableException.class, ()->
			f.getTSIConnection("nobody", null, null, -1));
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
	public void testRefreshConfigDirect() throws Exception {
		DefaultTSIConnectionFactory f = (DefaultTSIConnectionFactory)xnjs.get(TSIConnectionFactory.class);
		assertNotNull(f);
		System.out.println("Original TSI hosts:");
		assertEquals(1, f.getTSIHosts().size());
		Arrays.asList(f.getTSIHosts()).forEach( h ->System.out.println(h) );
		System.out.println(f.getConnectionStatus());
		Properties originals = new Properties();
		originals.putAll(xnjs.getRawProperties());
		originals.put(TSIProperties.PREFIX+TSIProperties.TSI_MACHINE, "127.0.0.1, localhost");
		TSIProperties tsiProps = f.getTSIProperties();
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

	@Test
	public void testRefreshConfigViaXNJS() throws Exception {
		DefaultTSIConnectionFactory f = (DefaultTSIConnectionFactory)xnjs.get(TSIConnectionFactory.class);
		assertNotNull(f);
		System.out.println("Original TSI hosts:");
		assertEquals(1, f.getTSIHosts().size());
		Arrays.asList(f.getTSIHosts()).forEach( h ->System.out.println(h) );
		System.out.println(f.getConnectionStatus());
		Properties originals = new Properties();
		originals.putAll(xnjs.getRawProperties());
		originals.put(TSIProperties.PREFIX+TSIProperties.TSI_MACHINE, "127.0.0.1, localhost");
		xnjs.setProperties(originals);
		System.out.println("NEW TSI hosts:");
		assertEquals(2, f.getTSIHosts().size());
		Arrays.asList(f.getTSIHosts()).forEach( h ->System.out.println(h) );
		System.out.println(f.getConnectionStatus());
		originals.put(TSIProperties.PREFIX+TSIProperties.TSI_MACHINE, "127.0.0.1");
		xnjs.setProperties(originals);
		f.configure();
		System.out.println("Reset TSI hosts:");
		assertEquals(1, f.getTSIHosts().size());
		Arrays.asList(f.getTSIHosts()).forEach( h ->System.out.println(h) );
		System.out.println(f.getConnectionStatus());
	}

	@Test
	public void testRollingIndex() {
		var index = new RollingIndex(3);
		var exp = new int[] {0, 1, 2, 0};
		for(int i = 0; i<4; i++) {
			assertEquals(exp[i], index.next());
		}
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
