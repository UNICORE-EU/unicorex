package eu.unicore.xnjs.util;

import java.io.Serializable;

public class ErrorCode implements Serializable {

	private static final long serialVersionUID = 1L;

	private final int code;
	
	private final String message;
	
	public ErrorCode(int code, String message){
		this.code=code;
		this.message=message;
	}
	
	
	public int getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}
	
	public String toString(){
		return message+" [XNJS error "+code+"]";
	}

	public boolean isWrongResourceSpec(){
		return code>=30 && code<40;
	}

	public boolean isNonRecoverableSubmissionError(){
		return code>=20 && code<30;
	}

	/**
	 * general unspecified error
	 */
	public static final int ERR_GENERAL=0;
	
	/**
	 * XNJS does not accept new work
	 */
	public static final int ERR_XNJS_DISABLED=1;
	
	/**
	 * requested operation is not possible
	 */
	public static final int ERR_OPERATION_NOT_POSSIBLE=2;
	

	/**
	 * requested action does not exist
	 */
	public static final int ERR_NO_SUCH_ACTION=3;
	
	/**
	 * TSI unavailable
	 */
	public static final int ERR_TSI_UNAVAILABLE=10;
	
	/**
	 * TSI communication error
	 */
	public static final int ERR_TSI_COMMUNICATION=11;
	
	/**
	 * TSI execution error
	 */
	public static final int ERR_TSI_EXECUTION=12;
	
	/**
	 * the requested application could not be found in the IDB
	 */
	public static final int ERR_UNKNOWN_APPLICATION=20;
	
	/**
	 * executing an executable is forbidden
	 */
	public static final int ERR_EXECUTABLE_FORBIDDEN=21;
	
	/**
	 * submitting a job on the login node failed
	 */
	public static final int ERR_INTERACTIVE_SUBMIT_FAILURE = 22;

	/**
	 * the requested resource does not exist on this XNJS
	 */
	public static final int ERR_UNKNOWN_RESOURCE=30;
	
	/**
	 * the requested resource(s) are not within the valid range
	 */
	public static final int ERR_RESOURCE_OUT_OF_RANGE=31;
	
	/**
	 * the requested resources are not consistent
	 */	
	public static final int ERR_RESOURCES_INCONSISTENT=32;
	

	/**
	 * the user application exited with non-zero exit code
	 */	
	public static final int ERR_NONZERO_EXITCODE=40;
}
