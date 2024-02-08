package eu.unicore.uas.xnjs;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.json.JSONObject;

import eu.unicore.security.Client;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.client.BaseClient;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.services.rest.jwt.JWTDelegation;
import eu.unicore.services.rest.jwt.JWTServerProperties;
import eu.unicore.services.utils.TimeoutRunner;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.event.INotificationSender;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

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
				doSend(url, msg, action.getClient());
				action.addLogTrace("Notified <"+url+">");
			}catch(Exception ex) {
				action.addLogTrace(Log.createFaultMessage("Could not notify <"+url+">", ex));
			}
		}
	}

	protected void doSend(final String url, final JSONObject message, final Client client)
			throws Exception {
		final IClientConfiguration security = kernel.getClientConfiguration();
		final IAuthCallback auth = new JWTDelegation(kernel.getContainerSecurityConfiguration(),
				new JWTServerProperties(kernel.getContainerProperties().getRawProperties()),
					client.getDistinguishedName());
		String res = new TimeoutRunner<String>( ()->{
						new BaseClient(url, security, auth).postQuietly(message);
						return "OK";
					},
				kernel.getContainerProperties().getThreadingServices(), 10, TimeUnit.SECONDS).call();
		if(res==null)throw new TimeoutException();
	}
	
}
