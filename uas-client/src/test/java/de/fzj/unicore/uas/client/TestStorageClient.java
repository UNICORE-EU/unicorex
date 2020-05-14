package de.fzj.unicore.uas.client;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.unigrids.x2006.x04.services.sms.ExtraParametersDocument.ExtraParameters;

public class TestStorageClient {

	@Test
	public void testParameterProvider(){
		ExtraParameters ep=StorageClient.makeExtraParameters(null, "TEST");
		assertNotNull(ep);
		assertTrue(ep.toString().contains("TEST.foo"));
		assertTrue(ep.toString().contains("bar"));
		
		//check that parameters are added to existing ones
		Map<String,String>params=new HashMap<String,String>();
		params.put("TEST.ham","spam");
		ExtraParameters ep2=StorageClient.makeExtraParameters(params, "TEST");
		assertNotNull(ep2);
		assertTrue(ep2.toString().contains("TEST.foo"));
		assertTrue(ep2.toString().contains("bar"));
		assertTrue(ep2.toString().contains("TEST.ham"));
		assertTrue(ep2.toString().contains("spam"));
		
		//check other protocol
		params.clear();
		ExtraParameters ep3=StorageClient.makeExtraParameters(params, "NNN");
		assertFalse(ep3.toString().contains("TEST.foo"));
	}
	
}
