package eu.unicore.xnjs.ems;

import java.io.Serializable;

/**
 * 
 * @author schuller
 */
public class ActionResult implements Serializable {

	private static final long serialVersionUID=1L;
	
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
