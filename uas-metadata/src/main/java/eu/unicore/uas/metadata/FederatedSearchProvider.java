/** Copyright (c) 2013 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/


package eu.unicore.uas.metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.client.MetadataClient;
import de.fzj.unicore.uas.client.StorageClient;
import de.fzj.unicore.uas.lookup.AddressFilter;
import de.fzj.unicore.uas.lookup.StorageLister;
import de.fzj.unicore.uas.metadata.FederatedSearchResult;
import de.fzj.unicore.uas.metadata.FederatedSearchResultCollection;
import de.fzj.unicore.uas.security.ClientConfigProvider;
import de.fzj.unicore.uas.security.WSRFClientConfigurationProviderImpl;
import eu.unicore.security.Client;
import eu.unicore.services.ContainerProperties;
import eu.unicore.services.Kernel;
import eu.unicore.services.registry.RegistryHandler;
import eu.unicore.services.ws.client.IRegistryQuery;
import eu.unicore.services.ws.client.RegistryClient;
import eu.unicore.services.ws.sg.Registry;
import eu.unicore.util.httpclient.IClientConfiguration;


/**
 * Federated metada search provider<br/>
 * 
 * @author Konstantine Muradov
 * @author schuller
 */
public class FederatedSearchProvider implements
		Callable<FederatedSearchResultCollection> {

	private final Kernel kernel;
	private final Client client;
	private final List<Pattern> acceptedStorageURLPatterns;
	private final String keyWord;
	private final boolean isAdvanced;

	public FederatedSearchProvider(Kernel kernel, Client client, String keyWord, 
			List<String> patterns, boolean isAdvanced) {
		this.kernel = kernel;
		this.client = client;
		this.keyWord = keyWord;
		this.acceptedStorageURLPatterns = createPatterns(patterns);
		this.isAdvanced = isAdvanced;
	}
	
	private List<Pattern> createPatterns(List<String> patterns){
		List<Pattern> res=new ArrayList<Pattern>();
		if(patterns!=null){
			for(String p: patterns){
				res.add(Pattern.compile(p));
			}
		}
		return res;
	}

	@Override 
	public FederatedSearchResultCollection call() throws Exception {

		FederatedSearchResultCollection result = new FederatedSearchResultCollection();

		IRegistryQuery registryClient = getRegistryClient();
		IClientConfiguration baseConfig = kernel.getClientConfiguration();
    	WSRFClientConfigurationProviderImpl clientConfigProvider=new ClientConfigProvider(baseConfig, client);
    	clientConfigProvider.configureRegistryBasedIdentityResolver(getRegistryClient());
		StorageLister list = new StorageLister(registryClient, clientConfigProvider);
		list.setAddressFilter(new AddressFilter<StorageClient>() {
			
			@Override
			public boolean accept(String uri) {
				return acceptURL(uri);
			}
			
			@Override
			public boolean accept(EndpointReferenceType epr) {
				return epr!=null && epr.getAddress()!=null && accept(epr.getAddress().getStringValue());
			}

			@Override
			public boolean accept(StorageClient client) throws Exception {
				return true;
			}
			
		});
		
		for(StorageClient client : list) {
			FederatedSearchResult federatedSearchResult = new FederatedSearchResult();
			
			MetadataClient metadataClient = client.getMetadataClient();
			
			String storageURL = client.getUrl();
			Collection<String> searchResult = metadataClient.search(keyWord, isAdvanced);

			federatedSearchResult.setStorageURL(storageURL);
			federatedSearchResult.addResourceNames(new ArrayList<String>(searchResult));
			
			result.addSearchResult(federatedSearchResult);
		}
		
		result.setSearchEndTime(new Date());
		
		return result;
	}
	
	private IRegistryQuery getRegistryClient() throws Exception {
		RegistryHandler rh=kernel.getAttribute(RegistryHandler.class);
		IRegistryQuery registryClient=rh.getExternalRegistryClient();
		if(registryClient==null){
			// setup a client talking to the local registry
			String url = kernel.getContainerProperties().getValue(ContainerProperties.EXTERNAL_URL);
			EndpointReferenceType endpointReferenceType = EndpointReferenceType.Factory.newInstance();
			endpointReferenceType.addNewAddress().setStringValue(url + "/services/" + Registry.REGISTRY_SERVICE + "?res=default_registry");
			registryClient = new RegistryClient(endpointReferenceType, kernel.getClientConfiguration());
		}
		return registryClient;
	}

	
	 // check if endpoint address is accepted (preferred storages list)
	private boolean acceptURL(String url){
		if(acceptedStorageURLPatterns==null || acceptedStorageURLPatterns.size()==0){
			return true;
		}
		
		for(Pattern pattern: acceptedStorageURLPatterns){
			if(pattern.matcher(url).matches())return true;
		}
		
		return false;
	}
	
	
}
