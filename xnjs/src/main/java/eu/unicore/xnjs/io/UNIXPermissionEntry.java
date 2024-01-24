package eu.unicore.xnjs.io;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Describes a cluster of UNIX permissions, i.e. 'rwx'.
 * @author K. Benedyczak
 */
public class UNIXPermissionEntry {
	private String permissions;
	private final Pattern pattern;
	
	private static final Pattern GET_PATTERN = Pattern.compile("([r-][w-][x-])\\s*(#.*)*");
	private static final Pattern SET_PATTERN = Pattern.compile("([r-][w-][xX-])\\s*(#.*)*");
	
	public UNIXPermissionEntry(boolean isSet) {
		pattern = isSet ? SET_PATTERN : GET_PATTERN;
	}
	
	public UNIXPermissionEntry(boolean isSet, String permissions) throws IllegalArgumentException {
		this(isSet);
		//we allow for null permissions in case of remove ACEs
		if (permissions != null)
			setPermissions(permissions);
	}
	
	public String getPermissions() {
		return permissions;
	}
	
	public void setPermissions(String permissions) throws IllegalArgumentException {
		Matcher matcher = pattern.matcher(permissions);
		if (!matcher.matches())
			throw new IllegalArgumentException("Permissions string >" + 
				permissions + "< must match " + pattern.pattern() + " regular expression.");
		this.permissions = matcher.group(0);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((permissions == null) ? 0 : permissions.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UNIXPermissionEntry other = (UNIXPermissionEntry) obj;
		if (permissions == null) {
			if (other.permissions != null)
				return false;
		} else if (!permissions.equals(other.permissions))
			return false;
		return true;
	}

}
