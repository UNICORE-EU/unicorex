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

import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.ApplicationType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyDocumentDocument1;
import org.unigrids.x2006.x04.services.jms.StartDocument;
import org.unigrids.x2006.x04.services.tss.SubmitDocument;
import org.unigrids.x2006.x04.services.tss.SubmitResponseDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.TargetSystem;
import de.fzj.unicore.uas.TestSecConfigs;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.client.JobClient;
import de.fzj.unicore.uas.client.TSFClient;
import de.fzj.unicore.uas.client.TSSClient;
import de.fzj.unicore.wsrflite.exceptions.ResourceUnavailableException;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * does simple load testing, storing client-side response times and
 * turnaround times
 * 
 * you can subclass this and override the getTask() method
 * 
 * @author schuller
 */
public class LoadTester extends TestCase {
	
	public static int max_num_client_threads=20;
	
	public static int num_requests=20;

	public static String statsFile;
	
	private static FileWriter fw;
	
	public static boolean haveInit=false;
	
	protected static String url="https://localhost:8080/DEMO-SITE/services/";

	protected static IClientConfiguration sp=null;

	protected static EndpointReferenceType tsEPR;
	private static GetResourcePropertyDocumentDocument1 getRpdReq;
	private static SubmitDocument submitReq;
	private static boolean submitOnly=true;
	private static StartDocument startReq;
	
	protected static AtomicInteger running=new AtomicInteger(0);
	private static ArrayList<Long>responseTimes=new ArrayList<Long>(num_requests*max_num_client_threads);
	//private static ArrayList<Long>turnaroundTimes=new ArrayList<Long>(num_requests*max_num_client_threads);
	private static ArrayList<String>jobids=new ArrayList<String>(num_requests*max_num_client_threads);
	protected static AtomicInteger errors=new AtomicInteger(0);
	
	protected TSSClient tssClient=null;
	
	protected void setUp() throws Exception {
		super.setUp();
		
		sp=TestSecConfigs.getClientSecurityCfg(true);
		String s="";
		if(sp!=null && sp.isSslEnabled())s="_ssl_" ;
		statsFile="target/load"+s+max_num_client_threads+"_"+num_requests;
		//while(new File(statsFile).exists()) statsFile+="_1";
		
		fw=new FileWriter(statsFile);
		
		if(haveInit)return;
		
		//setup static xml docs
		getRpdReq=GetResourcePropertyDocumentDocument1.Factory.newInstance();
		getRpdReq.addNewGetResourcePropertyDocument();
		
		//submit doc
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.newInstance();
		ApplicationType app=jdd.addNewJobDefinition().addNewJobDescription().addNewApplication();
		app.setApplicationName("Date");
		app.setApplicationVersion("1.0");
		submitReq=SubmitDocument.Factory.newInstance();
		submitReq.addNewSubmit().setJobDefinition(jdd.getJobDefinition());
		if(submitOnly){
			submitReq.getSubmit().setAutoStartWhenReady(true);
		}
		//start doc
		startReq=StartDocument.Factory.newInstance();
		startReq.addNewStart();
		
		printStats(new Date()+": gathering statistics started.");
		if(sp!=null)printStats("Security settings: "+sp.toString());
		
		EndpointReferenceType tsfEPR=EndpointReferenceType.Factory.newInstance();
		tsfEPR.addNewAddress().setStringValue(url+UAS.TSF+"?res=default_target_system_factory");
		TSFClient tsf=new TSFClient(tsfEPR.getAddress().getStringValue(),tsfEPR, sp);
		tssClient=tsf.createTSS();
		
		long count=0;
		int N=2000;
		long start=System.currentTimeMillis();
		for(int i=0; i<N; i++){
			count+=tsf.getAccessibleTargetSystems().size();
		}
		long perCall=(System.currentTimeMillis()-start)/N;
		System.out.println("count "+count+" time per call: "+perCall+" ms.");
		throw new Exception("stop.");
		
//		tsEPR=tssClient.getEPR();
//		printStats("Created TSS at "+tsEPR.getAddress().getStringValue());
//		haveInit=true;
	}
	
	
	protected Runnable getTask(){
		
		return new Runnable(){
			public void run(){
				try {
					String tName=Thread.currentThread().getName();
					int i=0;
					
					//create wsrf clients for this thread
					TSSClient tss=new TSSClient(url+UAS.TSS,tsEPR,sp);
					
					while(i<num_requests){
						
						//get tss rp
						//runTimed("["+tName+"] GetTSSRPDoc",getTaskGetTSSRPDoc(tss));
						
						runTimed("["+tName+"] RunDate",getTaskRunDate(tss));
						
						i++;
					}
					printStats("["+Thread.currentThread().getName()+"] ended.");
				} catch (Exception e) {
					e.printStackTrace();
				}
				running.decrementAndGet();
			}
		};
	} 
	
