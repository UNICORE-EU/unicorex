package eu.unicore.uas.impl.sms;

/**
 * Defines what type of SMS sharing implementation are used.
 * @author K. Benedyczak
 */
public enum StorageSharingMode
{
	/**
	 * Sharing operations are disabled and will fail.
	 */
	DISABLED,
	
	/**
	 * Sharing operations will be enabled. If ACL is supported then it is used, otherwise fall back to 
	 * chmod based implementation.
	 */
	PREFER_ACL,
	
	/**
	 * ACL mode will be enforced. If ACL is not supported on a storage then sharing will fail.
	 */
	ACL,
	
	/**
	 * CHMOD based implementation will be enforced.
	 */
	CHMOD
}
