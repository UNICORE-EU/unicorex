package de.fzj.unicore.uas.jclouds.s3;

import de.fzj.unicore.uas.impl.sms.SMSModel;

public class S3Model extends SMSModel {

	private static final long serialVersionUID = 1L;

	private String endpoint;
	private String provider;
	private String accessKey;
	private String secretKey;
	private String region;
	
	public String getEndpoint() {
		return endpoint;
	}
	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}
	public String getProvider() {
		return provider;
	}
	public void setProvider(String provider) {
		this.provider = provider;
	}
	public String getAccessKey() {
		return accessKey;
	}
	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}
	public String getSecretKey() {
		return secretKey;
	}
	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}
	public String getRegion() {
		return region;
	}
	public void setRegion(String region) {
		this.region = region;
	}
}
