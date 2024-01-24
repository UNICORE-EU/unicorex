package eu.unicore.xnjs.fts;

import java.io.Serializable;

public class SourceFileInfo implements Serializable {

	private static final long serialVersionUID = 1l;
	
	private String path;
	
	private boolean isDirectory;
	
	private long lastModified = -1;
	
	private long size = 0;
	
	private String permissions;

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public boolean isDirectory() {
		return isDirectory;
	}

	public void setDirectory(boolean isDirectory) {
		this.isDirectory = isDirectory;
	}

	public long getLastModified() {
		return lastModified;
	}

	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}

	public String getPermissions() {
		return permissions;
	}

	public void setPermissions(String permissions) {
		this.permissions = permissions;
	}
	
	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public String toString() {
		return path;
	}

}
