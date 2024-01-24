package eu.unicore.uas.xnjs;

import org.apache.hc.client5.http.classic.HttpClient;

import eu.emi.security.authn.x509.helpers.BinaryCertChainValidator;
import eu.unicore.security.Client;
import eu.unicore.services.Kernel;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.HttpUtils;
import eu.unicore.xnjs.io.http.IConnectionFactory;

/**
 * creates HTTP(s) connections for plain http(s) data staging
 * trusts every server
 *
 * @author schuller
 */
public class U6HttpConnectionFactory implements IConnectionFactory{

	public U6HttpConnectionFactory(Kernel kernel){}
	
	@Override
	public HttpClient getConnection(String url, Client client) {
		DefaultClientConfiguration cc =
			new DefaultClientConfiguration(new BinaryCertChainValidator(true), null);
		return HttpUtils.createClient(url, cc);
	}
}
