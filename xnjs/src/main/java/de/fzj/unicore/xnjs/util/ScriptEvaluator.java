package de.fzj.unicore.xnjs.util;

import java.util.Map;

import org.apache.log4j.Logger;

import eu.unicore.util.Log;
import groovy.lang.GroovyShell;

/**
 * evaluates Groovy scripts to generate values sets for iterators 
 * 
 * @author schuller
 */
public class ScriptEvaluator {

	private static final Logger logger=Log.getLogger(Log.SERVICES,ScriptEvaluator.class);

	private final ScriptSandbox sandbox; 
	
	public ScriptEvaluator(){
		this.sandbox=new ScriptSandbox();
	}

	/**
	 * @param script
	 * @param vars
	 * @return the value of the evaluated expression
	 * @throws IllegalArgumentException
	 */
	public Object evaluate(String script, Map<String,String> vars)throws IllegalArgumentException{
		if(vars==null){
			throw new IllegalArgumentException("Process variables can't be null.");
		}
		if(script==null){
			throw new IllegalArgumentException("Expression can't be null.");
		}
		GroovyShell interpreter = new GroovyShell();
		prepareInterpreter(interpreter, vars);
		if(logger.isDebugEnabled()){
			logger.debug("Evaluating expression: "+script+" with context "+vars);
		}
		return sandbox.eval(interpreter,script);
	}

	public String evaluateToString(String script, Map<String,String> vars)throws  IllegalArgumentException{
		return String.valueOf(evaluate(script, vars));
	}

	public Integer evaluateToInteger(String script, Map<String,String> vars)throws IllegalArgumentException{
		return Integer.valueOf(evaluate(script, vars).toString());
	}
	
	public Double evaluateToDouble(String script, Map<String,String> vars)throws IllegalArgumentException{
		return Double.valueOf(evaluate(script, vars).toString());
	}

	private void prepareInterpreter(GroovyShell interpreter, Map<String,String> vars){
		for(String key: vars.keySet()){
			String val=vars.get(key);
			interpreter.setVariable(key, makeSpecific(val));
		}
	}

	private Object makeSpecific(String value){
		try{
			return Integer.parseInt(value);
		}
		catch(NumberFormatException nfe){
			try{
				return Double.parseDouble(value);
			}
			catch(NumberFormatException nfe2){
			}	
		}
		return value;
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
