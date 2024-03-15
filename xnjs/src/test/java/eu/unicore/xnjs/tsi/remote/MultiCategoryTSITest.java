package eu.unicore.xnjs.tsi.remote;

import static org.junit.Assert.assertNotNull;

import java.util.Properties;

import org.junit.Test;

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
		props.put(p+TSIProperties.TSI_MACHINE+".bignodes", "localhost");
	}
	
	
	@Test
	public void testConnectionFactory()throws Exception{
		TSIConnectionFactory f=xnjs.get(TSIConnectionFactory.class);
		assertNotNull(f);
		try(TSIConnection c=f.getTSIConnection("nobody", null, null, -1)){
			System.out.println("TSI "+c.getTSIVersion()+" isAlive="+c.isAlive());
			System.out.println(c);
			System.out.println(c.getConnectionID());
		}
	}

}
