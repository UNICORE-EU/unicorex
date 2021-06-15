package de.fzj.unicore.uas.security;

import java.io.IOException;

import javax.security.auth.x500.X500Principal;

import eu.unicore.security.Client;
import eu.unicore.security.wsutil.client.authn.DelegationSpecification;
import eu.unicore.services.security.ETDAssertionForwarding;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * Config provider based on the container's base configuration and a UNICORE Client 
 * object. Can probably be moved to USE.
 *
 * @author schuller
 */
public class ClientConfigProvider extends WSRFClientConfigurationProviderImpl {

	private final Client client;
	
	public ClientConfigProvider(IClientConfiguration baseConfig, Client client){
		super();
		this.client=client;
		setBasicConfiguration(baseConfig);
	}
	
	@Override
	public IClientConfiguration getClientConfiguration(String serviceUrl, String serviceIdentity, 
			DelegationSpecification delegate) throws Exception
	{
		if (serviceUrl == null)
			throw new IllegalArgumentException("Service URL must be always given");
	
		if (serviceUrl.startsWith("http://")){
			// probably tests, do not configure anything
			return getBasicClientConfiguration();
		}
		
		if (serviceIdentity == null)
		{
			try
			{
				serviceIdentity = getIdentityResolver().resolveIdentity(serviceUrl);
			} catch (IOException e)
			{
				if (delegate.isDelegate())
					throw e;
			}
		} 
		else
		{
			getIdentityResolver().registerIdentity(serviceUrl, serviceIdentity);
		}
		
		IClientConfiguration securityProperties = getBasicClientConfiguration().clone();
		if(delegate.isDelegate()){
			ETDAssertionForwarding.configureETDChainExtension(client, securityProperties, 
					new X500Principal(serviceIdentity));
		}
		else{
			ETDAssertionForwarding.configureETD(client, securityProperties);
		}
		//make sure we use the same session id provider everywhere
		return securityProperties;
	}
}
