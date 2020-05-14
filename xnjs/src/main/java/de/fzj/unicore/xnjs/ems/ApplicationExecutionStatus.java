/*********************************************************************************
 * Copyright (c) 2011 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *********************************************************************************/
 

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
