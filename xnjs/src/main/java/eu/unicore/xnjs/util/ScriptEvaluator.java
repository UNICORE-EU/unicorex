package eu.unicore.xnjs.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.mvel2.MVEL;

import eu.unicore.util.Log;

/**
 * evaluates expressions to generate dynamic values for resource requests
 * 
 * @author schuller
 */
public class ScriptEvaluator {

	private static final Logger logger=Log.getLogger(Log.SERVICES,ScriptEvaluator.class);

	private ScriptEvaluator() {}

	/**
	 * @param script
	 * @param vars
	 * @return the value of the evaluated expression
	 * @throws IllegalArgumentException
	 */
	public static Object evaluate(String script, final Map<String,Object> vars)throws IllegalArgumentException{
		if(vars==null){
			throw new IllegalArgumentException("Process variables can't be null.");
		}
		if(script==null){
			throw new IllegalArgumentException("Expression can't be null.");
		}
		logger.debug("Evaluating expression: {} with context {}", script, vars);
		return MVEL.eval(script, vars);
	}

	public static String evaluateAsString(String script, Map<String, String> vars)throws  IllegalArgumentException{
		Map<String,Object> var2 = new HashMap<>();
		for(Map.Entry<String, String> entry: vars.entrySet()) {
			Object v = entry.getValue();
			try {
				v = Double.valueOf(entry.getValue());
			}catch(Exception e) {}
			var2.put(entry.getKey(), v);
		}
		return String.valueOf(evaluate(script, var2));
	}

	/**
	 * for an expression of the form '${expr}' extract the actual 
	 * expression (the 'expr')
	 */
	public static String extractScript(String expr){
		return expr.substring(2, expr.length()-1);
	}

	public static boolean isScript(String expr){
		return expr!=null && expr.startsWith("${") && expr.endsWith("}");
	}

}
