package eu.unicore.xnjs.tsi.remote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import eu.unicore.security.Client;
import eu.unicore.xnjs.ConfigurationSource;
import eu.unicore.xnjs.tsi.remote.server.DefaultTSIConnectionFactory;
import eu.unicore.xnjs.tsi.remote.server.ServerTSIConnection;


public class MultiCategoryTSITest extends RemoteTSITestCase {

	@Override
	protected String getTSIMachine(){
		return "localhost";
	}

	@Override
	protected void addProperties(ConfigurationSource cs){
		super.addProperties(cs);
		String p = TSIProperties.PREFIX;
		Properties props = cs.getProperties();
		props.put(p+TSIProperties.TSI_MACHINE+".bignodes", "127.0.0.1");
	}

	@Test
	public void testConnectionFactory()throws Exception{
		DefaultTSIConnectionFactory f = (DefaultTSIConnectionFactory
				)xnjs.get(TSIConnectionFactory.class);
		assertNotNull(f);
		assertEquals(2, f.getTSIConnectorStates().size());
		Client cl = TSIMessages.createMinimalClient("nobody");
		try(ServerTSIConnection c = f.getTSIConnection(cl, "127.0.*", -1)){
			System.out.println("TSI "+c.getTSIVersion()+" isAlive="+c.isAlive());
			System.out.println(c);
			System.out.println(c.getConnectionID());
		}
		try(ServerTSIConnection c = f.getTSIConnection(cl, "category:bignodes", -1)){
			System.out.println("TSI "+c.getTSIVersion()+" isAlive="+c.isAlive());
			System.out.println(c);
			System.out.println(c.getConnectionID());
		}
		System.out.println("Defined categories: "+f.getTSIHostCategories());
	}

}