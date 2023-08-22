package de.fzj.unicore.xnjs.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

import eu.unicore.util.Log;

/**
 * evaluates expressions to generate dynamic values for resource requests
 * 
 * @author schuller
 */
public class ScriptEvaluator {

	private static final Logger logger=Log.getLogger(Log.SERVICES,ScriptEvaluator.class);

	/**
	 * @param script
	 * @param vars
	 * @return the value of the evaluated expression
	 * @throws IllegalArgumentException
	 */
	public Object evaluate(String script, final Map<String,Object> vars)throws IllegalArgumentException{
		if(vars==null){
			throw new IllegalArgumentException("Process variables can't be null.");
		}
		if(script==null){
			throw new IllegalArgumentException("Expression can't be null.");
		}
		SimpleEvaluationContext ctx = SimpleEvaluationContext.
				forPropertyAccessors(new Lookup(vars)).
				withInstanceMethods().
				withRootObject(new DefaultRootObject()).build();
		Expression expr = new SpelExpressionParser().parseExpression(script);
		logger.debug("Evaluating expression: {} with context {}", script, vars);
		return expr.getValue(ctx);
	}

	public String evaluateAsString(String script, Map<String, String> vars)throws  IllegalArgumentException{
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


	public static class Lookup implements PropertyAccessor {

		private final Map<String,Object> vars;

		Lookup(Map<String,Object> vars){
			this.vars = vars;
		}

		@Override
		public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
		}

		@Override
		public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
			return new TypedValue(vars.get(name));
		}

		@Override
		public Class<?>[] getSpecificTargetClasses() {
			return null;
		}

		@Override
		public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
			return false;
		}

		@Override
		public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
			return true;
		}
	}

	public static class DefaultRootObject {
		// TBD? - add some useful methods that can be used in the expressions
	}

}