	protected Runnable getTaskGetTSSRPDoc(final TargetSystem tss)throws Exception {
		return new Runnable(){
			public void run(){
				try{				
					tss.GetResourcePropertyDocument(getRpdReq);
				}catch(Exception e){
					e.printStackTrace();
				}
			};
		};
	}
	
	protected Runnable getTaskRunDate(final TSSClient tss) throws Exception {
		return getTaskRunDate(tss, submitOnly);
	}

	protected Runnable getTaskRunDate(final TSSClient tss, final boolean submitOnly)throws Exception {
		return new Runnable(){
			public void run(){
				try{
					SubmitResponseDocument res=tss.Submit(submitReq);
					EndpointReferenceType epr=res.getSubmitResponse().getJobReference();
					JobClient jobClient=new JobClient(url+UAS.JMS,epr,sp);
					jobids.add(epr.getAddress().getStringValue());
					jobClient.getCurrentTime().getTimeInMillis();
					if(submitOnly)return;
				
					jobClient.waitUntilReady(10000);
					//start...
					jobClient.start();
					jobClient.waitUntilDone(10000);
					//read stdout
					ByteArrayOutputStream os=new ByteArrayOutputStream();
					jobClient.getUspaceClient().download("/stdout").readAllData(os);
				}catch(ResourceUnavailableException rue){
					System.out.println("UNAVAILABLE");
				}catch(Exception e){
					System.out.println("["+Thread.currentThread().getName()+"]");
					e.printStackTrace();
					errors.incrementAndGet();
				}
			};
		};
	}
	
	
	public void dumpeprs(){
		try{
			for(String s: jobids) printStats(s);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	protected void runTimed(String msg, Runnable r)throws Exception{
		long s=System.currentTimeMillis();
		r.run();
		long e=System.currentTimeMillis();
		printStats(msg+" ["+(e-s)+" ms], running threads="+running);
		responseTimes.add(Long.valueOf(e-s));
	}
	
	protected static synchronized void printStats(String s, boolean echo)throws Exception {
		fw.append(s+"\n");
		fw.flush();
		if(echo)System.out.println(s);
	}
	protected static synchronized void printStats(String s)throws Exception {
		printStats(s,false);
	}

	//run a test...
	public void doLoad() throws Exception{
		System.out.println("Running load test.");
		System.out.println("Threads="+max_num_client_threads);
		System.out.println("requests per thread="+num_requests);
		long start=System.currentTimeMillis();
		
		//startup threads
		for(int i=0;i<max_num_client_threads;i++){
			new Thread(getTask()).start();
			running.incrementAndGet();
		}
		//wait
		while(running.intValue()>0) {
			System.out.println("running="+running);
			Thread.sleep(1500);
		}
		System.out.println("running="+running);
		Long a=Long.valueOf(0);
		Long total=Long.valueOf(0);
		Long max=0l;
		for(Long l:responseTimes){
			total+=l;
			max=Math.max(max, l);
		}
		a=total/responseTimes.size();
		long t=System.currentTimeMillis()-start;
		
		printStats("Summary:",true);
		printStats("Ran "+num_requests*max_num_client_threads+" requests, using "+max_num_client_threads+" client threads.",true);
		printStats("Average response time: "+a+ "ms.",true);
		printStats("Maximum response time: "+max+ "ms.",true);
		printStats("Errors: "+errors.get(),true);
		printStats("Total time: "+t+ "ms.",true);
		printStats(new Date()+": Collecting stats ended.",true);
	}
	
	
	public void test1()throws Exception {
		doLoad();
	}
	
}
