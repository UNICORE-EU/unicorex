/*
 * Copyright (c) 2011 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 05-06-2011
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package de.fzj.unicore.xnjs.io;

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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((changeMode == null) ? 0 : changeMode.hashCode());
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
		ChangeACL other = (ChangeACL) obj;
		if (changeMode != other.changeMode)
			return false;
		return true;
	}

}
