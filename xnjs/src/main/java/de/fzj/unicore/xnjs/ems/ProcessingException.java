package de.fzj.unicore.xnjs.ems;

import de.fzj.unicore.xnjs.util.ErrorCode;

/**
 * Exception thrown during request processing
 * @author schuller
 */
public class ProcessingException extends ExecutionException{
	
	private static final long serialVersionUID=1L;
	
	public ProcessingException(ErrorCode ec){
		super(ec);
	}
	public ProcessingException(String msg){
		super(msg);
	}
	public ProcessingException(Throwable t){
		super(t);
	}
	public ProcessingException(String msg,Throwable t){
		super(msg,t);
	}

}
