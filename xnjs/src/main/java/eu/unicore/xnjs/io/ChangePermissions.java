package eu.unicore.xnjs.io;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Request to change permissions in UNIX syntax
 * @author K. Benedyczak
 */
public class ChangePermissions extends UNIXPermissionEntry {
	public enum Mode {ADD, SET, SUBTRACT};
	public enum PermissionsClass {OWNER, GROUP, OTHER};
	
	private Mode mode;
	private PermissionsClass clazz;
	
	public ChangePermissions() {
		super(true);
	}
	
	public ChangePermissions(Mode mode, PermissionsClass clazz,
			String permissions) throws IllegalArgumentException {
		super(true, permissions);
		this.mode = mode;
		this.clazz = clazz;
	}
	
	public Mode getMode() {
		return mode;
	}
	
	public String getModeOperator() {
		if (mode == Mode.ADD)
			return "+";
		if (mode == Mode.SUBTRACT)
			return "-";
		return "=";
	}
	
	public void setMode(Mode mode) {
		this.mode = mode;
	}
	
	public PermissionsClass getClazz() {
		return clazz;
	}
	
	public String getClazzSymbol() {
		if (clazz == PermissionsClass.GROUP)
			return "g";
		if (clazz == PermissionsClass.OWNER)
			return "u";
		return "o";
	}
	
	public void setClazz(PermissionsClass clazz) {
		this.clazz = clazz;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
		result = prime * result + ((mode == null) ? 0 : mode.hashCode());
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
		ChangePermissions other = (ChangePermissions) obj;
		if (clazz != other.clazz)
			return false;
		if (mode != other.mode)
			return false;
		return true;
	}
	
	
	private static final String[]jsonPermKinds = {"OWNER","GROUP","OTHER"};


	private static final Pattern unixPermPattern = Pattern.compile("([rwx-][rwx-][rwx-])");

	/**
	 * convert the UNIX style permissions to XNJS chmod2 request
	 * @param unixPermissions - UNIX style permissions string like "rwx------"
	 */
	public static eu.unicore.xnjs.io.ChangePermissions[] getChangePermissions(String unixPermissions) {
		if(!unixPermissions.matches("([rwx-][rwx-][rwx-]){1,3}")) {
			throw new IllegalArgumentException("Illegal permissions string <"+unixPermissions+">");
		}
		Matcher m = unixPermPattern.matcher(unixPermissions);
		List<ChangePermissions>res = new ArrayList<>();
		int i=0;
		while(m.find() && i<3) {
			String kind = jsonPermKinds[i];
			String perm = m.group();
			if(perm!=null){
				eu.unicore.xnjs.io.ChangePermissions cp = new eu.unicore.xnjs.io.ChangePermissions();
				cp.setClazz(PermissionsClass.valueOf(kind));
				cp.setMode(Mode.SET);
				cp.setPermissions(perm);
				res.add(cp);
			}
			i++;
		}
		return res.toArray(new ChangePermissions[res.size()]);
	}
}
