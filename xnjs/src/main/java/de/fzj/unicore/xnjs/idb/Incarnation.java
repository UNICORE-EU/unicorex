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
 

package de.fzj.unicore.xnjs.idb;

import java.util.List;

import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ExecutionContext;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.resources.ResourceRequest;
import eu.unicore.security.Client;
import eu.unicore.security.Xlogin;

/**
 * An interface for realizing abstract resource definitions
 * (applications, paths, etc) onto the real ones on a target system <br/>
 * 
 * @author schuller
 */
public interface Incarnation {
	
	/**
	 * get the user Xlogin to use, by analysing the job description and
	 * the client object
	 * @see Xlogin
	 * @see Client
	 * 
	 * @param client - the client
	 * @param job - the job description
	 * @return the preferred user login to use
	 */
	public String getUserLogin(Client client, Object job);
	
	/**
	 * get the user group to use, by analysing the job description and
	 * the client object
	 * @see Xlogin
	 * @see Client
	 * 
	 * @param client - the client
	 * @param job - the job description
	 * @return the preferred user group to use
	 */
	public String getUserGroup(Client client, Object job);
	

	/**
	 * Generate a full {@link ApplicationInfo} object from the supplied "abstract" ApplicationInfo
	 */
	public ApplicationInfo incarnateApplication(ApplicationInfo job, Client client) throws ExecutionException;
	
	/**
	 * map a filename and a (possibly abstract) filesystem onto a concrete path
	 * 
	 * @param fileName
	 * @param fileSystem
	 * @param ec
	 * @param client
	 * @return path String denoting the absolute path
	 * @throws ExecutionException
	 */
	public String incarnatePath(String fileName, String fileSystem, ExecutionContext ec, Client client) throws ExecutionException;
		
	/**
	 * The resources requested by a job are converted to the list of resources
	 * that are actually used for job submission on the execution system<br/>
	 */
	public List<ResourceRequest> incarnateResources(Action job) throws ExecutionException;

	/**
	 * The resources requested by a client are converted to the list of resources
	 * that are actually used for job submission on the execution system<br/>
	 *  
	 * @param request - the requested resources
	 * @param client Client
	 */
	public List<ResourceRequest> incarnateResources(List<ResourceRequest> request, Client client) throws ExecutionException;

}
