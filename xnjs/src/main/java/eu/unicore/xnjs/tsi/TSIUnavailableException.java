package eu.unicore.xnjs.tsi;

import eu.unicore.xnjs.util.ErrorCode;

/**
 * thrown when the TSI cannot be contacted
 */
public class TSIUnavailableException extends TSIProblem {

	private static final long serialVersionUID = 1L;

	public TSIUnavailableException() {
		this(null, null);
	}

	public TSIUnavailableException(String host) {
		this(host, null);
	}

	public TSIUnavailableException(String host, Throwable cause) {
		super(host, ErrorCode.ERR_TSI_UNAVAILABLE, null, cause);
	}
}
