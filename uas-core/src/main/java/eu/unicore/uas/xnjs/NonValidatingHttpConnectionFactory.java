package eu.unicore.uas.xnjs;

import org.apache.hc.client5.http.classic.HttpClient;

import eu.unicore.security.Client;
import eu.unicore.services.Kernel;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.HttpUtils;
import eu.unicore.xnjs.io.http.IConnectionFactory;
import io.imunity.tanl.x509.helpers.BinaryCertChainValidator;

/**
 * Creates HTTP(s) connections for (non-UNICORE) data staging
 * that does not validate server certificates
 *
 * @author schuller
 */
public class NonValidatingHttpConnectionFactory implements IConnectionFactory{

	public NonValidatingHttpConnectionFactory(Kernel kernel){}

	@Override
	public HttpClient getConnection(String url, Client client) {
		DefaultClientConfiguration cc =
			new DefaultClientConfiguration(new BinaryCertChainValidator(true), null);
		return HttpUtils.createClient(url, cc);
	}

}