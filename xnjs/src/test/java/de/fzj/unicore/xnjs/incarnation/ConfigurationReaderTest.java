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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.junit.Test;

public class ConfigurationReaderTest 
{
	private static final String TEST_CFG = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
"<tns:incarnationTweaker xmlns:tns=\"http://eu.unicore/xnjs/incarnationTweaker\"" +
"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
"</tns:incarnationTweaker>";
	
	@Test
	public void testUpdate()
	{
		try
		{
			File f = new File("target/test-classes/incarnationTweaker/tmp.xml");
			if (f.exists())
				f.delete();
			BufferedOutputStream fos = new BufferedOutputStream(
					new FileOutputStream(f.getAbsolutePath()));
			fos.write(TEST_CFG.getBytes());
			fos.close();
			f.setLastModified(System.currentTimeMillis());
			ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(2);
			TweakerConfiguration tc = new TweakerConfiguration(
				f, executor, 250, 10000);
			long firstLoad = tc.getLastRulesLoadTime();
			Thread.sleep(1000);

			fos = new BufferedOutputStream(
					new FileOutputStream(f.getAbsolutePath()));
			fos.write(TEST_CFG.getBytes());
			fos.write(' ');
			fos.close();
			f.setLastModified(System.currentTimeMillis());
			Thread.sleep(3000);
			assertTrue("" + firstLoad + " " + tc.getLastRulesLoadTime(), 
					tc.getLastRulesLoadTime() > firstLoad);
		} catch (Exception e)
		{
			e.printStackTrace();
			fail(e.toString());
		}
		
	}
	
	@Test
	public void testParsing()
	{
		try
		{
			ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(2);
			TweakerConfiguration tc = new TweakerConfiguration(
				new File("src/test/resources/incarnationTweaker/incarnationTweaker-empty.xml"),
				executor);
			assertTrue(tc.getAfterRules().getRulesList().isEmpty());
			assertTrue(tc.getBeforeRules().getRulesList().isEmpty());
			tc = new TweakerConfiguration(
				new File("src/test/resources/incarnationTweaker/incarnationTweaker-empty2.xml"),
				executor);
			assertTrue(tc.getAfterRules().getRulesList().isEmpty());
			assertTrue(tc.getBeforeRules().getRulesList().isEmpty());
			
			tc = new TweakerConfiguration(
				new File("src/test/resources/incarnationTweaker/incarnationTweaker.xml"),
				executor);
			List<Rule<BeforeAction>> res = tc.getBeforeRules().getRulesList();
			assertTrue(res.size() == 2);
			Rule<BeforeAction> r1 = res.get(0);
			assertTrue(!r1.isFinishOnHit());
			assertTrue(r1.getCondition().getExpressionString().equals("2 == 2"));
			assertTrue(r1.getActions().size() == 2);
			assertTrue(r1.getActions().get(0).getActionDefinition().equals(
					"resources.individualCPUTime = 300"));
			assertTrue(r1.getActions().get(0) instanceof SpelAction);
			assertTrue(r1.getActions().get(1).getActionDefinition().equals(
				"resources.individualPhysicalMemory=1024"));
			assertTrue(r1.getActions().get(1) instanceof SpelAction);
			
			Rule<BeforeAction> r2 = res.get(1);
			assertTrue(!r2.isFinishOnHit());
			assertTrue(r2.getCondition().getExpressionString().equals("2!=2"));
			assertTrue(r2.getActions().size() == 1);
			assertTrue(r2.getActions().get(0).getActionDefinition().equals(
					"System.exit()"));
			assertTrue(r2.getActions().get(0) instanceof GroovyAction);

			
			List<Rule<AfterAction>> res2 = tc.getAfterRules().getRulesList();
			assertTrue(res2.size() == 1);
			Rule<AfterAction> r3 = res2.get(0);
			assertTrue(r3.isFinishOnHit());
			assertTrue(r3.getCondition().getExpressionString().equals("resources != null"));
			assertTrue(r3.getActions().size() == 2);
			assertTrue(r3.getActions().get(0).getActionDefinition().equals(
					"String text = input.getText()\nprint(text)\n"));
			assertTrue(r3.getActions().get(0) instanceof GroovyFileFilter);
			assertTrue(r3.getActions().get(1).getActionDefinition().equals(
				"\"/bin/cat\""));
			assertTrue(r3.getActions().get(1) instanceof SpelFilter);
			
		} catch (Exception e)
		{
			e.printStackTrace();
			fail(e.toString());
		}
		
	}
}
