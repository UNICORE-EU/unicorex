package eu.unicore.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import eu.unicore.client.data.Metadata.CrawlerControl;

public class TestUtils {
	
	@Test
	public void testCrawlerControl()throws IOException{
		String[] excl={"not-this.pdf","*png","foo*.txt"};
		String[] incl={"*.java"};

		CrawlerControl cc=new CrawlerControl(null, excl);
		assertTrue(cc.toString().contains("exclude=not-this.pdf,*png,foo*.txt"));
		Properties p=new Properties();
		p.load(IOUtils.toInputStream(cc.toString(), "UTF-8"));
		assertEquals("not-this.pdf,*png,foo*.txt", p.get("exclude"));

		CrawlerControl cc2=new CrawlerControl(incl, null);
		assertTrue(cc2.toString().contains("include=*.java"));
		Properties p2=new Properties();
		p2.load(IOUtils.toInputStream(cc2.toString(), "UTF-8"));
		assertEquals("*.java", p2.get("include"));
	}
	
	@Test
	public void testCrawlerControlFromProperties()throws IOException{
		Properties p=new Properties();
		p.setProperty("exclude", "not-this.pdf, *png, foo*.txt");
		p.setProperty("useDefaultExcludes", "false");
		CrawlerControl cc=CrawlerControl.create(p);
		assertTrue(cc.toString().contains("exclude=not-this.pdf,*png,foo*.txt"));
		assertFalse(cc.isUseDefaultExcludes());
		Properties p2=new Properties();
		p2.setProperty("include", "*.java");
		CrawlerControl cc2=CrawlerControl.create(p2);
		assertTrue(cc2.toString().contains("include=*.java"));
		assertTrue(cc2.isUseDefaultExcludes());
	}

	@Test
	public void testEndpoint()throws IOException{
		Endpoint ep1 = new Endpoint("http://foo");
		ep1.setInterfaceName("test");
		ep1.setServerIdentity("cn=test");
		ep1.setServerPublicKey("...");
		Endpoint ep2 = ep1.cloneTo("http://foo");
		assertEquals(ep1, ep2);
		assertEquals(ep1.hashCode(), ep2.hashCode());
		assertEquals("cn=test", ep2.getServerIdentity());
		assertEquals("...", ep2.getServerPublicKey());
		assertEquals("test", ep2.getInterfaceName());
	}
}
