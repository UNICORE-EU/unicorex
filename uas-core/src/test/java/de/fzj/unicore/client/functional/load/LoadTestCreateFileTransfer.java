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
 
package de.fzj.unicore.client.functional.load;

import org.unigrids.x2006.x04.services.tss.SubmitDocument;

import de.fzj.unicore.uas.client.JobClient;
import de.fzj.unicore.uas.client.StorageClient;
import de.fzj.unicore.uas.client.TransferControllerClient;


/**
 * creates a large number of server-to-server transfers to check for memory problems
 * 
 * @author schuller
 */
public class LoadTestCreateFileTransfer extends LoadTester {
		
	@Override
	protected void setUp()throws Exception{
		max_num_client_threads=2;
		num_requests=50;
		super.setUp();
	}
	
	public void doLoad() throws Exception{
		
		super.doLoad();
	}
	
	@Override
	protected Runnable getTask(){
		return new Runnable(){
			public void run(){
				try {
					String tName=Thread.currentThread().getName();
					int i=0;
					SubmitDocument in=SubmitDocument.Factory.newInstance();
					in.addNewSubmit().addNewJobDefinition().addNewJobDescription().addNewApplication().setApplicationName("Date");
					JobClient jobClient=tssClient.submit(in);
					StorageClient storage=jobClient.getUspaceClient();
					while(i<num_requests){
					
						//create sendfile
						TransferControllerClient tfc=storage.sendFile("stdout", "BFT:"+storage.getEPR().getAddress().getStringValue()+"#/test");
						do{
							Thread.sleep(300);
						}while(!tfc.isComplete());
						i++;
					}
					printStats("["+tName+"] ended.");
					running.decrementAndGet();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
	}
	
}
