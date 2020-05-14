package de.fzj.unicore.uas.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.unigrids.services.atomic.types.MetadataType;
import org.unigrids.services.atomic.types.TextInfoType;

import de.fzj.unicore.uas.client.MetadataClient.CrawlerControl;

public class TestMetadataClient {
	
	@Test
	public void testConvertMetadataToMap(){
		MetadataType meta=MetadataType.Factory.newInstance();
		meta.setContentMD5("ABCD");
		meta.setContentType("text/plain");
		meta.setTagArray(new String[]{"foo","bar"});
		TextInfoType p=meta.addNewProperty();
		p.setName("x");
		p.setValue("value-of-x");
		Map<String, String> map=MetadataClient.asMap(meta);
		assertNotNull(map);
		assertEquals("ABCD",map.get(MetadataClient.CONTENT_MD5));
		assertEquals("text/plain",map.get(MetadataClient.CONTENT_TYPE));
		assertEquals("foo,bar",map.get(MetadataClient.TAGS));
		assertEquals("value-of-x",map.get("x"));
	}
	
	@Test
	public void testConvertMapToMeta(){
		Map<String, String> map=new HashMap<String, String>();
		map.put("x", "value-of-x");
		map.put("Content-Type", "text/plain");
		map.put("Content-MD5", "ABCD");
		map.put("Tags", "foo,bar");
		MetadataType md=MetadataClient.convert(map);
		assertEquals(2, md.getTagArray().length);
		assertEquals(1, md.getPropertyArray().length);
		assertEquals("ABCD",md.getContentMD5());
		assertEquals("text/plain",md.getContentType());
	}
	
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
