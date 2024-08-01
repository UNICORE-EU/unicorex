package eu.unicore.xnjs.tsi;

import eu.unicore.xnjs.ems.ExecutionException;

public class TSIProblem extends ExecutionException {

	private static final long serialVersionUID=1L;

	private final String tsiHost;

	public TSIProblem(String tsiHost, int code, String message, Throwable cause) {
		super(code, message, cause);
		this.tsiHost = tsiHost;
	}

	public String getMessage() {
		if(tsiHost==null) {
			return super.getMessage();
		}
		else {
			StringBuilder sb = new StringBuilder();
			sb.append("TSI <"+tsiHost+">");
			sb.append(": ").append(super.getMessage());
			return sb.toString();
		}
	}
}
