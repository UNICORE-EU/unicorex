/*********************************************************************************
 * Copyright (c) 2009 Forschungszentrum Juelich GmbH 
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
 ********************************************************************************/
 

package de.fzj.unicore.xnjs.io;

import java.io.InputStream;
import java.io.OutputStream;

import de.fzj.unicore.xnjs.ems.ExecutionException;

/**
 * Support for accessing hierarchical backend storage systems.<br/>
 * 
 * All paths (whether used as input parameters or returned as results) 
 * are <em>relative</em> to the "root" of the storage, which can be set
 * using {@link #setStorageRoot(String)}
 * 
 * @author schuller
 */
public interface IStorageAdapter {

	/**
	 * The default umask used if non is set. 
	 */
	public static final int DEFAULT_UMASK = 0077;
	
	/**
	 * Initial regular file permissions, are and-ed with a umask. 
	 */
	public static final int DEFAULT_FILE_PERMS = 0666;

	/**
	 * Initial directory permissions, are and-ed with a umask. 
	 */
	public static final int DEFAULT_DIR_PERMS = 0777;
	
	/**
	 * Set the root path for storage operations. This path should be 
	 * resolved, i.e. not contain any variables such as HOME
	 *  
	 * @param root - the root directory for the storage methods 
	 */
	public void setStorageRoot(String root);

	/**
	 * get the currently set root path for storage operations
	 */
	public String getStorageRoot();

	/**
	 * create an input stream for reading from the given resource
	 * 
	 * @param resource - the resource to read from
	 * @return {@link InputStream}
	 */
	public InputStream getInputStream(String resource)throws ExecutionException;
	

	/**
	 * create an OutputStream for writing the given number of bytes to the given resource.
	 * 
	 * @param resource - the resource to write to
	 * @param append - <code>true</code> if file should be appended to
	 * @param numBytes - the number of bytes to write, which can be negative if not known
	 */
	public OutputStream getOutputStream(String resource, boolean append, long numBytes)throws ExecutionException;
	
	/**
	 * create an OutputStream for writing to the given resource
	 * @param resource - the resource to write to
	 * @param append - <code>true</code> if file should be appended to
	 */
	public OutputStream getOutputStream(String resource, boolean append)throws ExecutionException;
	
	/**
	 * create an OutputStream for writing to the given resource, overwriting if it exists
	 * @param resource - the resource to write to
	 */
	public OutputStream getOutputStream(String resource)throws ExecutionException;
	
	/**
	 * create a directory (including any necessary parent directories)<br/>
	 *
	 * @param dir - the dir to create (relative to storage root)
	 * @throws ExecutionException if the directory could not be created
	 */
	public void mkdir(String dir) throws ExecutionException;

	/**
	 * set permissions on a file
	 * 
	 * @param file - the file for which to change permissions
	 * @param perm - the new permissions
	 * @throws ExecutionException if permissions could not be changed
	 */
	public void chmod(String file, Permissions perm) throws ExecutionException;

	/**
	 * set permissions on a file, allowing for a nearly full UNIX chmod syntax.
	 * 
	 * @param file - the file for which to change permissions
	 * @param perm - the new permissions
	 * @param recursive
	 * @throws ExecutionException if permissions could not be changed
	 */
	public void chmod2(String file, ChangePermissions[] perm, boolean recursive) throws ExecutionException;

	/**
	 * set owning group of a given file
	 * 
	 * @param file - the file for which to change owning group
	 * @param newGroup - the new group
	 * @param recursive
	 * @throws ExecutionException if group could not be changed
	 */
	public void chgrp(String file, String newGroup, boolean recursive) throws ExecutionException;

	/**
	 * change ACL of a given file
	 * 
	 * @param file - the file for which to change ACL
	 * @param clearAll - whether to clear all ACL entries prior to applying other changes
	 * @param changeACL - the ACL change specification
	 * @param recursive
	 * @throws ExecutionException if ACL could not be changed
	 */
	public void setfacl(String file, boolean clearAll, ChangeACL[] changeACL, boolean recursive) throws ExecutionException;
	
