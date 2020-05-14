/*********************************************************************************
 * Copyright (c) 2006-2008 Forschungszentrum Juelich GmbH 
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
 

package de.fzj.unicore.xnjs.tsi;

import de.fzj.unicore.xnjs.ems.ExecutionContext;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.security.Client;

/**
 * The execution management system's interface to the target system<br/>
 * <p>
 * This interface allows to access and modify files, and call executables on the
 * target system.
 * </p>
 * <p>
 * File system access: the TSI interface is a superset of the {@link IStorageAdapter}, allowing
 * to perform storage operations on the file system
 * </p>
 * <p>
 * Implementation note: TSIs are NOT intended to be threadsafe, each thread should use its 
 * own TSI instance
 * </p>
 * 
 * @author schuller
 */
public interface TSI extends IStorageAdapter{

	/**
	 * is the target system's file system local to the TSI?
	 * 
	 * @return true if filesystem is local
	 */
	public boolean isLocal();
	
	/**
	 * create a named pipe (FIFO) (probably Unix only)
	 * 
	 * @throws ExecutionException
	 */
	public void mkfifo(String name) throws ExecutionException;

	/**
	 * get the HOME directory of the current client
	 * 
	 * @return the HOME directory
	 * @throws ExecutionException
	 */
	public String getHomePath() throws ExecutionException;
	
	/**
	 * get an environment variable for the current client
	 * 
	 * @param name -  the name of the environment variable to retrieve
	 * @return the value of the variable
	 * @throws ExecutionException
	 */
	public String getEnvironment(String name) throws ExecutionException;
	
	/**
	 * Resolve a name in the current environment, e.g.
	 * /tmp/$ENV/foo.txt
	 * 
	 * @param name -  the path name to resolve
	 * @return the resolved value
	 * @throws ExecutionException
	 */
	public String resolve(String name) throws ExecutionException;
	
	/**
	 * execute a command asynchronously
	 * 
	 * @param what the shell script to execute
	 * @param ec the ExecutionContext
	 * @throws TSIBusyException
	 * @throws ExecutionException
	 */
	public void exec(String what, ExecutionContext ec) throws TSIBusyException,ExecutionException;
	
	/**
	 * execute a command synchronously
	 * 
	 * @param what the shell script to execute
	 * @param ec the ExecutionContext
	 * @throws TSIBusyException
	 * @throws ExecutionException
	 */
	public void execAndWait(String what, ExecutionContext ec) throws TSIBusyException,ExecutionException;
	
	/**
	 * set the client for which this TSI is performing work
	 */
	public void setClient(Client client);
	
	/**
	 * get the groups the current client is in
	 * @return list of groups
	 */
	public String[]getGroups() throws TSIBusyException, ExecutionException;
	
}
