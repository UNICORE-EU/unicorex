/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *********************************************************************************/
 

package de.fzj.unicore.xnjs.io;

import java.util.Calendar;

/**
 * Standard implementation of {@link XnjsFile}
 *  
 * @author schuller
 */
public class XnjsFileImpl implements XnjsFileWithACL {

	private String path;
	
	private long size;
	
	private boolean isDirectory;
	
	private boolean isOwnedByCaller;
	
	private Permissions permissions;
	
	private Calendar lastModified;
	
	private String metadata;
	
	private String owner;
	
	private String group;
	
	private String unixPermissions;
	
	private ACLEntry[] acl;

	public XnjsFileImpl(){
	}

	public XnjsFileImpl(String path,long size,boolean isDirectory,long lastModified,
			Permissions permissions,boolean isOwnedByCaller){
		this.path=path;
		this.isDirectory=isDirectory;
		this.size=size;
		this.lastModified=Calendar.getInstance();
		this.lastModified.setTimeInMillis(lastModified);
		this.permissions=permissions;
		this.isOwnedByCaller=isOwnedByCaller;
	}
	
	public XnjsFileImpl(String path,long size,boolean isDirectory,long lastModified,
			Permissions permissions,boolean isOwnedByCaller, String owner, 
			String group, String unixPermissions){
		this(path, size, isDirectory, lastModified, permissions, isOwnedByCaller);
		this.owner = owner;
		this.group = group;
		this.unixPermissions = unixPermissions;
	}
	
	public Calendar getLastModified() {
		return lastModified;
	}

	public String getPath() {
		return path;
	}

	public long getSize() {
		return size;
	}

	public boolean isDirectory() {
		return isDirectory;
	}
	
	public String toString(){
		return getPath()+" ["+getSize()+" bytes] ["+(isDirectory()?"d":"-")+permissions+"] "+lastModified.getTime();
	}

	public void setDirectory(boolean isDirectory) {
		this.isDirectory = isDirectory;
	}

	public void setLastModified(Calendar lastModified) {
		this.lastModified = lastModified;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public Permissions getPermissions() {
		return permissions;
	}

	public void setPermissions(Permissions permissions) {
		this.permissions = permissions;
	}

	public boolean isOwnedByCaller() {
		return isOwnedByCaller;
	}

	public void setOwnedByCaller(boolean isOwnedByCaller) {
		this.isOwnedByCaller = isOwnedByCaller;
	}

	public String getMetadata() {
		return metadata;
	}

	public void setMetadata(String metadata) {
		this.metadata=metadata;
	}

	@Override
	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	@Override
	public String getGroup() {
		return group;
	}

	@Override
	public String getUNIXPermissions() {
		return unixPermissions;
	}

	public void setUNIXPermissions(String unixPermissions) {
		this.unixPermissions = unixPermissions;
	}

	@Override
	public ACLEntry[] getACL()
	{
		return acl;
	}

	public void setACL(ACLEntry[] acl)
	{
		this.acl = acl;
	}
}
