package eu.unicore.xnjs.io.impl;

import eu.unicore.security.Client;
import eu.unicore.xnjs.io.DataStagingCredentials;
import eu.unicore.xnjs.util.IOUtils;

/**
 * holds username and password for generating
 * an HTTP "Authorization: Basic ..." header
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
