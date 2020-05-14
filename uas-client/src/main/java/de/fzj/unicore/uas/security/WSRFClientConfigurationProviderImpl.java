/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package de.fzj.unicore.uas.security;

import java.util.Map;

import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.wsrflite.xmlbeans.WSUtilities;
import de.fzj.unicore.wsrflite.xmlbeans.client.IRegistryQuery;
import eu.unicore.security.wsutil.client.authn.AuthenticationProvider;
import eu.unicore.security.wsutil.client.authn.CachingIdentityResolver;
import eu.unicore.security.wsutil.client.authn.ClientConfigurationProviderImpl;
import eu.unicore.security.wsutil.client.authn.DelegationSpecification;
import eu.unicore.security.wsutil.client.authn.SecuritySessionPersistence;
import eu.unicore.security.wsutil.client.authn.ServiceIdentityResolver;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * By default this implementation uses a {@link CachingIdentityResolver} however it is advised to configure
 * usage {@link RegistryIdentityResolver}.
 *    
 * @author K. Benedyczak
 */
public class WSRFClientConfigurationProviderImpl extends ClientConfigurationProviderImpl 
		implements WSRFClientConfigurationProvider
{
	
	public WSRFClientConfigurationProviderImpl() {
		super();
	}

	public WSRFClientConfigurationProviderImpl(AuthenticationProvider authnProvider,
			SecuritySessionPersistence sessionsPersistence,
			ServiceIdentityResolver identityResolver,
			Map<String, String[]> securityPreferences) throws Exception {
		super(authnProvider, sessionsPersistence, identityResolver, securityPreferences);
	}

	@Override
	public void configureRegistryBasedIdentityResolver(IRegistryQuery registry)
	{
		setIdentityResolver(new RegistryIdentityResolver(registry));
	}
	
	@Override
	public IClientConfiguration getClientConfiguration(EndpointReferenceType epr, DelegationSpecification delegate) 
			throws Exception
	{
		String serviceIdentity = WSUtilities.extractServerIDFromEPR(epr);
		String serviceUrl = epr.getAddress().getStringValue();
		return getClientConfiguration(serviceUrl, serviceIdentity, delegate);
	}
		
}
