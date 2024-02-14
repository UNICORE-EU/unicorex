package eu.unicore.uas.trigger.impl;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.unicore.security.Client;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.client.BaseClient;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.services.rest.jwt.JWTDelegation;
import eu.unicore.services.rest.jwt.JWTServerProperties;
import eu.unicore.services.utils.TimeoutRunner;
import eu.unicore.uas.trigger.MultiFileTriggeredAction;
import eu.unicore.uas.util.LogUtil;
import eu.unicore.util.httpclient.IClientConfiguration;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.io.IStorageAdapter;

/**
 * sends notification to a predefined URL with the list of new files
 * 
 * @author schuller
 */
public class NotificationAction extends BaseAction implements MultiFileTriggeredAction {

	private static final Logger logger = LogUtil.getLogger(LogUtil.TRIGGER, NotificationAction.class);

	private final String url;
	
	public NotificationAction(String url){
		this.url = url;
	}
	
	@Override
	public String run(IStorageAdapter storage, String filePath, Client client, XNJS xnjs) throws Exception{
		return fire(storage,Collections.singletonList(filePath), client, xnjs);
	}

	@Override
	public String fire(IStorageAdapter storage, List<String> files, Client client, XNJS xnjs) throws Exception{
		logger.info("Sending notification for <{}> to <{}>", url, client.getDistinguishedName());
		JSONArray fList = new JSONArray();
		for(String f: files)fList.put(f);
		JSONObject msg = new JSONObject();
		Kernel kernel = xnjs.get(Kernel.class);
		msg.put("href", kernel.getContainerProperties().getContainerURL()+"/rest/core");
		msg.put("directory", storage.getStorageRoot());
		msg.put("files", fList);
		doSend(kernel, url, msg, client);
		return null;
	}

	protected void doSend(Kernel kernel, String url, JSONObject message, Client client)
			throws Exception {
		final IClientConfiguration security = kernel.getClientConfiguration();
		final IAuthCallback auth = new JWTDelegation(kernel.getContainerSecurityConfiguration(),
				new JWTServerProperties(kernel.getContainerProperties().getRawProperties()),
					client.getDistinguishedName());
		String res = new TimeoutRunner<String>( ()-> {
						new BaseClient(url, security, auth).postQuietly(message);
						return "OK";
					},
					kernel.getContainerProperties().getThreadingServices(), 10, TimeUnit.SECONDS).call();
		if(res==null)throw new TimeoutException();
	}

	public String toString(){
		return "NOTIFY";
	}
}
