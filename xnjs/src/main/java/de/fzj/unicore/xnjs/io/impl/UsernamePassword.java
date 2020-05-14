package de.fzj.unicore.xnjs.io.impl;

import de.fzj.unicore.xnjs.io.DataStagingCredentials;
import de.fzj.unicore.xnjs.util.IOUtils;
import eu.unicore.security.Client;

/**
 * holds username and password for some types of data staging, 
 * as found in the job description
 * 
 * @author schuller
 */
public class UsernamePassword implements DataStagingCredentials {

	private static final long serialVersionUID = 1l;
	
	private final String user;
	
	private final String password;
	
	public UsernamePassword(String user, String password){
		if(user==null){
			throw new IllegalArgumentException("User name cannot be null.");
		}
		this.user=user;
		this.password=password;
	}

	public String getUser() {
		return user;
	}

	public String getPassword() {
		return password;
	}

	public String toString(){
		return "UsernamePassword["+user+" "+(password!=null?password:"(no password)")+"]";
	}
	
	@Override
	public String getHTTPAuthorizationHeader(Client client){
		return IOUtils.getBasicAuth(user, password);
	}
}
