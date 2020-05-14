/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 17-12-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package de.fzj.unicore.xnjs.incarnation;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import de.fzj.unicore.xnjs.ems.ExecutionException;

public class SpELTest {
	
	@Test
	public void testAction()
	{
		try
		{
			RootCtxBean bean = new MockContextBean();

			SpelAction a;

			a = new SpelAction("resources.individualCPUTime=200.0");
			a.invoke(bean);
			assertTrue(bean.getResources().getIndividualCPUTime() == 200.0);

			a = new SpelAction("resources.individualCPUCount=100.0");
			a.invoke(bean);
			assertTrue(bean.getResources().getIndividualCPUCount() == 100.0);
			a = new SpelAction("app.executable=\"exec\"");
			a.invoke(bean);
			assertTrue(bean.getApp().getExecutable().equals("exec"));

			a = new SpelAction("app.preCommand=\"aaa\"");
			a.invoke(bean);
		
			a = new SpelAction("app.arguments={\"-v\"}");
			a.invoke(bean);
			assertTrue(bean.getApp().getArguments().get(0).equals("-v"));
			
		} catch(Exception e)
		{
			e.printStackTrace();
			fail(e.toString());
		}
	}
	
	@Test
	public void testFilter()
	{
		try
		{
			RootCtxBean bean = new MockContextBean();

			String orig = "ORIG\nSCRIPT\nAAA";
			SpelFilter f = new SpelFilter("java -cp target/test-classes de.fzj.unicore.xnjs.incarnation.JavaCat", 3000);
			String result = f.filter(bean, orig);
			assertTrue("Got '" + result + "'", result.equals(orig+"\n"));
			
			f = new SpelFilter("java -cp target/test-classes de.fzj.unicore.xnjs.incarnation.JavaEcho " +
					"${app.version} \"${client.distinguishedName}\"", 3000);
			result = f.filter(bean, orig);
			assertTrue("Got '" + result + "'", result.equals("'1.0'\n'CN=Test,O=TestOrg,C=PL'\n"));
			
			f = new SpelFilter("java -cp target/test-classes de.fzj.unicore.xnjs.incarnation.JavaEcho " +
					"${app.version}\\ \"${client.distinguishedName}\"" +
					" \" a a \"   f\\ f", 3000);
			result = f.filter(bean, orig);
			assertTrue("Got '" + result + "'", result.equals("'1.0 CN=Test,O=TestOrg,C=PL'\n' a a '\n'f f'\n"));
		} catch (Exception e)
		{
			e.printStackTrace();
			fail(e.toString());
		}
	}
	
	@Test
	public void testScriptAction()
	{
		try
		{
			RootCtxBean bean = new MockContextBean();

			SpelScriptAction f = new SpelScriptAction("java -cp target/test-classes " +
					"de.fzj.unicore.xnjs.incarnation.JavaEcho ${app.version}", 3000);
			f.invoke(bean);
		} catch (Exception e)
		{
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Test
	public void testScriptError()
	{
		try
		{
			RootCtxBean bean = new MockContextBean();

			SpelScriptAction f = new SpelScriptAction("java -cp target/test-classes " +
					"de.fzj.unicore.xnjs.incarnation.JavaErrorReporter", 3000);
			f.invoke(bean);
			fail("Expected exception not received.");
		} catch (ExecutionException ee)
		{
			assertTrue("Test ERROR".equals(ee.getErrorCode().getMessage()));
		} catch (Exception e)
		{
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Test
	public void testScriptFilterError()
	{
		try
		{
			RootCtxBean bean = new MockContextBean();

			SpelFilter f = new SpelFilter("java -cp target/test-classes " +
					"de.fzj.unicore.xnjs.incarnation.JavaErrorReporter", 3000);
			f.filter(bean, "ABC");
			fail("Expected exception not received.");
		} catch (ExecutionException ee)
		{
			assertTrue("Test ERROR".equals(ee.getErrorCode().getMessage()));
		} catch (Exception e)
		{
			e.printStackTrace();
			fail(e.toString());
		}
	}
	
	@Test
	public void testScriptTimeout()
	{
		try
		{
			RootCtxBean bean = new MockContextBean();

			SpelScriptAction a = new SpelScriptAction("java -cp target/test-classes " +
					"de.fzj.unicore.xnjs.incarnation.JavaWait", 1000);
			try
			{
				a.invoke(bean);
				fail("Didn't kill the hung process");
			} catch (Exception e)
			{
				//OK
				assertTrue(e.getMessage().startsWith("Killed the invoked"));
			}
			
			String orig = "ORIG\nSCRIPT\nAAA";
			SpelFilter f = new SpelFilter("java -cp target/test-classes " +
					"de.fzj.unicore.xnjs.incarnation.JavaWait", 1000);
			try
			{
				f.filter(bean, orig);
				fail("Didn't kill the hung process");
			} catch (Exception e)
			{
				//OK
				assertTrue(e.getMessage().startsWith("Killed the invoked"));
			}
		} catch (Exception e)
		{
			e.printStackTrace();
			fail(e.toString());
		}
	}

}
