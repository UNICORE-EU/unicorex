package de.fzj.unicore.uas.xnjs;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.json.JSONObject;

import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.event.INotificationSender;
import eu.unicore.security.Client;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.client.BaseClient;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.services.rest.jwt.JWTDelegation;
import eu.unicore.services.rest.jwt.JWTServerProperties;
import eu.unicore.services.utils.TimeoutRunner;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;

@Singleton
public class NotificationSender implements INotificationSender {

	private final Kernel kernel;
	
	@Inject
	public NotificationSender(Kernel kernel) {
		this.kernel = kernel;
	}
	
	@Override
	public void send(JSONObject msg, Action action) {
		if(action==null || action.getNotificationURLs()==null || action.getNotificationURLs().isEmpty())return;
		msg.put("href", kernel.getContainerProperties().getContainerURL()+"/rest/core/jobs/"+action.getUUID());
		List<String>urls = action.getNotificationURLs();
		for(String url: urls) {
			try{
				doSend(url, msg, action.getClient(), action);
				action.addLogTrace("Notified <"+url+">");
			}catch(Exception ex) {
				action.addLogTrace(Log.createFaultMessage("Could not notify <"+url+">", ex));
			}
		}
	}

	protected void doSend(String url, JSONObject msg, Client client, Action action) throws Exception {
		IClientConfiguration security = kernel.getClientConfiguration();
		String user = client.getDistinguishedName();
		IAuthCallback auth = new JWTDelegation(kernel.getContainerSecurityConfiguration(), 
				new JWTServerProperties(kernel.getContainerProperties().getRawProperties()), user);
		final BaseClient bc = new BaseClient(url, security, auth);
		final JSONObject message = new JSONObject();
		Callable<String>task = new Callable<String>() {
			@Override
			public String call() throws Exception {
				bc.postQuietly(message);
				return "OK";
			}
		};
		String res = new TimeoutRunner<String>(task, kernel.getContainerProperties().getThreadingServices(), 30, TimeUnit.SECONDS).call();
		if(res==null)throw new TimeoutException();
	}
	
}
