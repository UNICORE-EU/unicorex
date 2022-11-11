package de.fzj.unicore.uas.cdmi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpMessage;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.StatusLine;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.unicore.services.rest.client.BaseClient;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * CDMI style REST client. Helps with adding the correct headers etc.
 * All resource paths are relative to the endpoint, and are expected 
 * to start with "/"
 * 
 * @author schuller
 */
public class CDMIClient extends BaseClient {

	private static final Logger logger = Log.getLogger(Log.SERVICES, CDMIClient.class);

	/**
	 * value for the "X-CDMI-Specification-Version" header
	 */
	protected String SPEC_VERSION = "1.0.1";

	public static final ContentType CDMI_CONTAINER = ContentType.create("application/cdmi-container");

	public static final ContentType CDMI_OBJECT = ContentType.create("application/cdmi-object");

	/**
	 * endpoint / base URL of the CDMI storage
	 */
	private final String endpoint;

	public CDMIClient(String endpoint, IClientConfiguration security,
			IAuthCallback authCallback) {
		super(endpoint, security, authCallback);
		if(endpoint.endsWith("/"))endpoint=endpoint.substring(0, endpoint.length()-1);
		this.endpoint = endpoint;
	}

	public CDMIClient(String endpoint, IClientConfiguration security) {
		super(endpoint, security);
		this.endpoint = endpoint;
	}

	public String getEndpoint(){
		return endpoint;
	}

	protected void setVersion(HttpMessage message){
		message.setHeader("X-CDMI-Specification-Version", SPEC_VERSION);
	}

	public JSONObject getResourceInfo(String resource) throws Exception {
		setURL(endpoint+resource+"?metadata");
		return getJSON(CDMI_OBJECT);
	}

	public JSONObject getDirectoryInfo(String container) throws Exception {
		setURL(endpoint+container);
		return getJSON(CDMI_CONTAINER);
	}

	public List<String> listChildren(String container, int offset, int number) throws Exception {
		setURL(endpoint+container);
		try(ClassicHttpResponse res = get(CDMI_CONTAINER)){
			JSONObject j = asJSON(res);
			List<String>result = new ArrayList<>();
			JSONArray children = j.getJSONArray("children");
			for(int i=0; i<children.length(); i++){
				result.add(children.getString(i));
			}
			return result;
		}
	}

	public boolean directoryExists(String container) throws Exception {
		setURL(endpoint+container);
		try(ClassicHttpResponse res = get(CDMI_CONTAINER)){
			return 200 == res.getCode();
		}
	}

	public boolean resourceExists(String resource) throws Exception {
		setURL(endpoint+resource+"?metadata");
		try(ClassicHttpResponse res = get(CDMI_OBJECT)){
			return 200 == res.getCode();
		}
	}

	public void createDirectory(String dir) throws Exception {
		JSONObject content = new JSONObject();
		content.put("metadata", new JSONObject());
		HttpPut put = new HttpPut(endpoint+dir);
		put.setHeader("Accept", CDMI_CONTAINER.toString());
		put.setHeader("Content-Type", CDMI_CONTAINER.toString());
		put.setEntity(new StringEntity(content.toString()));
		try(ClassicHttpResponse res = execute(put)){
			int code = res.getCode();
			if(logger.isDebugEnabled()){
				logger.debug("Created directory <{}>: {}", dir, res.getCode());
			}
			if(code>299)throw new IOException("Could not create <"+dir+"> : "+new StatusLine(res));
		}
	}

	public void writeObject(String path, JSONObject object, boolean partial) throws Exception {
		writeObject(path, object, -1, -1, partial);
	}

	public void writeObject(String path, JSONObject object, long first, long last, boolean partial) throws Exception {
		String uri = endpoint+path;
		if(first>-1 && partial){
			uri += "?value:"+first+"-"+last;
		}
		HttpPut put = new HttpPut(uri);
		put.setHeader("Accept", CDMI_OBJECT.toString());
		put.setHeader("Content-Type", CDMI_OBJECT.toString());
		if(partial)put.setHeader("X-CDMI-Partial", "true");
		put.setEntity(new StringEntity(object.toString()));
		try(ClassicHttpResponse res = execute(put)){
			int code = res.getCode();
			logger.debug("Wrote to URI <"+uri+"> : "+res.getReasonPhrase());
			if(code>299)throw new IOException("Could not write <"+path+"> : "+new StatusLine(res));
		}
	}

	public int readObjectData(String resource, byte[] buf, long offset, long length) throws Exception {
		String uri = endpoint+resource;
		if(offset>-1){
			uri += "?valuetransferencoding;value:bytes="+offset+"-"+(length-1);
		}
		logger.debug("Reading {}", uri);
		HttpGet get = new HttpGet(uri);
		get.setHeader("Accept", CDMI_OBJECT.toString());
		get.setHeader("Content-Type", CDMI_OBJECT.toString());
		try(ClassicHttpResponse res = execute(get)){
			logger.debug("Read from to URI <{}> : {}", uri, res.getReasonPhrase());
			JSONObject j = asJSON(res);
			boolean isBase64 = "base64".equals(j.getString("valuetransferencoding"));
			String val = j.getString("value");
			byte[] decoded = isBase64 ? Base64.decode(val) : val.getBytes();
			int len = decoded.length;
			int toCopy = Math.min(decoded.length,(int)length);
			System.arraycopy(decoded, 0, buf, 0, toCopy);
			return len;
		}
	}

	protected ClassicHttpResponse execute(HttpUriRequestBase method) throws Exception {
		setVersion(method);
		return super.execute(method);
	}
}
