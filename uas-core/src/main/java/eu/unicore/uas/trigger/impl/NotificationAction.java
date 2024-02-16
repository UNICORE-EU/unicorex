package eu.unicore.uas.trigger.impl;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.unicore.security.Client;
import eu.unicore.services.Kernel;
import eu.unicore.uas.trigger.MultiFileAction;
import eu.unicore.uas.util.LogUtil;
import eu.unicore.uas.xnjs.NotificationSender;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.io.IStorageAdapter;

/**
 * sends notification to a predefined URL with the list of new files
 * 
 * @author schuller
 */
public class NotificationAction extends BaseAction implements MultiFileAction {

	private static final Logger logger = LogUtil.getLogger(LogUtil.TRIGGER, NotificationAction.class);

	private final String url;
	
	public NotificationAction(String url){
		this.url = url;
	}
		
	private List<String>files;

	@Override
	public void setTarget(List<String>target) {
		this.files = target;
	}

	@Override
	public String run(IStorageAdapter storage, Client client, XNJS xnjs) throws Exception{
		logger.info("Sending notification for <{}> to <{}>", url, client.getDistinguishedName());
		JSONArray fList = new JSONArray();
		for(String f: files)fList.put(f);
		JSONObject msg = new JSONObject();
		Kernel kernel = xnjs.get(Kernel.class);
		msg.put("href", kernel.getContainerProperties().getContainerURL()+"/rest/core");
		msg.put("directory", storage.getStorageRoot());
		msg.put("files", fList);
		NotificationSender.doSend(kernel, url, msg, client.getDistinguishedName());
		return null;
	}

	public String toString(){
		return "NOTIFY";
	}
}
