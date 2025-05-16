package eu.unicore.uas.jclouds.swift;

import eu.unicore.uas.impl.sms.SMSModel;

public class SwiftModel extends SMSModel {

	private static final long serialVersionUID = 1L;

	private String endpoint;
	private String container;
	private String region;
	private String username;
	private String password;
	
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
	public String getRegion() {
		return region;
	}
	public void setRegion(String region) {
		this.region = region;
	}
	public String getContainer() {
		return container;
	}
	public void setContainer(String container) {
		this.container = container;
	}
}
