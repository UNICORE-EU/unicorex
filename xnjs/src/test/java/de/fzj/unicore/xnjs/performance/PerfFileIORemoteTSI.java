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
import de.fzj.unicore.xnjs.tsi.remote.RemoteTSITestCase;

/**
 * filetransfer performance tests using a TSI server
 */
public class PerfFileIORemoteTSI extends RemoteTSITestCase {

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
