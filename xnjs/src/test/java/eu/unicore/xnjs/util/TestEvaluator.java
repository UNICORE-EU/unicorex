package eu.unicore.xnjs.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class TestEvaluator {
	
	@Test
	public void testEvaluator()throws Exception{
		String script = "A";
		Map<String,Object> vars = new HashMap<>();
		vars.put("A", 1);
		assertEquals(1, ScriptEvaluator.evaluate(script, vars));
		script = "A == 1";
		assertEquals(Boolean.TRUE, ScriptEvaluator.evaluate(script, vars));
		script = "A = A + 1";
		assertEquals(2, ScriptEvaluator.evaluate(script, vars));
		assertEquals(2, vars.get("A"));
	}

	@Test
	public void testEvaluateExpressions()throws Exception{
		String script = "A + B";
		Map<String,Object> vars = new HashMap<>();
		vars.put("A", 1);
		vars.put("B", 2);
		Object i = ScriptEvaluator.evaluate(script, vars);
		assertEquals((Integer)3,i);
	
		script = "3";
		vars.clear();
		i = ScriptEvaluator.evaluate(script, vars);
		assertEquals((Integer)3,i);
	
		script = "3.0 + A";
		vars.put("A", 1.5);
		Object d = ScriptEvaluator.evaluate(script, vars);
		assertEquals((Double)4.5,d);
		
		script =  "A + B";
		vars.put("A", "foo");
		vars.put("B", "bar");
		assertEquals("foobar", ScriptEvaluator.evaluate(script, vars));
		
		script = "(A>B)? A : B";
		vars.put("A", 11);
		vars.put("B", 2);
		assertEquals(11, ScriptEvaluator.evaluate(script, vars));
	}

	@Test
	public void testExtractExpressions()throws Exception{
		assertFalse(ScriptEvaluator.isScript("123"));
		String expr = "${A+B}";
		assertTrue(ScriptEvaluator.isScript(expr));
		assertEquals("A+B",ScriptEvaluator.extractScript(expr));
	}

}
