/*********************************************************************************
 * Copyright (c) 2007 Forschungszentrum Juelich GmbH 
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

import java.io.File;

/**
 * stores read,write,execute permissions</br>
 * toString() gives a representation suitable for use with "chmod"
 */
public class Permissions {

	private boolean isExecutable;
	
	private boolean isWritable;
	
	private boolean isReadable;

	public Permissions(){
	}
	
	public Permissions(boolean isReadable, boolean isWritable, boolean isExecutable)
	{
		this.isExecutable=isExecutable;
		this.isReadable=isReadable;
		this.isWritable=isWritable;
	}
	
	public boolean isExecutable() {
		return isExecutable;
	}

	public void setExecutable(boolean isExecutable) {
		this.isExecutable = isExecutable;
	}

	public boolean isReadable() {
		return isReadable;
	}

	public void setReadable(boolean isReadable) {
		this.isReadable = isReadable;
	}

	public boolean isWritable() {
		return isWritable;
	}

	public void setWritable(boolean isWritable) {
		this.isWritable = isWritable;
	}
	
	public boolean isAccessible() {
		return isWritable || isReadable || isExecutable;
	}
	
	public String toString(){
		String r=isReadable?"r":"-";
		String w=isWritable?"w":"-";
		String x=isExecutable?"x":"-";
		return r+w+x;
	}
	

	public String toChmodString(){
		String r=isReadable?"r":"";
		String w=isWritable?"w":"";
		String x=isExecutable?"x":"";
		return r+w+x;
	}
	
	/**
	 * Get the permissions for the given File using Java means
	 * 
	 * @param f File
	 * @return permissions
	 */
	public static Permissions getPermissions(File f){
		boolean r=f.canRead();
		boolean w=f.canWrite();
		boolean x=f.canExecute();

		return new Permissions(r,w,x);
	}

}
