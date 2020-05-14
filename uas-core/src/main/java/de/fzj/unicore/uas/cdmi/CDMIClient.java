package de.fzj.unicore.uas.cdmi;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.log4j.Logger;
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
		HttpResponse res = get(CDMI_OBJECT);
		try{
			checkNotFound(res, resource);
			checkError(res);
			return asJSON(res);
		}finally{
			if(res instanceof CloseableHttpResponse){
				((CloseableHttpResponse)res).close();
			}
		}
	}

	public JSONObject getDirectoryInfo(String container) throws Exception {
		setURL(endpoint+container);
		HttpResponse res = get(CDMI_CONTAINER);
		checkNotFound(res, container);
		checkError(res);
		return asJSON(res);
	}

	public List<String> listChildren(String container, int offset, int number) throws Exception {
		setURL(endpoint+container);
		HttpResponse res = get(CDMI_CONTAINER);
		checkNotFound(res, container);
		checkError(res);
		JSONObject j = asJSON(res);
		List<String>result = new ArrayList<String>();
		JSONArray children = j.getJSONArray("children");
		for(int i=0; i<children.length(); i++){
			result.add(children.getString(i));
		}
		return result;
	}

	public boolean directoryExists(String container) throws Exception {
		setURL(endpoint+container);
		HttpResponse res = get(CDMI_CONTAINER);
		close(res);
		return 200 == res.getStatusLine().getStatusCode();
	}

	public boolean resourceExists(String resource) throws Exception {
		setURL(endpoint+resource+"?metadata");
		HttpResponse res = get(CDMI_OBJECT);
		return 200 == res.getStatusLine().getStatusCode();
	}

	public void createDirectory(String dir) throws Exception {
		JSONObject content = new JSONObject();
		content.put("metadata", new JSONObject());
		HttpPut put = new HttpPut(endpoint+dir);
		try{
			put.setHeader("Accept", CDMI_CONTAINER.toString());
			put.setHeader("Content-Type", CDMI_CONTAINER.toString());
			put.setEntity(new StringEntity(content.toString()));
			HttpResponse res = execute(put);
			int code = res.getStatusLine().getStatusCode();
			if(logger.isDebugEnabled()){
				logger.debug("Created directory <"+dir+"> : "+res.getStatusLine());
			}
			if(code>299)throw new IOException("Could not create <"+dir+"> : "+res.getStatusLine().toString());
		}finally{
			put.reset();
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
		try{
			put.setHeader("Accept", CDMI_OBJECT.toString());
			put.setHeader("Content-Type", CDMI_OBJECT.toString());
			if(partial)put.setHeader("X-CDMI-Partial", "true");
			put.setEntity(new StringEntity(object.toString()));
			HttpResponse res = execute(put);
			int code = res.getStatusLine().getStatusCode();
			if(logger.isDebugEnabled()){
				logger.debug("Wrote to URI <"+uri+"> : "+res.getStatusLine());
			}
			if(code>299)throw new IOException(res.getStatusLine().toString());
		}finally{
			put.reset();
		}
	}

	public int readObjectData(String resource, byte[] buf, long offset, long length) throws Exception {
		String uri = endpoint+resource;
		if(offset>-1){
			uri += "?valuetransferencoding;value:bytes="+offset+"-"+(length-1);
		}
		if(logger.isDebugEnabled())logger.debug("Reading "+uri);
		HttpGet get = new HttpGet(uri);
		try{
			get.setHeader("Accept", CDMI_OBJECT.toString());
			get.setHeader("Content-Type", CDMI_OBJECT.toString());
			HttpResponse res = execute(get);
			checkError(res);
			if(logger.isDebugEnabled()){
				logger.debug("Read from to URI <"+uri+"> : "+res.getStatusLine());
			}
			JSONObject j = asJSON(res);
			boolean isBase64 = "base64".equals(j.getString("valuetransferencoding"));
			String val = j.getString("value");
			byte[] decoded = isBase64 ? Base64.decode(val) : val.getBytes();
			int len = decoded.length;
			int toCopy = Math.min(decoded.length,(int)length);
			System.arraycopy(decoded, 0, buf, 0, toCopy);
			return len;
		}finally{
			get.reset();
		}
	}
	
	protected void checkNotFound(HttpResponse response, String resource) throws Exception {
		if(response.getStatusLine().getStatusCode()==404){
			close(response);			
			throw new FileNotFoundException("Not found: "+resource);
		}
	}

	protected HttpResponse execute(HttpRequestBase method) throws Exception {
		setVersion(method);
		return super.execute(method);
	}
}
