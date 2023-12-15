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
