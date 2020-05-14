/*
 * Copyright (c) 2011 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 29-07-2011
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package de.fzj.unicore.uas.util;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

import org.junit.Test;

import de.fzj.unicore.uas.SMSProperties;
import de.fzj.unicore.uas.UASProperties;
import de.fzj.unicore.uas.impl.sms.SMSBaseImpl;
import de.fzj.unicore.uas.impl.sms.StorageDescription;
import eu.unicore.util.configuration.ConfigurationException;


public class TestAddOnStorageDescriptorFactory {

	@Test
	public void test() throws ConfigurationException, IOException {
		Properties p = new Properties();
		String PREFIX = UASProperties.PREFIX + UASProperties.SMS_ADDON_STORAGE_PREFIX;
		
		p.setProperty(PREFIX+"NNN."+SMSProperties.PATH , "path");
		p.setProperty(PREFIX+"NNN."+SMSProperties.CLASS, SMSBaseImpl.class.getName());
		p.setProperty(PREFIX+"NNN."+SMSProperties.TYPE, "CUSTOM");
		p.setProperty(PREFIX+"NNN."+SMSProperties.EXTRA_PREFIX+"other", "dd");
		p.setProperty(PREFIX+"NNN."+SMSProperties.EXTRA_PREFIX+"oneMore.fff", "dd");
		
		p.setProperty(PREFIX+"NNN2."+SMSProperties.PATH , "path");
		p.setProperty(PREFIX+"NNN2."+SMSProperties.CLASS, SMSBaseImpl.class.getName());
		p.setProperty(PREFIX+"NNN2."+SMSProperties.TYPE, "CUSTOM");
		p.setProperty(PREFIX+"NNN2."+SMSProperties.EXTRA_PREFIX+"other", "dd");
		p.setProperty(PREFIX+"NNN2."+SMSProperties.EXTRA_PREFIX+"oneMore.fff", "dd");
		
		UASProperties props = new UASProperties(p);
		
		assertEquals(2, props.getAddonStorages().size());
		for (StorageDescription desc: props.getAddonStorages()) {
			assertTrue(desc.getName(), desc.getName().equals("NNN") || desc.getName().equals("NNN2"));
			assertEquals("path", desc.getPathSpec());
			assertEquals(SMSBaseImpl.class, desc.getStorageClass());
			assertEquals("CUSTOM", desc.getStorageTypeAsString());
			assertEquals(2, desc.getAdditionalProperties().size());
		}
	}
	
	@Test
	public void testTSSAttachedStorageDescription() throws ConfigurationException, IOException{
		String PREFIX = UASProperties.PREFIX + UASProperties.SMS_ADDON_STORAGE_PREFIX;
		Properties p=new Properties();
		p.put(PREFIX+"1.name","WORK");
		p.put(PREFIX+"1.type","VARIABLE");
		p.put(PREFIX+"1.path","WORK");
		
		p.put(PREFIX+"2.name","TEMP");
		p.put(PREFIX+"2.type","FIXEDPATH");
		p.put(PREFIX+"2.path","/tmp/unicorex-test");
		
		UASProperties cfg = new UASProperties(p);
		Collection<StorageDescription>list = cfg.getAddonStorages();
		assertTrue(list.size()==2);
		
		p=new Properties();
		p.put(PREFIX+"1.name","WORK");
		p.put(PREFIX+"1.type","VARIABLE");
		p.put(PREFIX+"1.path","MY_WORK");
		p.put(PREFIX+"1.protocols","UFTP");
		
		cfg = new UASProperties(p);
		list=cfg.getAddonStorages();
		assertEquals(1, list.size());
		StorageDescription asd=list.iterator().next();
		assertNotNull(asd);
		System.out.println(asd);
		assertEquals("WORK", asd.getName());
		assertEquals("VARIABLE", asd.getStorageTypeAsString());
		assertEquals("MY_WORK", asd.getPathSpec());
	}
}
