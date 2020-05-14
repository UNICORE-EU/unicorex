/*
 * Copyright (c) 2011 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 05-06-2011
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package de.fzj.unicore.xnjs.io;

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
	
}
