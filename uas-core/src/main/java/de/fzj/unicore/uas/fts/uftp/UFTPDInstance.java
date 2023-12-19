package de.fzj.unicore.uas.fts.uftp;

import java.security.SecureRandom;

import javax.net.ssl.SSLSocketFactory;

import eu.emi.security.authn.x509.impl.SocketFactoryCreator2;
import eu.unicore.services.ExternalSystemConnector;
import eu.unicore.services.Kernel;
import eu.unicore.uftp.server.UFTPDInstanceBase;
import eu.unicore.util.httpclient.HostnameMismatchCallbackImpl;
import eu.unicore.util.httpclient.IClientConfiguration;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;

/**
 * Holds properties and parameters for a single UFTPD server,
 * and is used for communicating with the UFTPD.
 *
 * @author schuller
 */
public class UFTPDInstance extends UFTPDInstanceBase
implements ExternalSystemConnector {

	private final Kernel kernel;

	public UFTPDInstance(Kernel kernel){
		super();
		this.kernel = kernel;
	}

	public void configure(UFTPProperties properties) {
		setCommandHost(properties.getValue(UFTPProperties.PARAM_COMMAND_HOST));
		setCommandPort(properties.getIntValue(UFTPProperties.PARAM_COMMAND_PORT));
		setSsl(!properties.getBooleanValue(UFTPProperties.PARAM_COMMAND_SSL_DISABLE));
		setHost(properties.getValue(UFTPProperties.PARAM_SERVER_HOST));
		setPort(properties.getIntValue(UFTPProperties.PARAM_SERVER_PORT));
	}
	
	public String getConnectionStatusMessage(){
		checkConnection();
		return statusMessage;
	}
	
	public Status getConnectionStatus() {
		checkConnection();
		return isUp? Status.OK : Status.DOWN;
	}
	
	public String getExternalSystemName() {
		return  "UFTPD "+getHost()+":"+getPort();
	}

	private SSLSocketFactory socketfactory = null;
	
	public synchronized SSLSocketFactory getSSLSocketFactory() {
		if(socketfactory==null) {
			IClientConfiguration cfg = kernel.getClientConfiguration();
			socketfactory = new SocketFactoryCreator2(cfg.getCredential(), cfg.getValidator(), 
					new HostnameMismatchCallbackImpl(ServerHostnameCheckingMode.NONE),
					getRandom(), "TLS").getSocketFactory();
		}
		return socketfactory;
	}

	private static SecureRandom random=null;

	private synchronized SecureRandom getRandom(){
		if(random==null){
			random=new SecureRandom();
		}
		return random;
	}
}
