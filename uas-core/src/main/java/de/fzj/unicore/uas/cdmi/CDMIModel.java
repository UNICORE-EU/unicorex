package de.fzj.unicore.uas.cdmi;

import de.fzj.unicore.uas.impl.sms.SMSModel;

public class CDMIModel extends SMSModel {

	private static final long serialVersionUID = 1L;

	private String endpoint;
	private String username;
	private String password;
	
	// where to get a keystone token
	private String tokenEndpoint;
	
	public String getEndpoint() {
		return endpoint;
	}
	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getTokenEndpoint() {
		return tokenEndpoint;
	}
	public void setTokenEndpoint(String tokenEndpoint) {
		this.tokenEndpoint = tokenEndpoint;
	}
}
