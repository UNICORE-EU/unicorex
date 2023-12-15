package de.fzj.unicore.xnjs.ems;

import java.io.Serializable;

/**
 * sub-status codes for tracking the execution of the actual application
 * including pre- and post-commands
 *  
 * @author schuller
 */
public class ApplicationExecutionStatus implements Serializable{
	
	private static final long serialVersionUID = 1L;

	private int status;
	
	/**
	 * create a new application execution status in state CREATED
	 */
	public ApplicationExecutionStatus(){
		this.status=CREATED;
	}
	
	public ApplicationExecutionStatus(int status){
		this.status=status;
	}
	
	public int get(){
		return status;
	}
	
	public void set(int status){
		this.status=status;
	}
	
	//??
	public static final int UNKNOWN=-1;
	
	//just created
	public static final int CREATED=0;
	
	//precommand(s) are executing
	public static final int PRECOMMAND_EXECUTION=1;
	
	public static final int PRECOMMAND_DONE=2;
	
	//main thing is running
	public static final int MAIN_EXECUTION=50;
	//main thing is running
	public static final int MAIN_EXECUTION_DONE=51;
	
	//post-command(s) are executing
	public static final int POSTCOMMAND_EXECUTION=70;
	
	//finished execution
	public static final int DONE=99;
		
	public String toString(){
		return asString(status);
	}
	
	public static String asString(int s){
		switch(s){
			case UNKNOWN: return "UNKNOWN";
			case CREATED: return "CREATED";
			case PRECOMMAND_EXECUTION: return "PRECOMMAND_EXECUTION";
			case PRECOMMAND_DONE: return "PRECOMMAND_DONE";
			case MAIN_EXECUTION: return "MAIN_EXECUTION";
			case MAIN_EXECUTION_DONE: return "MAIN_EXECUTION_DONE";
			case POSTCOMMAND_EXECUTION: return "POSTCOMMAND_EXECUTION";
			case DONE: return "DONE";
		}
		return null;
	}

	public static ApplicationExecutionStatus done(){
		return new ApplicationExecutionStatus(DONE);
	}
	public static ApplicationExecutionStatus precommandDone(){
		return new ApplicationExecutionStatus(PRECOMMAND_DONE);
	}
	public static ApplicationExecutionStatus precommandRunning(){
		return new ApplicationExecutionStatus(PRECOMMAND_EXECUTION);
	}
	public static ApplicationExecutionStatus mainExecutionDone(){
		return new ApplicationExecutionStatus(MAIN_EXECUTION_DONE);
	}
	public static ApplicationExecutionStatus postcommandRunning(){
		return new ApplicationExecutionStatus(POSTCOMMAND_EXECUTION);
	}
}
