package eu.unicore.xnjs.tsi;

import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.util.ErrorCode;

/**
 * thrown when the TSI cannot be contacted
 */
public class TSIUnavailableException extends ExecutionException {

	private static final long serialVersionUID = 1L;

	public TSIUnavailableException() {
		super(ErrorCode.ERR_TSI_UNAVAILABLE, null);
	}

	public TSIUnavailableException(String message) {
		super(ErrorCode.ERR_TSI_UNAVAILABLE, message);
	}

}
