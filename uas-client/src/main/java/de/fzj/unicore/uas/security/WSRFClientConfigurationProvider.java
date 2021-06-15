/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package de.fzj.unicore.uas.security;

import org.w3.x2005.x08.addressing.EndpointReferenceType;

import eu.unicore.security.wsutil.client.authn.ClientConfigurationProvider;
import eu.unicore.security.wsutil.client.authn.DelegationSpecification;
import eu.unicore.security.wsutil.client.authn.ServiceIdentityResolver;
import eu.unicore.services.ws.client.IRegistryQuery;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * Extension of {@link ClientConfigurationProvider}, adding support for EPRs. 
 * For delegation, EPRs may or may not contain the DN of the server, so this interface
 * adds server identity resolution via the Registry.<
 *    
 * @author K. Benedyczak
 */
public interface WSRFClientConfigurationProvider extends ClientConfigurationProvider
{
	public void configureRegistryBasedIdentityResolver(IRegistryQuery registry);
	
	public IClientConfiguration getClientConfiguration(EndpointReferenceType epr, DelegationSpecification delegate) 
			throws Exception;

	public ServiceIdentityResolver getIdentityResolver();
}
