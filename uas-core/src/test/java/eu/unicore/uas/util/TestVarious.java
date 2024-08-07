package eu.unicore.uas.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.ServiceLoader;

import org.junit.jupiter.api.Test;

import eu.unicore.services.Capabilities;
import eu.unicore.services.Capability;
import eu.unicore.uas.SMSProperties;
import eu.unicore.uas.UASProperties;
import eu.unicore.uas.impl.sms.SMSBaseImpl;
import eu.unicore.uas.impl.sms.SMSUtils;
import eu.unicore.uas.impl.sms.StorageDescription;
import eu.unicore.uas.impl.sms.StorageManagementHomeImpl.StorageTypes;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.xnjs.io.ChangePermissions;
import eu.unicore.xnjs.io.ChangePermissions.PermissionsClass;
import eu.unicore.xnjs.io.IFileTransferCreator;
import eu.unicore.xnjs.io.IOCapabilities;

public class TestVarious {
	

	@Test
	public void testLoadCapabilities(){
		System.out.println("Loading USE capabilities.");
		ServiceLoader<Capabilities> sl=ServiceLoader.load(Capabilities.class);
		Iterator<Capabilities>iter=sl.iterator();
		int i=0;
		while(iter.hasNext()){
			Capability[]cs=iter.next().getCapabilities();
			for(int j=0; j<cs.length;j++){
				Capability c=cs[j];
				System.out.println(c.getInterface().getName()+ " provided by "+c.getImplementation().getName());
			}
			i++;
		}
		assertTrue(0<i);
	}
	
	@Test
	public void testLoadIOCapabilities(){
		System.out.println("Loading XNJS IO capabilities.");
		ServiceLoader<IOCapabilities> sl=ServiceLoader.load(IOCapabilities.class);
		Iterator<IOCapabilities>iter=sl.iterator();
		int i=0;
		while(iter.hasNext()){
			Class<? extends IFileTransferCreator>[]cs=iter.next().getFileTransferCreators();
			for(int j=0; j<cs.length;j++){
				Class <? extends IFileTransferCreator>c=cs[j];
				System.out.println("Filetransfer creator class "+c.getName());
			}
			i++;
		}
		assertTrue(0<i);
	}

		
	@Test
	public void testMakeSMSLocal(){
		String in="\\";
		assertTrue(in.replaceAll("\\\\", "/").equals("/"));
	}
	
	@Test
	public void testDirForLS(){
		String[] filenames=new String[]{"/foo", "/foo/bar", "foo/bar/baz"};
		String[] dirs=new String[]{"", "foo/", "foo/bar/"};
		String[] names=new String[]{"foo", "bar", "baz"};
		
		for(int i=0;i<filenames.length;i++){
			String filename = filenames[i];
			int b=filename.startsWith("/")? 1: 0;
			if(b>0)filename=filename.substring(b);
			String dir="";
			if(filename.length()>0 && filename.contains("/")){
				int j=filename.lastIndexOf("/");
				dir=j>-1?filename.substring(0,j+1):filename;
				filename=filename.substring(j+1);
			}
			assertEquals(dirs[i],dir);
			assertEquals(names[i],filename);
		}
	}
	
	public void testURLEncoding1()throws Exception{
		String file="/a test file";
		String e=SMSUtils.urlEncode(file);
		String d=SMSUtils.urlDecode(e);
		System.out.println(e);
		System.out.println(d);
		assertEquals(file, d);
	}

	@Test
	public void testURLEncoding2()throws Exception{
		String file="http://www.w3.org/People/Dürst/";
		String e=SMSUtils.urlEncode(file);
		System.out.println(e);
	}

	@Test
	public void testURLDecodingSimple()throws Exception{
		String file="/a%20test%20file";
		String expect="/a test file";
		String d=SMSUtils.urlDecode(file);
		assertEquals(expect,d);
		// should leave actual 'plus' alone
		file="/a+test+file";
		d=SMSUtils.urlDecode(file);
		assertEquals(file,d);
	}

	@Test
	public void testURLEncodingTrailingEscape1()throws Exception{
		String test="%";
		System.out.println(URLEncoder.encode(test, "UTF-8"));
		String file="/file%~";
		String e=SMSUtils.urlEncode(file);
		System.out.println(e);
		String d=SMSUtils.urlDecode(e);
		System.out.println(d);
		assertEquals(file, d);
	}

	@Test
	public void testChangePermissionUtils() throws Exception {
		String perm = "r-x-w-rwx";
		ChangePermissions[] xnjsRequest = SMSUtils.getChangePermissions(perm);
		assertEquals("r-x",xnjsRequest[0].getPermissions());
		assertEquals(PermissionsClass.OWNER,xnjsRequest[0].getClazz());
		assertEquals("-w-",xnjsRequest[1].getPermissions());
		assertEquals(PermissionsClass.GROUP,xnjsRequest[1].getClazz());
		assertEquals("rwx",xnjsRequest[2].getPermissions());
		assertEquals(PermissionsClass.OTHER,xnjsRequest[2].getClazz());

		// short version
		perm = "r-x";
		xnjsRequest = SMSUtils.getChangePermissions(perm);
		assertEquals("r-x",xnjsRequest[0].getPermissions());
		assertEquals(PermissionsClass.OWNER,xnjsRequest[0].getClazz());
		
		// invalid
		perm = "r-x-w-rw";
		try{
			SMSUtils.getChangePermissions(perm);
			fail("Expected exception");
		}catch(IllegalArgumentException e){
			System.out.println("OK: "+e.getMessage());
		}
		
		// too many
		perm = "rwxrwxrwxrwx";
		try{
			SMSUtils.getChangePermissions(perm);
			fail("Expected exception");
		}catch(IllegalArgumentException e){
			System.out.println("OK: "+e.getMessage());
		}
		
	}
	

	@Test
	public void testStorageDescription() throws ConfigurationException, IOException {
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
			assertTrue(desc.getName().equals("NNN") || desc.getName().equals("NNN2"));
			assertEquals("path", desc.getPathSpec());
			assertEquals(SMSBaseImpl.class, desc.getStorageClass());
			assertEquals(StorageTypes.CUSTOM, desc.getStorageType());
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
		assertEquals(StorageTypes.VARIABLE, asd.getStorageType());
		assertEquals("MY_WORK", asd.getPathSpec());
	}

}
