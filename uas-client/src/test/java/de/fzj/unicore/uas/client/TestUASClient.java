package de.fzj.unicore.uas.client;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestUASClient {

	@Test
	public void testVersionCompare(){
		assertTrue(BaseUASClient.compareVersions("1.4.1","1.4.2"));
		assertTrue(BaseUASClient.compareVersions("1.4.1","DEVELOPMENT"));
		assertTrue(BaseUASClient.compareVersions("1.4.1-SNAPSHOT","1.4.2"));
		assertTrue(BaseUASClient.compareVersions("1.4.1","1.4.2-SNAPSHOT"));
		assertFalse(BaseUASClient.compareVersions("1.4.1","1.4.0"));
	}
	
	@Test
	public void testRetrySetup()throws Exception{
		//TODO
	}
}
