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

import de.fzj.unicore.xnjs.util.ErrorCode;
import de.fzj.unicore.xnjs.util.LogUtil;

/**
 * This exception is thrown upon general ems errors. It includes an {@link ErrorCode} that
 * can be retrieved from the exception hierarchy using {@link #getRootErrorCode()}
 * @author schuller
 */
public class ExecutionException extends Exception {
	
	private static final long serialVersionUID=1L;
	
	private final ErrorCode errorCode;
	
	public ExecutionException(){
		this(new ErrorCode(ErrorCode.ERR_GENERAL,""));
	}
	
	public ExecutionException(int code, String message){
		this(new ErrorCode(code,message));
	}
	
	public ExecutionException(String message){
		this(new ErrorCode(ErrorCode.ERR_GENERAL,message));
	}
	
	public ExecutionException(ErrorCode ec){
		super(ec.getMessage());
		this.errorCode=ec;
	}
	
	public ExecutionException(Throwable cause) {
		super(cause);
		if(cause instanceof ExecutionException){
			errorCode=((ExecutionException)cause).getErrorCode();
		}
		else this.errorCode=new ErrorCode(ErrorCode.ERR_GENERAL,LogUtil.getDetailMessage(cause));
	}
	
	public ExecutionException(String message, Throwable cause) {
		super(message,cause);
		if(cause instanceof ExecutionException){
			errorCode=((ExecutionException)cause).getErrorCode();
		}
		else this.errorCode=new ErrorCode(ErrorCode.ERR_GENERAL,LogUtil.getDetailMessage(cause));
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}

	/**
	 * returns the error code from this exception's root cause, or <code>null</code> if no such error code
	 * is set
	 */
	public ErrorCode getRootErrorCode() {
		if(getCause()!=null && getCause() instanceof ExecutionException){
			ExecutionException ee=(ExecutionException)getCause();
			return ee.getErrorCode();
		}
		return errorCode;
	}

	public static ExecutionException wrapped(Exception cause) throws ExecutionException {
		if(cause instanceof ExecutionException){
			return (ExecutionException)cause;
		}
		else{
			return new ExecutionException(cause);
		}
	}
	
}
