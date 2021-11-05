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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

import de.fzj.unicore.xnjs.ConfigurationSource;
import de.fzj.unicore.xnjs.tsi.TSIUnavailableException;

public class TestTSIConnectionPool extends RemoteTSITestCase {
	
	@Test
	public void testConnectionFactory()throws Exception{
		DefaultTSIConnectionFactory f = (DefaultTSIConnectionFactory)xnjs.get(TSIConnectionFactory.class);
		assertNotNull(f);
		
		List<TSIConnection>connections = new ArrayList<>();
		
		for(int i = 0; i<8; i++){
			TSIConnection c=f.getTSIConnection("nobody", null, null, -1);
			connections.add(c);
		}
		System.out.println("Connections : "+f.getLiveConnections());
		assertEquals(8, f.getLiveConnections());
		try{
			// check limit is respected
			f.getTSIConnection("nobody", null, null, -1);
			fail("Should hit limit");
		}catch(TSIUnavailableException te){}
		
		// put back into pool
		for(TSIConnection c: connections){
			c.close();
		}
		System.out.println("Pooled connections : "+f.getNumberOfPooledConnections());
		assertEquals(4, f.getNumberOfPooledConnections());
		
	}
	
	@Test
	public void testConnectorRestart() throws Exception {
		DefaultTSIConnectionFactory f = (DefaultTSIConnectionFactory)xnjs.get(TSIConnectionFactory.class);
		assertNotNull(f);
		TSIConnection c=f.getTSIConnection("nobody", null, null, -1);
		c.close();
		f.getTSISocketFactory().reInit();
		c=f.getTSIConnection("nobody", null, null, -1);
		c.close();
	}

	@Test
	public void testHostnameMatch() throws Exception {
		String[] requests = new String[] { "*", "node*", "node01*", "node??.cluster.org",
				"node01", "node01.cluster.org" };
		String actual = "node01.cluster.org";
		for(int i=0; i<requests.length; i++) {
			String want = requests[i];
			assertTrue(DefaultTSIConnectionFactory.matches(want, actual));
		}
		
		requests = new String[] { "node??.cluster.org" };
		actual = "node-special1.cluster.org";
		for(int i=0; i<requests.length; i++) {
			String want = requests[i];
			assertFalse(DefaultTSIConnectionFactory.matches(want, actual));
		}
		
		
	}
	
	@Override
	protected void addProperties(ConfigurationSource cs){
		super.addProperties(cs);
		String p = TSIProperties.PREFIX;
		Properties props = cs.getProperties();
		props.put(p+TSIProperties.TSI_POOL_SIZE,"4");
		props.put(p+TSIProperties.TSI_WORKER_LIMIT,"8");
		props.put(p+TSIProperties.BSS_UPDATE_INTERVAL,"30000");
	}

}
