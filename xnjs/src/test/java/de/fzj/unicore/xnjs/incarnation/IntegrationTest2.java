/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 17-12-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package de.fzj.unicore.xnjs.incarnation;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.junit.Test;

import de.fzj.unicore.xnjs.ConfigurationSource;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.tsi.remote.LegacyTSITestCase;
import eu.unicore.security.Client;
import eu.unicore.security.Role;
import eu.unicore.security.Xlogin;

public class IntegrationTest2 extends LegacyTSITestCase
{
	private static String 
		d1="src/test/resources/ems/sleep.jsdl";

	
	@Override
	protected void addProperties(ConfigurationSource cs){
		super.addProperties(cs);
		cs.getProperties().put("XNJS.incarnationTweakerConfig",
				new File("src/test/resources/incarnationTweaker/incarnationTweaker4.xml").getAbsolutePath());
	}

	@Test
	public void testSubmit1() throws Exception {
		JobDefinitionDocument job=getJSDLDoc(d1);
		String id="";
			Action a=xnjs.makeAction(job);
			Client c=new Client();
			a.setClient(c);
			c.setXlogin(new Xlogin(new String[] {"nobody"}));
			c.setRole(new Role("anonymous", ""));
			id=a.getUUID();
			mgr.add(a,c);
			doRun(id);
			a=internalMgr.getAction(id);
			assertTrue(a.getResult().getErrorMessage().contains("Test ERROR"));
			assertTrue(!a.getResult().isSuccessful());
	}
}
