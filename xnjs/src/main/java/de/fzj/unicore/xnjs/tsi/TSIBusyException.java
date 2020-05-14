package de.fzj.unicore.xnjs.tsi;


/**
 * Thrown when the TSI is busy, and application code should 
 * retry the action after some time<br/>
 */
public class TSIBusyException extends Exception {

	private static final long serialVersionUID = 1L;

	public TSIBusyException(String msg) {
		super(msg);
	}

	public TSIBusyException(Throwable t) {
		super(t);
	}

}
