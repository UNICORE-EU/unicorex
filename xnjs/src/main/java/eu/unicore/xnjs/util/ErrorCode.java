package eu.unicore.xnjs.util;

public class ErrorCode {

	private ErrorCode() {}

	/**
	 * get a somewhat user friendly error message
	 * @param code the error code
	 */
	public static String toString(int code){
		String msg;
		switch(code){
		case ERR_XNJS_DISABLED:
			msg = "XNJS does not accept new actions";
			break;
		case ERR_INTERACTIVE_SUBMIT_FAILURE:
			msg = "Submission to login node failed";
			break;
		case ERR_TSI_COMMUNICATION:
			msg = "Error communicating to the TSI";
			break;
		case ERR_TSI_EXECUTION:
			msg = "Command execution on TSI reported an error";
			break;
		case ERR_EXECUTABLE_FORBIDDEN:
			msg = "User executable is not allowed";
			break;
		case ERR_RESOURCE_OUT_OF_RANGE:
		case ERR_UNKNOWN_RESOURCE:
		case ERR_RESOURCES_INCONSISTENT:
			msg = "Invalid resource request";
			break;
		case ERR_UNKNOWN_APPLICATION:
			msg = "Requested application is not defined";
			break;
		case ERR_NONZERO_EXITCODE:
			msg = "User executable exited with non-zero exit code";
			break;
		case ERR_JOB_DESCRIPTION:
			msg = "Job description error";
			break;
		default:
			msg = "XNJS error";
		}
		return msg + "["+code+"]";
	}

	public static boolean isWrongResourceSpec(int code){
		return code>=30 && code<40;
	}

	public static boolean isNonRecoverableSubmissionError(int code){
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
	
	/**
	 * Error in the job description
	 */	
	public static final int ERR_JOB_DESCRIPTION=50;
}
