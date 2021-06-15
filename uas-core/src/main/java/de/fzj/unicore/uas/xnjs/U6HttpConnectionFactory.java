package de.fzj.unicore.uas.xnjs;

import org.apache.http.client.HttpClient;

import de.fzj.unicore.xnjs.io.http.IConnectionFactory;
import eu.unicore.security.Client;
import eu.unicore.services.Kernel;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.HttpUtils;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * creates HTTP(s) connections for plain http(s) data staging
 * using the UNICORE/X server's key/truststore
 * 
 * @author schuller
 */
public class U6HttpConnectionFactory implements IConnectionFactory{

	private final Kernel kernel;
	
	public U6HttpConnectionFactory(Kernel kernel){
		this.kernel = kernel;
	}
	
	@Override
	public HttpClient getConnection(String url, Client client) {
		IClientConfiguration auth=kernel.getClientConfiguration();
		try {
			DefaultClientConfiguration cc = (DefaultClientConfiguration)auth;
			cc.setSslAuthn(false);
			cc.setSslEnabled(false);
		} catch(Exception ex) {}
		return HttpUtils.createClient(url, auth);
	}
}
