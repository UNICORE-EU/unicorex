package eu.unicore.xnjs.ems;

import eu.unicore.xnjs.util.ErrorCode;
import eu.unicore.xnjs.util.LogUtil;

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
