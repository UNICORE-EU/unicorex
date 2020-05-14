/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 17-12-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package de.fzj.unicore.xnjs.incarnation;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import de.fzj.unicore.xnjs.ems.ExecutionException;
import eu.emi.security.authn.x509.impl.X500NameUtils;

public class GroovyTest {
	
	@Test
	public void testAction()
	{
		try
		{
			TestContextBean ctx = new TestContextBean();
			GroovyAction a = new GroovyAction(
				"principal = new javax.security.auth.x500.X500Principal(\"CN=New,O=TestOrg2,C=PL\")\n" +
				"securityT = new eu.unicore.security.SecurityTokens()\n" +
				"securityT.userName=principal\n" +
				"securityT.consignorTrusted=true\n" +
				"client.setAuthenticatedClient(securityT)\n" +
				"app.version=2.2\n" +
				"resources.individualCPUCount=100;");
			a.invoke(ctx);
			assertNotNull(ctx.getClient().getDistinguishedName());
			assertTrue(ctx.getClient().getDistinguishedName(),
					X500NameUtils.equal(ctx.getClient().getDistinguishedName(), 
							"CN=New,O=TestOrg2,C=PL"));
			assertTrue(ctx.getApp().getVersion().equals("2.2"));
			assertTrue(ctx.getResources().getIndividualCPUCount() == 100);
			
		} catch(Exception e)
		{
			e.printStackTrace();
			fail(e.toString());
		}
	}

	public void testActionError()
	{
		try
		{
			TestContextBean ctx = new TestContextBean();
			GroovyAction a = new GroovyAction(
				"throw new de.fzj.unicore.xnjs.ems.ExecutionException(" +
				"de.fzj.unicore.xnjs.util.ErrorCode.ERR_EXECUTABLE_FORBIDDEN" +
				", \"Test ERROR\");");
			a.invoke(ctx);
			fail("No error received");
		} catch(ExecutionException e)
		{
			//OK
		} catch(Exception e)
		{
			e.printStackTrace();
			fail("Got unexpected exception " + e.toString());
		}
	}

	public void testFilterError()
	{
		try
		{
			TestContextBean ctx = new TestContextBean();
			GroovyFilter f = new GroovyFilter(
				"throw new de.fzj.unicore.xnjs.ems.ExecutionException(" +
				"de.fzj.unicore.xnjs.util.ErrorCode.ERR_EXECUTABLE_FORBIDDEN" +
				", \"Test ERROR\");");
			f.filter(ctx, "ABC");
			fail("No error received");
		} catch(ExecutionException e)
		{
			//OK
		} catch(Exception e)
		{
			e.printStackTrace();
			fail("Got unexpected exception " + e.toString());
		}
	}
	
	
	public void testFilter()
	{
		try
		{
			TestContextBean ctx = new TestContextBean();
			GroovyFilter f = new GroovyFilter(
					"print \"ORIG\\nTEXT\""
			);
			String result = f.filter(ctx, "blah blah");
			assertTrue(result.equals("ORIG\nTEXT"));
			
			f = new GroovyFilter(
				"String text = input.getText()\n" +
				"print(text)\n"
			);
			String input = "blah blah\nexit\n";
			result = f.filter(ctx, input);
			
			assertTrue("Got: '" + result + "'", result.equals(input));

			String groovyS = "int i=0;\n" + 
			"input.eachLine() { line -> \n" +  
			"if(i==1) {\n" +
			"     println(\"ADDED\");\n"+
			"     println(line);\n"+
			"} else  \n"+
			"     println(line);\n"+
			"i++;\n"+
			"}";
			f = new GroovyFilter(groovyS);
			input = "line1\nline2\nline3";
			result = f.filter(ctx, input);
				
			assertTrue("Got: '" + result + "'", 
					result.equals("line1\nADDED\nline2\nline3\n"));

		} catch(Exception e)
		{
			e.printStackTrace();
			fail(e.toString());
		}
	}

	public void testFileFilter()
	{
		try
		{
			TestContextBean ctx = new TestContextBean();
			GroovyFileFilter f = new GroovyFileFilter(
					"src/test/resources/incarnationTweaker/groovyScript.gs");
			String input = "aaa ddd\n sdfsd";
			String result = f.filter(ctx, input);
			assertTrue("Got: '" + result + "'", result.equals(input));
		} catch(Exception e)
		{
			e.printStackTrace();
			fail(e.toString());
		}
	}

}
