package eu.unicore.xnjs.tsi.remote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import eu.unicore.xnjs.ConfigurationSource;


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
		try(TSIConnection c=f.getTSIConnection("nobody", null, "127.0.*", -1)){
			System.out.println("TSI "+c.getTSIVersion()+" isAlive="+c.isAlive());
			System.out.println(c);
			System.out.println(c.getConnectionID());
		}
		try(TSIConnection c=f.getTSIConnection("nobody", null, "category:bignodes", -1)){
			System.out.println("TSI "+c.getTSIVersion()+" isAlive="+c.isAlive());
			System.out.println(c);
			System.out.println(c.getConnectionID());
		}
		System.out.println("Defined categories: "+f.getTSIHostCategories());
	}

}
