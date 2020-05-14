package de.fzj.unicore.uas.xnjs;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.http.HttpResponse;
import org.json.JSONObject;

import de.fzj.unicore.uas.impl.job.StatusInfoResourceProperty;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.utils.TimeoutRunner;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ActionResult;
import de.fzj.unicore.xnjs.ems.ActionStateChangeListener;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import eu.unicore.security.Client;
import eu.unicore.services.rest.client.BaseClient;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.services.rest.jwt.JWTDelegation;
import eu.unicore.services.rest.jwt.JWTServerProperties;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;

@Singleton
public class RESTNotificationSender implements ActionStateChangeListener {

	final int[] defaultTriggers = new int[] {ActionStatus.RUNNING, ActionStatus.DONE};
	
	private final Kernel kernel;
	
	@Inject
	public RESTNotificationSender(Kernel kernel) {
		this.kernel = kernel;
	}
	
	protected boolean isTrigger(int actionState, int[]triggers) {
		for(int i: triggers) {
			if(i==actionState)return true;
		}
		return false;
	}
	
	@Override
	public void stateChanged(Action action) {
		if(action==null || action.getNotificationURLs()==null || action.getNotificationURLs().isEmpty())return;
		
		int status = action.getStatus();
		if(!isTrigger(status, defaultTriggers))return;
		
		List<String>urls = action.getNotificationURLs();
		
		for(String url: urls) {
			try{
				send(url, status, action.getClient(), action);
				action.addLogTrace("Notified <"+url+">");
			}catch(Exception ex) {
				String msg = Log.createFaultMessage("Could not notify <"+url+">", ex);
				action.addLogTrace(msg);
			}
		}
	}

	protected void send(String url, int newStatus, Client client, Action action) throws Exception {
		IClientConfiguration security = kernel.getClientConfiguration().clone();
		String user = client.getDistinguishedName();
		IAuthCallback auth = new JWTDelegation(kernel.getContainerSecurityConfiguration(), 
				new JWTServerProperties(kernel.getContainerProperties().getRawProperties()), user);
		final BaseClient bc = new BaseClient(url, security, auth);
		final JSONObject message = new JSONObject();
		message.put("href", kernel.getContainerProperties().getContainerURL()+"/rest/core/jobs/"+action.getUUID());
		ActionResult result = action.getResult();
		message.put("status", String.valueOf(StatusInfoResourceProperty.convertStatus(action.getStatus(),result.isSuccessful())));
		message.put("statusMessage", "");
		Integer exitCode = action.getExecutionContext().getExitCode();
		if(exitCode!=null){
			message.put("exitCode", String.valueOf(exitCode.longValue()));
		}
		if(!result.isSuccessful()){
			String errorMessage = result.getErrorMessage();
			if(errorMessage==null)errorMessage="";
			message.put("statusMessage", errorMessage);
		}
		
		Callable<String>task = new Callable<String>() {
			@Override
			public String call() throws Exception {
				HttpResponse res = bc.post(message);
				bc.checkError(res);
				return "OK";
			}
		};
		String res = new TimeoutRunner<String>(task, kernel.getContainerProperties().getThreadingServices(), 30, TimeUnit.SECONDS).call();
		if(res==null)throw new TimeoutException();
	}
	
}
