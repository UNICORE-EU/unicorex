package de.fzj.unicore.xnjs.util;

import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import de.fzj.unicore.xnjs.ems.Action;

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
		return Logger.getLogger(prefix+"."+clazz.getSimpleName());
	}

	/**
	 * returns a logger, using the given prefix and the given name
	 * 
	 * @param prefix - the prefix to use
	 * @param name - the name
	 * @return logger
	 */
	public static Logger getLogger(String prefix, String name){
		return Logger.getLogger(prefix+"."+name);
	}
	
	/** 
	 * log an error message to the default logger ("unicore.wsrflite")
	 * A human-friendly message is constructed and logged at "INFO" level.
	 * The stack trace is logged at "DEBUG" level.
	 * 
	 * @param message - the error message
	 * @param cause - the cause of the error
	 *
	 */
	public static void logException(String message, Throwable cause){
		logException(message,cause,Logger.getLogger(XNJS));
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
		logger.error(message);
		if(cause!=null){
			logger.error("The root error was: "+getDetailMessage(cause));
			if(logger.isDebugEnabled())logger.debug("Stack trace",cause);
			else{
				logger.error("To see the full error stack trace, set log4j.logger."+logger.getName()+"=DEBUG");
			}
		}
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
		MDC.put("jobID",a.getUUID());
		if(a.getClient()!=null && a.getClient().getDistinguishedName()!=null){
			MDC.put("clientName",a.getClient().getDistinguishedName());
		}
	}
	
	public static void clearLogContext(){
		MDC.remove("jobID");
		MDC.remove("clientName");
	}
}
