package eu.unicore.xnjs.io;

/**
 * Change ACL request.
 * 
 * @author K. Benedyczak
 */
public class ChangeACL extends ACLEntry {

	public enum ACLChangeMode {REMOVE, MODIFY};

	private ACLChangeMode changeMode;

	public ChangeACL() {
		super(true);
	}

	public ChangeACL(Type type, String subject, String permissions, boolean defaultACL,
			ACLChangeMode changeMode) {
		super(true, type, subject, permissions, defaultACL);
		setChangeMode(changeMode);
	}

	public ACLChangeMode getChangeMode() {
		return changeMode;
	}

	public void setChangeMode(ACLChangeMode changeMode) {
		this.changeMode = changeMode;
	}

}
