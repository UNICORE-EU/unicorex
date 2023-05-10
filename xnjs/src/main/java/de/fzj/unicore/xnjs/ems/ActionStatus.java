/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
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

/**
 * EMS status codes for actions,
 * and some static methods for modelling 
 * the state chart and state transitions
 * 
 * @author schuller
 */
public class ActionStatus {
	//don't want instances of this
	private ActionStatus(){}
	
	//status codes
	
	//??
	public static final int UNKNOWN=-1;
	
	//just created
	public static final int CREATED=0;
	
	//staging in input data
	public static final int PREPROCESSING=1;
	
	//ready to go
	public static final int READY=2;
	
	//to be submitted to the BSS shortly
	public static final int PENDING=21;
	
	//submitted to the BSS, but not yet running.
	public static final int QUEUED=22;
	
	//executing, but paused
	public static final int PAUSED=3;
	
	//permanently paused (checkpointed)
	public static final int FROZEN=4;
	
	//running
	public static final int RUNNING=5;
	
	//staging out data
	public static final int POSTPROCESSING=6;
	
	//finished execution
	public static final int DONE=7;
	
	//to be removed from XNJS storage
	public static final int DESTROYED=8;
	
	//transition codes....
	public static final int TRANSITION_NONE=0;
	public static final int TRANSITION_ABORTING=2;
	public static final int TRANSITION_PAUSING=3;
	public static final int TRANSITION_RESUMING=4;
	public static final int TRANSITION_REMOVING=5;
	public static final int TRANSITION_RESTARTING=6;
	
	
	public static boolean canRun(int as){
		switch(as){
			case CREATED:
			case PREPROCESSING:
			case READY:
			case PENDING:
				return true;
			default: return false;
		}
	}
	
	/**
	 * can we abort?
	 * @param as the status code
	 * @return boolean
	 */
	public static boolean canAbort(int as){
		switch(as){
			case DONE:
				return false;
			default: return true;
		}
	}
	
	/**
	 * can we pause?
	 * @param as the status code
	 * @return boolean
	 */
	public static boolean canPause(int as){
		switch(as){
			case DONE:
			case PAUSED:
				return false;
			default: return true;
		}
	}
	/**
	 * can we continue?
	 * @param as the status code
	 * @return boolean
	 */
	public static boolean canResume(int as){
		switch(as){
			case PAUSED:
				return true;
			default: return false;
		}
	}
	
	public static boolean canRestart(int as){
		switch(as){
			case DONE:
				return true;
			default: return false;
		}
	}
	
	public static String transitionalStatus(int s){
		switch (s) {
		case TRANSITION_NONE:
			return "(trans.: none)";
		case TRANSITION_ABORTING:
			return "(trans.: aborting)";
		case TRANSITION_REMOVING:
			return "(trans.: removing)";
		case TRANSITION_PAUSING:
			return "(trans.: pausing)";
		case TRANSITION_RESUMING:
			return "(trans.: resuming)";
		case TRANSITION_RESTARTING:
			return "(trans.: restarting)";
			
		default:
			break;
		}
		return "(trans.: invalid)";
	}
	
	public static String toString(int s){
		switch(s){
			case UNKNOWN: return "UNKNOWN";
			case CREATED: return "CREATED";
			case PREPROCESSING: return "PREPROCESSING";
			case READY: return "READY";
			case PENDING: return "PENDING";
			case QUEUED: return "QUEUED";
			case PAUSED: return "PAUSED";
			case FROZEN: return "FROZEN";
			case RUNNING: return "RUNNING";
			case POSTPROCESSING: return "POSTPROCESSING";
			case DONE: return "DONE";
			case DESTROYED: return "DESTROYED";
		}
		return null;
	}
	
	public static int fromString(String s){
		switch(s){
			case "UNKNOWN": return UNKNOWN;
			case "CREATED": return CREATED;
			case "PREPROCESSING": return PREPROCESSING;
			case "STAGINGIN": return PREPROCESSING;
			case "READY": return READY;
			case "PENDING": return PENDING;
			case "QUEUED": return QUEUED;
			case "PAUSED": return PAUSED;
			case "FROZEN": return FROZEN;
			case "RUNNING": return RUNNING;
			case "POSTPROCESSING": return POSTPROCESSING;
			case "STAGINGOUT": return POSTPROCESSING;
			case "DONE": return DONE;
			case "SUCCESSFUL": return DONE;
			case "DESTROYED": return DESTROYED;
		}
		throw new IllegalArgumentException("Not a UNICORE status: "+s);
	}
}
