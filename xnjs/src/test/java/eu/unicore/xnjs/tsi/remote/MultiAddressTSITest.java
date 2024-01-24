package eu.unicore.xnjs.tsi.remote;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;


public class MultiAddressTSITest extends RemoteTSITestCase {

	@Override
	protected String getTSIMachine(){
		return "localhost localhost, localhost";
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
	
	@Test
	public void testConnectionFactory2()throws Exception{
		TSIConnectionFactory f=xnjs.get(TSIConnectionFactory.class);
		assertNotNull(f);
		try(TSIConnection c = f.getTSIConnection("nobody", null, "localh*", -1)){
		}catch(Exception ex) {
			assertTrue(ex.getMessage().contains("No TSI is configured at"));
			System.out.println("EX: "+ex.getMessage());
		}
		try(TSIConnection c = f.getTSIConnection("nobody", null, "localh*", -1)){
			System.out.println("TSI "+c.getTSIVersion()+" isAlive="+c.isAlive());
			System.out.println(c);
			System.out.println(c.getConnectionID());
		}
	}

}
