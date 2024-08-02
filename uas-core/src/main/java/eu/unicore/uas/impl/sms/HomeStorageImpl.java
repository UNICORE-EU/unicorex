package eu.unicore.uas.impl.sms;

/**
 * Represents a HOME storage. The storage root is the current user's 
 * home directory on the target system.
 *
 * @author schuller
 */
public class HomeStorageImpl extends PathedStorageImpl {

	@Override
	protected String getDefaultWorkdir() {
		return "$HOME";
	}

}
