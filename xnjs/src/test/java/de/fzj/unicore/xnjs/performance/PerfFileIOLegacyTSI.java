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
 
package de.fzj.unicore.xnjs.performance;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import de.fzj.unicore.xnjs.tsi.remote.RemoteTSI;
import de.fzj.unicore.xnjs.tsi.remote.LegacyTSITestCase;

/**
 * filetransfer performance tests using a TSI server
 */
public class PerfFileIOLegacyTSI extends LegacyTSITestCase {

	int size=50000;
	String theLine="test123";
	
	@Test
	public void testRead()throws Exception{
		System.out.println("Start.");
		long total=0;
		int i=0;
		for(i=0;i<10;i++){
			total+=doRead();
		}
		System.out.println("Mean read rate = "+total/i+" kb/sec.");
	}
	
	
	private long doRead()throws Exception{
		File tmpDir = new File("target","tsi_io_tests");
		FileUtils.deleteQuietly(tmpDir);
		
		RemoteTSI tsi=(RemoteTSI)xnjs.getTargetSystemInterface(null);
		tsi.mkdir(tmpDir.getAbsolutePath());

		//write some junk to the file
		File f=new File(tmpDir,"out");
		FileWriter fw=new FileWriter(f);
		for(int i=0;i<size;i++){
			fw.write(theLine+"\n");
		}
		fw.close();
		System.out.println("length="+f.length());
		long start=System.currentTimeMillis();
		InputStream is=tsi.getInputStream(f.getAbsolutePath());
		assertNotNull(is);
		BufferedReader br=new BufferedReader(new InputStreamReader(is));
		String line;
		while(true){
			line=br.readLine();
			if(line==null)break;
			assertEquals(theLine,line);
		}
		long duration=System.currentTimeMillis()-start;
		long rate=f.length()/duration;
		
		System.out.println("Duration: "+duration+" ms., Read rate: "+rate+" kb/sec.");
		br.close();
		is.close();
		f.delete();
		return rate;
	}
	
	@Test
	public void testWrite()throws Exception{
		long total=0;
		int i=0;
		for(i=0;i<10;i++){
			total+=doWrite();
		}
		System.out.println("Mean write rate = "+total/i+" kb/sec.");
	}
	
	private long doWrite()throws Exception{
		File tmpDir = new File("target","tsi_io_tests");
		FileUtils.deleteQuietly(tmpDir);
		
		RemoteTSI tsi=(RemoteTSI)xnjs.getTargetSystemInterface(null);
		tsi.mkdir(tmpDir.getAbsolutePath());
		String file=new File(tmpDir,"/out2").getAbsolutePath();
		OutputStream os=tsi.getOutputStream(file);
		assertNotNull(os);
		OutputStreamWriter writer=new OutputStreamWriter(os);
		long start=System.currentTimeMillis();
		
		for(int i=0;i<size;i++){
			writer.write(theLine+"\n");
		}
		writer.close();
		
		File f=new File(file);
		System.out.println("length="+f.length());
		
		long duration=System.currentTimeMillis()-start;
		long rate=f.length()/duration;
		System.out.println(duration+" ms., "+rate+" kb/sec.");
		
		Thread.sleep(100);
		
		BufferedReader br=new BufferedReader(new InputStreamReader(
				new FileInputStream(file)));
		
		String line;
		while(true){
			line=br.readLine();
			if(line==null)break;
			assertEquals("Content does not match",theLine,line);
		}
		
		br.close();
		
		return rate;
	}
		
}
