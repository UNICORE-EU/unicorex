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

import org.ggf.schemas.jsdl.x2005.x11.jsdl.ApplicationType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.unigrids.x2006.x04.services.tss.SubmitDocument;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.client.TSSClient;

public class LoadTestJobMgmtServiceExt extends LoadTester{

	public void doLoad() throws Exception{
		
		int n=1000;
		
		try {
			
			System.out.println();
			System.out.println("***** Creating Target System Service *****");
			System.out.println();

			TSSClient tss=new TSSClient(url+UAS.TSS,tsEPR,sp);
			System.out.println("Created TSS at "+tsEPR);
			
			System.out.println();
			System.out.println("***** Running 'Date' application *****");
			System.out.println();
			//run date!!
			JobDefinitionDocument jdd=JobDefinitionDocument.Factory.newInstance();
			ApplicationType app=jdd.addNewJobDefinition().addNewJobDescription().addNewApplication();
			app.setApplicationName("Date");
			app.setApplicationVersion("1.0");
			
			SubmitDocument req=SubmitDocument.Factory.newInstance();
			req.addNewSubmit().setJobDefinition(jdd.getJobDefinition());
			
			for(int i=0;i<n;i++)
			{
				tss.Submit(req);
				if( (n % 50)==0) System.out.print(".");
			}
				
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
