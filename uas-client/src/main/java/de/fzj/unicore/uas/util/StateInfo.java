package de.fzj.unicore.uas.util;

import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * annotation used on a public instance method 
 * to mark a state transition 
 * 
 * @author schuller
 */
@Documented
@Target({METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface StateInfo {
 
	/**
	 * the name of the state
	 */
	public String name();
	
	/**
	 * which state to move into when this method completes without error
	 */
	public String onSuccess();
	
	/**
	 * which state to move into when an error occurs in this method
	 */
	public String onError();
	
	/**
	 * whether to retry the method in case of error
	 */
	public int retryCount() default 0;

	/**
	 * whether this is the initial state
	 */
	public boolean isInitial() default false;

	/**
	 * support for "asynchronous" state transitions: if this is true,
	 * the state machine can "pause" after this state and continue later
	 */
	public boolean isPausable() default true;
	
	/**
	 * whether this is a terminal state
	 */
	public boolean isTerminal() default false;

	/**
	 * how often should a failed transition be retried
	 */
	public int nmberOfRetries() default 0;
	
	/**
	 * delay in millis between retry attempts
	 */
	public int retryDelay() default 0;
	
}
