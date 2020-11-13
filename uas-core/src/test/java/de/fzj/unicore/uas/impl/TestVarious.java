/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/
 

package de.fzj.unicore.uas.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URLEncoder;
import java.util.Calendar;

import org.apache.xmlbeans.SchemaProperty;
import org.apache.xmlbeans.XmlObject;
import org.ggf.schemas.byteio.x2005.x10.byteIo.TransferInformationType;
import org.ggf.schemas.byteio.x2005.x10.byteIo.TransferInformationTypeDocument;
import org.junit.Test;
import org.oasisOpen.docs.wsrf.rl2.CurrentTimeDocument;
import org.unigrids.x2006.x04.services.fts.FileTransferPropertiesDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.fts.FileTransferImpl;
import de.fzj.unicore.uas.fts.byteio.ByteIO;
import de.fzj.unicore.uas.fts.byteio.RandomByteIO;
import de.fzj.unicore.uas.fts.rft.StoreImpl;
import de.fzj.unicore.uas.impl.sms.SMSUtils;
import de.fzj.unicore.wsrflite.xmlbeans.client.RegistryClient;
import de.fzj.unicore.xnjs.io.ChangePermissions;
import de.fzj.unicore.xnjs.io.ChangePermissions.PermissionsClass;
import eu.unicore.services.ws.utils.WSServerUtilities;

public class TestVarious {
	
	@Test
	public void testRegistryContentMaker(){
	   CurrentTimeDocument ct=CurrentTimeDocument.Factory.newInstance();
	   ct.addNewCurrentTime().setCalendarValue(Calendar.getInstance());
	   assertTrue(RegistryClient.makeContent(new XmlObject[]{ct}).toString().contains("CurrentTime"));
	}
	
	@Test
	public void testFTProps(){
		   FileTransferPropertiesDocument pd=FileTransferPropertiesDocument.Factory.newInstance();
		   pd.addNewFileTransferProperties();
		   SchemaProperty[] sp=pd.getFileTransferProperties().schemaType().getElementProperties();
			for(SchemaProperty p: sp){
					p.getName();
			}
	}
		
	@Test
	public void testMakeSMSLocal(){
		String in="\\";
		assertTrue(in.replaceAll("\\\\", "/").equals("/"));
	}
	
	@Test
	public void testEPRUtils(){
		String dn="CN=Test server";
		EndpointReferenceType epr=EndpointReferenceType.Factory.newInstance();
		WSServerUtilities.addServerIdentity(epr, dn);
		String out=WSServerUtilities.extractServerIDFromEPR(epr);
		assertEquals(dn, out);
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

	@Test
	public void testByteIOCodec1()throws Exception{
		TransferInformationTypeDocument tid=TransferInformationTypeDocument.Factory.newInstance();
		TransferInformationType ti=tid.addNewTransferInformationType();
		String mechanism=RandomByteIO.TRANSFER_SIMPLE;
		ti.set(ByteIO.encode(mechanism, "foobar".getBytes()));
		
		ti.setTransferMechanism(mechanism);
		assertTrue(tid.toString().contains("data"));
		assertTrue(tid.toString().contains("Zm9vYmFy"));
	}

	@Test
	public void testByteIOCodec4()throws Exception{
		TransferInformationTypeDocument tid=TransferInformationTypeDocument.Factory.newInstance();
		TransferInformationType ti=tid.addNewTransferInformationType();
		String mechanism=RandomByteIO.TRANSFER_SIMPLE;
		ti.set(ByteIO.encode(mechanism, "foobar".getBytes()));
		ti.setTransferMechanism(mechanism);
		assertTrue(tid.toString().contains("data"));
		assertTrue(tid.toString().contains("Zm9vYmFy"));
		//decode
		try{
			String s=new String(ByteIO.decode(mechanism, ti));
			assertTrue("foobar".equals(s));
		}catch(Exception e){
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testURLEncoding()throws Exception{
		String file="/a test file";
		String e=FileTransferImpl.urlEncode(file);
		String d=SMSUtils.urlDecode(e);
		assertEquals(file, d);
	}
	
	public void testURLEncoding2()throws Exception{
		String file="/a test file";
		String e=SMSUtils.urlEncode(file);
		String d=SMSUtils.urlDecode(e);
		System.out.println(e);
		System.out.println(d);
		assertEquals(file, d);
	}

	@Test
	public void testURLEncoding3()throws Exception{
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
	public void testURLEncodingTrailingEscape()throws Exception{
		String test="%";
		System.out.println(URLEncoder.encode(test, "UTF-8"));
		String file="/file%~";
		String e=FileTransferImpl.urlEncode(file);
		System.out.println(e);
		String d=SMSUtils.urlDecode(e);
		System.out.println(d);
		assertEquals(file, d);
	}

	@Test
	public void testURLEncodingTrailingEscape2()throws Exception{
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
	public void testComputeRFTChunkLength(){
		
		long total=20*1024*1024L;
		assertEquals(StoreImpl.minChunkLength, StoreImpl.computeChunkLength(total));
		
		total=1100*1024*1024L;
		assertTrue(StoreImpl.computeChunkLength(total)>StoreImpl.minChunkLength);
		assertTrue(StoreImpl.computeChunkLength(total)*100L>total);
		assertTrue(StoreImpl.computeChunkLength(total)*100L<1.01*total);
		
		total=3000*1024*1024L;
		assertTrue(StoreImpl.computeChunkLength(total)>StoreImpl.minChunkLength);
		assertTrue(StoreImpl.computeChunkLength(total)*100L>total);
		assertTrue(StoreImpl.computeChunkLength(total)*100L<1.01*total);
		
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
	
}
