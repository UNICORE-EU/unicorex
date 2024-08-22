package eu.unicore.xnjs.io;


/**
 * Describes a single ACL entry.
 * @author K. Benedyczak
 */
public class ACLEntry extends UNIXPermissionEntry {
	public enum Type {USER, GROUP};
	
	private boolean defaultACL = false;
	private Type type;
	private String subject;
	
	public ACLEntry(boolean isSet) {
		super(isSet);
	}

	public ACLEntry(Type type, String subject, String permissions, 
			boolean dafaultACL) throws IllegalArgumentException {
		super(false, permissions);
		setType(type);
		setSubject(subject);
		setDefaultACL(dafaultACL);
	}
	
	public ACLEntry(boolean isSet, Type type, String subject, String permissions, 
			boolean dafaultACL) throws IllegalArgumentException {
		super(isSet, permissions);
		setType(type);
		setSubject(subject);
		setDefaultACL(dafaultACL);
	}
	
	public Type getType() {
		return type;
	}
	
	public void setType(Type type) {
		this.type = type;
	}
	
	public String getSubject() {
		return subject;
	}
	
	public void setSubject(String subject) {
		this.subject = subject;
	}
	

	public void setDefaultACL(boolean defaultACL)
	{
		this.defaultACL = defaultACL;
	}

	public boolean isDefaultACL()
	{
		return defaultACL;
	}

	@Override
	public String toString() {
		return (defaultACL ? "default:" : "") + type.name() + ":" + subject + ":" + getPermissions();
	}
	
}
