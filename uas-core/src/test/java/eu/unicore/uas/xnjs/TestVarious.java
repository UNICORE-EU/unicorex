package eu.unicore.uas.xnjs;

import static org.junit.Assert.assertEquals;

import java.net.URI;

import org.junit.Test;

import eu.unicore.uas.xnjs.UFileTransferCreator;
import eu.unicore.util.Pair;

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
