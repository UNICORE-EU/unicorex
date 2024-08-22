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

}
