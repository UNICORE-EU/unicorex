package de.fzj.unicore.xnjs.tsi;

import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.util.ErrorCode;

/**
 * thrown when the TSI cannot be contacted
 */
public class TSIUnavailableException extends ExecutionException {

	private static final long serialVersionUID = 1L;

	public TSIUnavailableException() {
		super(new ErrorCode(ErrorCode.ERR_TSI_UNAVAILABLE, "TSI unavailable"));
	}

	public TSIUnavailableException(String message) {
		super(new ErrorCode(ErrorCode.ERR_TSI_UNAVAILABLE, message));
	}

}
