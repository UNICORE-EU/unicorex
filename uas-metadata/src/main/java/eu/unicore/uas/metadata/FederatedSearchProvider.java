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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import de.fzj.unicore.uas.metadata.FederatedSearchResult;
import de.fzj.unicore.uas.metadata.FederatedSearchResultCollection;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.EnumerationClient;
import eu.unicore.client.core.SiteClient;
import eu.unicore.client.core.StorageClient;
import eu.unicore.security.Client;
import eu.unicore.services.Kernel;
import eu.unicore.services.registry.IRegistry;
import eu.unicore.services.registry.RegistryHandler;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.services.rest.client.RegistryClient;
import eu.unicore.services.rest.jwt.JWTDelegation;
import eu.unicore.services.rest.jwt.JWTServerProperties;


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

	public FederatedSearchProvider(Kernel kernel, Client client, String keyWord, List<String> patterns) {
		this.kernel = kernel;
		this.client = client;
		this.keyWord = keyWord;
		this.acceptedStorageURLPatterns = createPatterns(patterns);
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
		IRegistry registryClient = getRegistryClient();
		IAuthCallback jwt = new JWTDelegation(kernel.getContainerSecurityConfiguration(), 
				new JWTServerProperties(kernel.getContainerProperties().getRawProperties()),
				client.getDistinguishedName());
		List<String> storageURLs = new ArrayList<>();
		Iterator<Map<String,String>> entries = registryClient.listEntries().iterator();
		while(entries.hasNext()) {
			try {
				Map<String,String> entry = entries.next();
				if("CoreServices".equals(entry.get(RegistryClient.INTERFACE_NAME))) {
					String url = entry.get(RegistryClient.ENDPOINT);
					SiteClient site = new SiteClient(new Endpoint(url), kernel.getClientConfiguration(), jwt);
					String storagesURL = site.getLinkUrl("storages");
					EnumerationClient ec = new EnumerationClient(new Endpoint(storagesURL), kernel.getClientConfiguration(), jwt);
					Iterator<String> urls = ec.iterator();
					while(urls.hasNext()) {
						String storageURL = urls.next();
						if(acceptURL(storageURL))storageURLs.add(storageURL);
					}
				}
			}catch(Exception ex) {}
		}
   		
		for(String storageURL: storageURLs) {
			FederatedSearchResult federatedSearchResult = new FederatedSearchResult();
			StorageClient sms = new StorageClient(new Endpoint(storageURL), kernel.getClientConfiguration(), jwt);
			Collection<String> searchResult = sms.searchMetadata(keyWord);
			
			federatedSearchResult.setStorageURL(storageURL);
			federatedSearchResult.addResourceNames(new ArrayList<String>(searchResult));
			
			result.addSearchResult(federatedSearchResult);
		}
		
		result.setSearchEndTime(new Date());
		
		return result;
	}
	
	private IRegistry getRegistryClient() throws Exception {
		RegistryHandler rh = kernel.getAttribute(RegistryHandler.class);
		IRegistry registryClient = rh.getExternalRegistryClient();
		if(registryClient==null){
			registryClient = rh.getRegistryClient();
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
