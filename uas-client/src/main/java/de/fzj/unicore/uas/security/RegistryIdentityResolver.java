/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package de.fzj.unicore.uas.security;

import java.io.IOException;

import org.oasisOpen.docs.wsrf.sg2.EntryType;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.wsrflite.xmlbeans.WSUtilities;
import de.fzj.unicore.wsrflite.xmlbeans.client.IRegistryQuery;

import eu.unicore.security.wsutil.client.authn.CachingIdentityResolver;

/**
 * Extension of the {@link CachingIdentityResolver} which also searches in configured registry 
 * for identities of container.
 * 
 * @author K. Benedyczak
 */
public class RegistryIdentityResolver extends CachingIdentityResolver
{
	private IRegistryQuery registry;
	
	public RegistryIdentityResolver(IRegistryQuery registry)
	{
		this.registry = registry;
	}

	@Override
	public synchronized String resolveIdentity(String serviceURL) throws IOException
	{
		try
		{
			return super.resolveIdentity(serviceURL);
		} catch (IOException e)
		{
			return searchInRegistry(serviceURL);
		}
	}

	private String searchInRegistry(String url) throws IOException
	{
		String container = getContainerAddress(url);
		try
		{
			for(EntryType entry: registry.listEntries()){
				EndpointReferenceType memberEpr = entry.getMemberServiceEPR();
				if(memberEpr.getAddress().getStringValue().startsWith(container)){
					String dn=WSUtilities.extractServerIDFromEPR(memberEpr);
					if (dn != null) 
					{
						cachedIdentities.put(container, dn);
						return dn;
					}
				}
			}
		} catch (Exception e)
		{
			throw new IOException("Problem when searching in registry for container DN", e);
		}
		throw new IOException("DN of the container not found in Registry");
	}
}
