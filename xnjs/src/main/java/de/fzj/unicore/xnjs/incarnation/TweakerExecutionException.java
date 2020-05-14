package de.fzj.unicore.xnjs.incarnation;

import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.util.ErrorCode;

public class TweakerExecutionException extends ExecutionException {

	private static final long serialVersionUID = 1L;

	public TweakerExecutionException(int code, String message) {
		super(new ErrorCode(code, message));
	}

	public TweakerExecutionException(String message) {
		super(new ErrorCode(ErrorCode.ERR_GENERAL, message));
	}

	public TweakerExecutionException(ErrorCode ec) {
		super(ec);
	}
	
}