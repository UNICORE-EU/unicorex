/*********************************************************************************
 * Copyright (c) 2016 Forschungszentrum Juelich GmbH 
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

package de.fzj.unicore.xnjs.tsi.remote;

import java.util.Map;

import de.fzj.unicore.xnjs.tsi.TSIUnavailableException;
import eu.unicore.security.Client;

/**
 * Creates connections to a UNICORE TSI server
 * 
 * @author schuller
 */
public interface TSIConnectionFactory {

	/**
	 * return a connection that executes commands under the given user id.
	 * If a timeout is given (larger than zero), the factory waits for a connection to
	 * become available before attempting to create a new one.
	 * 
	 * @param user - user name (may never be null)
	 * @param group - group (may be null)
	 * @param preferredHost - the preferred TSI host (in case multiple hosts are available)
	 * @param timeoutMillis - timeout for waiting for a connection to become available.
	 * 
	 * @return a valid connection object or null in case of errors
	 * @throws TSIUnavailableException if TSI is down
	 * @throws IllegalArgumentException - if user is <code>null</code>
	 */
	public TSIConnection getTSIConnection(String user, String group, String preferredHost, int timeoutMillis)throws TSIUnavailableException;

	/**
	 * Return a connection that executes commands under the given user id. 
	 * If a timeout is given (larger than zero), the factory waits for a connection to
	 * become available before attempting to create a new one.
	 * 
	 * @param client - the {@link Client} for which to create the connection
	 * @param preferredHost - the preferred TSI host (in case multiple hosts are available)
	 * @return a valid connection object or null in case of errors
	 */
	public TSIConnection getTSIConnection(Client client, String preferredHost, int timeoutMillis) throws TSIUnavailableException;

	/**
	 * notify that a connection was removed / died
	 */
	public void notifyConnectionDied();

	/**
	 * notify that the use of the connection is finished
	 *
	 * @param connection
	 */
	public void done(TSIConnection connection);
	
	/**
	 * get the version of the TSI server we are connected to
	 */
	public String getTSIVersion();

	/**
	 * TSI machine as given in config file
	 */
	public String getTSIMachine();

	/**
	 * TSI host names
	 */
	public String[] getTSIHosts();

	/**
	 * Connection status overview message
	 */
	public String getConnectionStatus();

	/**
	 * get status messages for the individual TSIs we are connecting to
	 */
	public Map<String,String>getTSIConnectorStates();

}
