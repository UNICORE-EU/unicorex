package de.fzj.unicore.xnjs.io;


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
	
	public String toString() {
		return (defaultACL ? "default:" : "") + type.name() + ":" + subject + ":" + getPermissions();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (defaultACL ? 1231 : 1237);
		result = prime * result + ((subject == null) ? 0 : subject.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ACLEntry other = (ACLEntry) obj;
		if (defaultACL != other.defaultACL)
			return false;
		if (subject == null) {
			if (other.subject != null)
				return false;
		} else if (!subject.equals(other.subject))
			return false;
		if (type != other.type)
			return false;
		return true;
	}
	
}
