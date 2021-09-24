package eu.unicore.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

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
}
