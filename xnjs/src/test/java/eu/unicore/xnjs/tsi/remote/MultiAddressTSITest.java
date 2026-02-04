package eu.unicore.xnjs.tsi.remote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import eu.unicore.xnjs.tsi.remote.server.DefaultTSIConnectionFactory;
import eu.unicore.xnjs.tsi.remote.server.ServerTSIConnection;


public class MultiAddressTSITest extends RemoteTSITestCase {

	@Override
	protected String getTSIMachine(){
		return "localhost 127.0.0.1";
	}

	@Test
	public void testConnectionFactory()throws Exception{
		DefaultTSIConnectionFactory f = (DefaultTSIConnectionFactory)xnjs.get(TSIConnectionFactory.class);
		assertNotNull(f);
		assertEquals(2, f.getTSIConnectorStates().size());
		try(ServerTSIConnection c = f.getTSIConnection(TSIMessages.createMinimalClient("nobody"), null, -1)){
			System.out.println("TSI "+c.getTSIVersion()+" isAlive="+c.isAlive());
			System.out.println(c);
			System.out.println(c.getConnectionID());
		}
	}

	@Test
	public void testConnectionFactory2()throws Exception{
		DefaultTSIConnectionFactory f = (DefaultTSIConnectionFactory)xnjs.get(TSIConnectionFactory.class);
		assertNotNull(f);
		try(TSIConnection c = f.getTSIConnection(TSIMessages.createMinimalClient("nobody"), "127.0.*", -1)){
		}catch(Exception ex) {
			assertTrue(ex.getMessage().contains("No TSI is configured at"));
			System.out.println("EX: "+ex.getMessage());
		}
		try(ServerTSIConnection c = f.getTSIConnection(TSIMessages.createMinimalClient("nobody"), "localh*", -1)){
			System.out.println("TSI "+c.getTSIVersion()+" isAlive="+c.isAlive());
			System.out.println(c);
			System.out.println(c.getConnectionID());
		}
	}

}