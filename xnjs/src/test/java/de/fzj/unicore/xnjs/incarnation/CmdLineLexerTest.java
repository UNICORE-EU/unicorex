/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 17-12-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package de.fzj.unicore.xnjs.incarnation;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class CmdLineLexerTest {
	
	@Test
	public void test()
	{
		String []res;
		res = SimplifiedCmdLineLexer.tokenizeString("");
		assertTrue(Arrays.toString(res), 
				Arrays.equals(res, new String[] {}));
		res = SimplifiedCmdLineLexer.tokenizeString("   ");
		assertTrue(Arrays.toString(res), 
				Arrays.equals(res, new String[] {}));
		res = SimplifiedCmdLineLexer.tokenizeString("  s s ");
		assertTrue(Arrays.toString(res), 
				Arrays.equals(res, new String[] {"s", "s"}));
		res = SimplifiedCmdLineLexer.tokenizeString("  s  s");
		assertTrue(Arrays.toString(res), 
				Arrays.equals(res, new String[] {"s", "s"}));
		res = SimplifiedCmdLineLexer.tokenizeString("  \"s  s\" ");
		assertTrue(Arrays.toString(res), 
				Arrays.equals(res, new String[] {"s  s"}));
		res = SimplifiedCmdLineLexer.tokenizeString("  \"s  s\" df \"\"");
		assertTrue(Arrays.toString(res), 
				Arrays.equals(res, new String[] {"s  s", "df"}));
		res = SimplifiedCmdLineLexer.tokenizeString("\\  \"s\\ \\ s\" df \"\"");
		assertTrue(Arrays.toString(res), 
				Arrays.equals(res, new String[] {" ", "s  s", "df"}));
		res = SimplifiedCmdLineLexer.tokenizeString("s\\ s\\  d ");
		assertTrue(Arrays.toString(res), 
				Arrays.equals(res, new String[] {"s s ", "d"}));
	}
}
