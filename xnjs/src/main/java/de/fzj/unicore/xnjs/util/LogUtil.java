package de.fzj.unicore.xnjs.util;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import de.fzj.unicore.xnjs.ems.Action;
import eu.unicore.util.Log;

public class LogUtil {

	private LogUtil(){}
	
	/**
	 * log category: general XNJS
	 */
	public static final String XNJS="unicore.xnjs";

	/**
	 * log category: general persistence
	 */
	public static final String PERSISTENCE="unicore.persistence";

	/**
	 * log category: job execution
	 */
	public static final String JOBS="unicore.services.jobexecution";

	/**
	 * log category: tsi
	 */
	public static final String TSI="unicore.xnjs.tsi";

	/**
	 * log category: i/o
	 */
	public static final String IO="unicore.xnjs.io";

	/**
	 * returns a logger, using the given prefix and the simple name
	 * of the given class
	 * 
	 * @param prefix - the prefix to use
	 * @param clazz - the class
	 * @return logger
	 */
	public static Logger getLogger(String prefix, Class<?>clazz){
		return Log.getLogger(prefix, clazz);
	}

	/**
	 * log an error message to the specified logger.
	 * A human-friendly message is constructed and logged at "INFO" level.
	 * The stack trace is logged at "DEBUG" level.
	 * 
	 * @param message - the error message
	 * @param cause - the cause of the error
	 * @param logger - the logger to use
	 */
	public static void logException(String message, Throwable cause, Logger logger){
		Log.logException(message, cause, logger);
	}
	
	/**
	 * construct a (hopefully) useful error message from the root cause of an 
	 * exception
	 * @param throwable
	 * @return the cause of the exception
	 */
	public static String getDetailMessage(Throwable throwable){
		StringBuilder sb=new StringBuilder();
		Throwable cause=throwable;
		String message=null;
		String type=null;type=cause.getClass().getName();
		do{
			type=cause.getClass().getName();
			message=cause.getMessage();
			cause=cause.getCause();
		}
		while(cause!=null);
		
		sb.append(type).append(" ");
		if(message!=null){
			sb.append(message);
		}
		else sb.append(" (no further message available)");
		return sb.toString();
	}
	
	public static String createFaultMessage(String message, Throwable cause){
		return message+": "+getDetailMessage(cause);
	}
	
	
	public static void fillLogContext(Action a){
		ThreadContext.put("jobID",a.getUUID());
		if(a.getClient()!=null && a.getClient().getDistinguishedName()!=null){
			ThreadContext.put("clientName",a.getClient().getDistinguishedName());
		}
	}
	
	public static void clearLogContext(){
		ThreadContext.remove("jobID");
		ThreadContext.remove("clientName");
	}
}
