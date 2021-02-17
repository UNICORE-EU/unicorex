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

import org.json.JSONObject;
import org.junit.Test;

import de.fzj.unicore.xnjs.ConfigurationSource;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.EMSTestBase;
import eu.unicore.security.Client;
import eu.unicore.security.Role;
import eu.unicore.security.Xlogin;

public class IntegrationTest extends EMSTestBase {

	private static String d1="src/test/resources/json/date.json";

	@Override
	protected void addProperties(ConfigurationSource cs){
		super.addProperties(cs);
		cs.getProperties().put("XNJS.incarnationTweakerConfig",
				new File("src/test/resources/incarnationTweaker/incarnationTweaker2.xml").getAbsolutePath());
	}

	@Test
	public void testSubmit1() throws Exception {
		JSONObject job = loadJSONObject(d1);
		String id="";
		Action a=xnjs.makeAction(job);
		Client c=new Client();
		a.setClient(c);
		c.setXlogin(new Xlogin(new String[] {"nobody"}));
		c.setRole(new Role(new String[] {"anonymous", "someRole"}));

		id=a.getUUID();
		mgr.add(a,c);
		doRun(id);
		// must reload action from storage
		a=internalMgr.getAction(id);
		assertTrue(a.getClient().getSelectedXloginName().equals("anonymous"));
		assertTrue(a.getClient().getRole().getName().equals("someRole"));
	}
}
