package de.fzj.unicore.uas.faults;

/**
 * thrown by the TSSClient when auto-start is attempted, but not supported by the
 * server
 *
 * @author schuller
 * @since 6.4.1
 */
public class AutoStartNotSupportedException extends Exception {

	private static final long serialVersionUID = 1L;

	public AutoStartNotSupportedException() {
	}

}
