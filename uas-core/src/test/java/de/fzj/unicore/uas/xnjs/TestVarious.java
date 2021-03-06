package de.fzj.unicore.uas.xnjs;

import static org.junit.Assert.assertEquals;

import java.net.URI;

import org.junit.Test;
import org.unigrids.services.atomic.types.PermissionsDocument;

import de.fzj.unicore.uas.util.Pair;
import de.fzj.unicore.xnjs.io.Permissions;

public class TestVarious {

	@Test
	public void testUmask()throws Exception{
		String u = "077";
		System.out.println(u+ " = "+Integer.parseInt(u, 8)+" = "+Integer.toOctalString(Integer.parseInt(u, 8)));
	}

	
	@Test
	public void test0()throws Exception{
		URI uri=new URI("bft:http://somehost:1234?res=mystorage#foo.txt");
		assertEquals("http://somehost:1234?res=mystorage", uri.getSchemeSpecificPart());
	}
	
	@Test
	public void testPermissionConverter(){
		PermissionsDocument pd=PermissionsDocument.Factory.newInstance();
		pd.addNewPermissions();
		Permissions p=XNJSFacade.getXNJSPermissions(pd.getPermissions());
		assertEquals("r--",String.valueOf(p));
		pd.getPermissions().setExecutable(true);
		pd.getPermissions().setWritable(true);
		p=XNJSFacade.getXNJSPermissions(pd.getPermissions());
		assertEquals("rwx",String.valueOf(p));
	}

	@Test
	public void testExtractStorageURL() throws Exception {
		String u1 = "UFTP:http://localhost:8080/SITE/rest/core/storages/WORK/files/test.txt";
		Pair<String,String>spec = UFileTransferCreator.extractUrlInfo(new URI(u1));
		assertEquals("UFTP", spec.getM1());
		assertEquals("http://localhost:8080/SITE/rest/core/storages/WORK", spec.getM2());
		
		u1 = "https://localhost:8080/rest/core/storages/WORK/files/some/test.txt";
		spec = UFileTransferCreator.extractUrlInfo(new URI(u1));
		assertEquals("BFT", spec.getM1());
		assertEquals("https://localhost:8080/rest/core/storages/WORK", spec.getM2());
		
		u1 = "https://localhost:8080/rest/core/storages/WORK//files/some/test.txt";
		spec = UFileTransferCreator.extractUrlInfo(new URI(u1));
		assertEquals("BFT", spec.getM1());
		assertEquals("https://localhost:8080/rest/core/storages/WORK/", spec.getM2());
		
	}
}
