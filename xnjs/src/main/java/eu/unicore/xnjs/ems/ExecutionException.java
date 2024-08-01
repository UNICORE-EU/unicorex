package eu.unicore.xnjs.ems;

import eu.unicore.xnjs.util.ErrorCode;

/**
 * This exception is thrown upon errors during the processing of an action.
 * It includes an integer error code {@link ErrorCode} that can be retrieved
 * from the exception hierarchy using {@link #getRootErrorCode()}
 * @author schuller
 */
public class ExecutionException extends Exception {
	
	private static final long serialVersionUID=1L;
	
	private int errorCode=-1;
	
	public ExecutionException(){
		this(ErrorCode.ERR_GENERAL, null, null);
	}
	
	public ExecutionException(int code, String message){
		this(code, message, null);
	}
	
	public ExecutionException(String message){
		this(ErrorCode.ERR_GENERAL, message, null);
	}

	public ExecutionException(String message, Throwable cause){
		this(ErrorCode.ERR_GENERAL, message, cause);
	}

	public ExecutionException(Throwable cause) {
		this(ErrorCode.ERR_GENERAL, "", cause);
	}
	
	public ExecutionException(int code, String message, Throwable cause) {
		super(message,cause);
		this.errorCode = code;
	}

	public int getErrorCode() {
		return errorCode;
	}

	/**
	 * returns the error code from this exception's root cause, or <code>null</code> if no such error code
	 * is set
	 */
	public int getRootErrorCode() {
		if(getCause()!=null && getCause() instanceof ExecutionException){
			ExecutionException ee=(ExecutionException)getCause();
			return ee.getErrorCode();
		}
		return errorCode;
	}

	@Override
	public String getMessage() {
		StringBuilder sb = new StringBuilder();
		if(super.getMessage()!=null) {
			sb.append(super.getMessage());
			sb.append(" - ");
		}
		if(errorCode>-1) {
			sb.append(ErrorCode.toString(errorCode));
		}
		return sb.toString();
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
