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

import java.io.Serializable;

public class ActionResult implements Serializable {
	private static final long serialVersionUID=8348462510L;
	
	public static final int UNKNOWN=0;
	public static final int USER_ABORTED=1;
	public static final int NOT_SUCCESSFUL=2;
	public static final int SUCCESSFUL=3;
	
	private static String[] names={"UNKNOWN","USER_ABORTED","NOT_SUCCESSFUL","SUCCESSFUL"};
	
	private int code;
	private String errorMessage;
	private int exitCode;
	
	public ActionResult(int status, String error, int exitCode){
		this.code=status;
		this.errorMessage=error;
		this.exitCode=exitCode;
	}
	
	public ActionResult(int status, String error){
		this(status, error, 0);
	}
	
	public ActionResult(int status){
		this(status, null, 0);
	}
	
	public ActionResult(){
		this(UNKNOWN, null, 0);
	}
	
	/**
	 * @return Returns the code.
	 */
	public String getStatusString() {
		return names[code];
	}
	/**
	 * @return Returns the code.
	 */
	public int getStatusCode() {
		return code;
	}
	/**
	 * @param code The code to set.
	 */
	public void setStatusCode(int code) {
		this.code = code;
	}
	/**
	 * @return Returns the errorMessage.
	 */
	public String getErrorMessage() {
		return errorMessage;
	}
	/**
	 * @param errorMessage The errorMessage to set.
	 */
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	/**
	 * @return Returns the exitCode.
	 */
	public int getExitCode() {
		return exitCode;
	}
	/**
	 * @param exitCode The exitCode to set.
	 */
	public void setExitCode(int exitCode) {
		this.exitCode = exitCode;
	}
	
	/**
	 * is this result a "successful" result?
	*/
	public boolean isSuccessful(){
		return code==SUCCESSFUL;
	}
	
	/**
	 * get a user-friendly representation of this result
	 */
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(names[code]);
		if(errorMessage!=null){
			sb.append(" [").append(errorMessage).append("]");
		}
		return sb.toString();
	}
}