	/**
	 * remove a file
	 * @param target - the file to remove
	 * @throws ExecutionException if file could not be removed
	 */
	public void rm(String target) throws ExecutionException;
	
	/**
	 * remove a directory (need not be empty)
	 * @param target - the directory to remove
	 * @throws ExecutionException if directory could not be removed
	 */
	public void rmdir(String target) throws ExecutionException;
	
	/**
	 * copy a file
	 * @param source - the file to copy
	 * @param target - the copy
	 * 
	 * @throws ExecutionException if file could not be copied
	 */
	public void cp(String source, String target) throws ExecutionException;
	
	/**
	 * creates a soft link (UNIX only)
	 * 
	 * @param target - the link target (i.e. the real file)
	 * @param linkName - the name of the link
	 * @throws ExecutionExcepion
	 */
	public void link(String target, String linkName)throws ExecutionException;
	
	/**
	 * rename a file
	 * @param source - the file to rename
	 * @param target - the new name
	 * 
	 * @throws ExecutionException if file could not be renamed
	 */
	public void rename(String source, String target) throws ExecutionException;

	/**
	 * list files
	 * @param base - the base resource from which to list
	 * @return XnjsFile[]
	 * @throws ExecutionException
	 */
	public XnjsFile[] ls(String base) throws ExecutionException;
	
	/**
	 * list files, while limiting the number
	 * of results
	 * 
	 * @return XnjsFile[]
	 * @param base - the base resource from which to list
	 * @param offset - the index of the first result to return
	 * @param limit - the maximum number of results to return
	 * @param filter - if true, only include files owned or accessible by the caller
	 * @throws ExecutionException
	 */
	public XnjsFile[] ls(String base, int offset, int limit, boolean filter) throws ExecutionException;
	
	
	/**
	 * execute a "find" from the current working directory
	 * 
	 * @param base - the base path
	 * @param options - filtering options
	 * @param offset - the index of the first result to return
	 * @param limit - the maximum number of results to return
	 * @return XnjsFile[]
	 * @throws ExecutionException
	 */
	public XnjsFile[] find(String base, FileFilter options, int offset, int limit)throws ExecutionException;

	/**
	 * get an {@link XnjsFile} describing the given file
	 * @param file - the resource to check
	 * @return an {@link XnjsFile} or <code>null</code> if file does not exist
	 * @throws ExecutionException if property check runs into problems
	 */
	public XnjsFileWithACL getProperties(String file)throws ExecutionException;


	/**
	 * get the file separator
	 */
	public String getFileSeparator();

	/**
	 * get the identifier that <b>uniquely identifies</b> the "file system" represented by
	 * this storage.<br/> 
	 * The contract is such that if the filesystem IDs of two IStorageAdapter
	 * instances match, they represent the "same" store, so a "cp()" operation 
	 * could be used to "transfer" files from one to the other
	 * 
	 * @return an identifier. If none can be determined, <code>null</code> is returned
	 */
	public String getFileSystemIdentifier(); 

	/**
	 * for the given path, get the available disk space (in bytes), 
	 * or <code>-1</code> if not known
	 */
	public XnjsStorageInfo getAvailableDiskSpace(String path);
	
	/**
	 * for the given path, return whether ACL are supported
	 * @throws ExecutionException 
	 */
	public boolean isACLSupported(String path) throws ExecutionException;
	
	/**
	 * Set a umask which shall be used for new files and directories creation.
	 * @param umask A new umask as a (maximally) 3 digit octal number, passed as a string. 
	 * If the value is null then TSI sets umask to DEFAULT_UMASK  
	 */
	public void setUmask(String umask);

	/**
	 * Returns a currently used umask.
	 * @return octal umask in a string form
	 */
	public String getUmask();
}
