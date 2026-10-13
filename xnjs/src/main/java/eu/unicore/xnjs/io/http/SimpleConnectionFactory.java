package eu.unicore.xnjs.io.http;

import org.apache.hc.client5.http.classic.HttpClient;

import eu.unicore.security.Client;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.HttpUtils;
import io.imunity.tanl.x509.helpers.BinaryCertChainValidator;

/**
 * Creates HTTP(s) connections for (plain http(s), non-UNICORE) data staging
 * (does not validate server certificates)
 *
 * @author schuller
 */
public class SimpleConnectionFactory implements IConnectionFactory {

	public HttpClient getConnection(String url, Client client) {
		return HttpUtils.createClient(
				url,
				new DefaultClientConfiguration(new BinaryCertChainValidator(true), null));
	}

}
