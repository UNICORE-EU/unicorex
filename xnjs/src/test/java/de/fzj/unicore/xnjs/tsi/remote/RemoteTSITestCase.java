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

package de.fzj.unicore.xnjs.tsi.remote;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.google.inject.AbstractModule;

import de.fzj.unicore.xnjs.BaseModule;
import de.fzj.unicore.xnjs.ConfigurationSource;
import de.fzj.unicore.xnjs.ems.EMSTestBase;
import de.fzj.unicore.xnjs.tsi.local.LocalExecution.DataMover;

/**
 * this  starts a TSI server (on ports 65431/65432)
 */
public abstract class RemoteTSITestCase extends EMSTestBase {

	@After
	public void tearDown() throws Exception {
		if(xnjs!=null){
			((DefaultTSIConnectionFactory)xnjs.get(TSIConnectionFactory.class)).stop();
		}
		super.tearDown();
	}

	protected RemoteTSI makeTSI(){
		return (RemoteTSI)xnjs.getTargetSystemInterface(null);
	}

	@Override
	protected void addProperties(ConfigurationSource cs){
		super.addProperties(cs);
		String p = TSIProperties.PREFIX;
		Properties props = cs.getProperties();
		props.put(p+TSIProperties.TSI_MACHINE,getTSIMachine());
		props.put(p+TSIProperties.TSI_PORT,getTSIPort());
		props.put(p+TSIProperties.TSI_MYPORT,"65432");
		props.put(p+TSIProperties.TSI_BSSUSER,System.getProperty("user.name"));
		props.put(p+TSIProperties.BSS_UPDATE_INTERVAL,"2000");
	}

	protected void addModules(ConfigurationSource cs){
		cs.addModule(new BaseModule(cs.getProperties()));
		cs.addModule(getTSIModule(cs));
		cs.addModule(getPersistenceModule());
	}

	protected AbstractModule getTSIModule(ConfigurationSource cs){
		return new RemoteTSIModule(cs.getProperties());
	}


	protected String getFileSpace(){
		File f=new File("target/xnjs_test_"+System.currentTimeMillis());
		return f.getAbsolutePath();
	}

	protected String getTSIMachine(){
		return "127.0.0.1";
	}

	protected String getTSIPort(){
		return "65431";
	}

	@BeforeClass
	public static void startTSI() throws Exception {
		ProcessBuilder pb=new ProcessBuilder();
		File tsiExec=new File("src/test/resources/tsi/bin/start.sh");
		pb.command(tsiExec.getAbsolutePath());
		Process p=pb.start();
		DataMover m=new DataMover(p.getInputStream(),System.out);
		m.run();
		int exitCode=p.waitFor();
		Thread.sleep(500);
		System.out.println("TSI started.");
		if(exitCode!=0)throw new IOException("TSI start returned non-zero exit code <"+exitCode+">");
	}

	@AfterClass
	public static void stopTSI() throws Exception {
		ProcessBuilder pb=new ProcessBuilder();
		File tsiExec=new File("src/test/resources/tsi/bin/stop.sh");
		pb.command(tsiExec.getAbsolutePath());
		Process p=pb.start();
		DataMover m=new DataMover(p.getInputStream(),System.out);
		m.run();
		int exitCode=p.waitFor();
		if(exitCode!=0)throw new IOException("TSI stop returned non-zero exit code");
	}

}
