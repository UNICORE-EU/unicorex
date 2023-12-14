/*********************************************************************************
 * Copyright (c) 2018 Forschungszentrum Juelich GmbH 
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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.resources.Resource;
import eu.unicore.security.Client;

/**
 * Allows to lookup information necessary for task incarnation
 * and for publishing applications and resources to the outside
 * 
 * @author schuller
 */
public interface IDB {

	/**
	 * return the {@link ApplicationInfo} for the specified app
	 * 
	 * @param name - application name
	 * @param version - application version (can be <code>null</code>)
	 * @param client - the current client
	 * @return the {@link ApplicationInfo} stored in the IDB
	 */
	public ApplicationInfo getApplication(String name, String version, Client client);

	/**
	 * list all apps available to the current client
	 * 
	 * @param client
	 * @return (non-live) collection of {@link ApplicationInfo}
	 */
	public Collection<ApplicationInfo> getApplications(Client client);

	/**
	 * return the configured partitions
	 */
	public List<Partition> getPartitions() throws ExecutionException;

	/**
	 * gets the named partition
	 */
	public Partition getPartition(String partition) throws ExecutionException;

	/**
	 * gets the default partition (named "default", or the first defined one)
	 */
	public default Partition getDefaultPartition() throws ExecutionException {
		return getPartition(null);
	}

	/**
	 * get the header that is used for building shell scripts for
	 * execution by the (classic) TSI
	 * 
	 * @return the template (or the default one if not defined)
	 */
	public String getScriptHeader();

	/**
	 * Get a TextInfo property</br>
	 * 
	 * @param name - the name of the property
	 * @return The value of the requested property, or null if property does not exist
	 */
	public String getTextInfo(String name);

	/**
	 * Get all TextInfo properties in a map keyed by property name</br>
	 *
	 * @return A map containing all properties
	 */
	public Map<String,String> getTextInfoProperties();

	/**
	 * get the definition of the named filesystem. This can contain variables, 
	 * so needs to be resolved in the current context.
	 * @see Incarnation#incarnatePath(String, String, de.fzj.unicore.xnjs.ems.ExecutionContext, Client)
	 * @return the file system definition from the IDB
	 */
	public String getFilespace(String key);

	/**
	 * Get the names of the configured file systems
	 */
	public String[] getFilesystemNames();

	/**
	 * get the time when the underlying information was last updated
	 * 
	 * @return the last update time
	 */
	public long getLastUpdateTime();
	
	/**
	 * get the available partition names (queue names) for the current client
	 */
	public  Resource getAllowedPartitions(Client c) throws ExecutionException;

}