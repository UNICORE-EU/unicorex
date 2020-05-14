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
 *********************************************************************************/
 

package de.fzj.unicore.xnjs.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;

import org.apache.commons.io.FileUtils;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.junit.Before;
import org.junit.Test;

import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.EMSTestBase;
import de.fzj.unicore.xnjs.io.TransferInfo.Status;
import de.fzj.unicore.xnjs.io.impl.FileTransferEngine;

public class TestSimpleHttp extends EMSTestBase {
	
	int port;
	boolean gotCall=false;
	String answer="hello world!";
	
	@Before
	public void startFakeHttpServer()throws Exception{
		// start a fake HTTP server which processes exactly 
		// one request before shutting itself down
		Runnable r=new Runnable(){
			public void run(){
				try(ServerSocket s=new ServerSocket(0)){
					port=s.getLocalPort();
					s.setSoTimeout(10000);
					try{
						byte[]buf=new byte[2048];
						Socket socket=s.accept();
						//Thread.sleep(1000);
						gotCall=true;
						socket.getInputStream().read(buf);
						String req=new String(buf);
						if(req.startsWith("GET")){
							doGet(socket);
						}
						else if(req.startsWith("PUT")){
							doPut(socket);
						}
						socket.close();
					}catch(SocketTimeoutException te){};
				}catch(Exception ex){
					ex.printStackTrace();
				}
			}
			
			
			private void doGet(Socket socket)throws Exception{
				gotCall=true;
				socket.getOutputStream().write(("HTTP/1.1 200 OK\nContent-Length: "+answer.length()+"\n\n").getBytes());
				socket.getOutputStream().write(answer.getBytes());
				socket.getOutputStream().flush();
			}
			
			private void doPut(Socket socket)throws Exception{
				socket.getOutputStream().write("HTTP/1.1 204 No Content\n".getBytes());
				socket.getOutputStream().flush();
			}
			
		};
		Thread t = new Thread(r);
		t.setName("FakeHTTPServerThread");
		t.start();
		Thread.sleep(2000);
	}

	@Test
	public void testHttpDownload(){
		try {
			DataStageInInfo info = new DataStageInInfo();
			info.setSources(new URI[]{new URI("http://localhost:"+port)});
			info.setFileName("tmp-"+System.currentTimeMillis());
			IFileTransfer ft=new FileTransferEngine(xnjs).
			  createFileImport(null, "target",info);
			assertNotNull(ft);
			ft.run();
			TransferInfo fti = ft.getInfo();
			long transferred=fti.getTransferredBytes();
			assertEquals(answer.length(),transferred);
			System.out.println("Final status: "+fti.getStatusMessage());
			assertEquals(Status.DONE,fti.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			//do not fail, as in some environments this
			//can happen due to missing proxy setup etc.
		}
	}
	
	@Test
	public void testDownloadUsingWget(){
		try {
			DataStageInInfo info = new DataStageInInfo();
			info.setSources(new URI[]{new URI("http://localhost:"+port)});
			info.setFileName("tmp-"+System.currentTimeMillis());
			String actionID=createDummyParent();
			String wd=getWorkingDir(actionID);
			xnjs.setProperty("wget", "wget");
			IFileTransfer ft=new FileTransferEngine(xnjs).
			  createFileImport(null,wd,info);
			assertNotNull(ft);
			TransferInfo fti = ft.getInfo();
			fti.setParentActionID(actionID);
			ft.run();
			long transferred=fti.getTransferredBytes();
			assertEquals(answer.length(),transferred);
			xnjs.setProperty("wget", null);
		} catch (Exception e) {
			e.printStackTrace();
			//do not fail, as in some environments this
			//can happen due to missing proxy setup etc.
		}
	}
	
	@Test
	public void testHttpUpload(){
		try {
			File source=new File("target","httpsource-"+System.currentTimeMillis());
			DataStageOutInfo info = new DataStageOutInfo();
			info.setTarget(new URI("http://localhost:"+port));
			info.setFileName(source.getName());
			FileUtils.writeStringToFile(source, "testdata", "UTF-8");
			IFileTransfer ft=new FileTransferEngine(xnjs).
			  createFileExport(null, "target", info);
			assertNotNull(ft);
			ft.run();
			TransferInfo fti = ft.getInfo();
			System.out.println("Final status: " + fti.getStatusMessage());
			assertEquals(Status.DONE, fti.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			//do not fail, as in some environments this
			//can happen due to missing proxy setup etc.
		}
	}
	
	private String createDummyParent()throws Exception{
		JobDefinitionDocument xml=JobDefinitionDocument.Factory.newInstance();
		xml.addNewJobDefinition().addNewJobDescription().addNewApplication().setApplicationName("Date");
		Action job=xnjs.makeAction(xml);
		String id=(String)mgr.add(job, null);
		return id;
	}
	
	private String getWorkingDir(String actionID)throws Exception{
		return internalMgr.getAction(actionID).getExecutionContext().getWorkingDirectory();
	}

}
