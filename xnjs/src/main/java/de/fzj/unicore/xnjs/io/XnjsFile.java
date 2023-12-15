package de.fzj.unicore.xnjs.io;

import java.util.Calendar;

/**
 * 
 * Describes a file<br/>
 * 
 * inspired by UniGrids' GridFile type
 * 
 * @author schuller
 */
public interface XnjsFile {

	public String getPath();
	
	public long getSize();
	
	public Calendar getLastModified();
	
	public boolean isDirectory();
	
	public Permissions getPermissions();
	
	public boolean isOwnedByCaller();

	public String getMetadata();
	
	public String getOwner();
	
	public String getGroup();
	
	public String getUNIXPermissions();
}
