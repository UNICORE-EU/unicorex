package eu.unicore.uas.metadata;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.EnumerationClient;
import eu.unicore.client.core.SiteClient;
import eu.unicore.client.core.StorageClient;
import eu.unicore.security.Client;
import eu.unicore.services.Kernel;
import eu.unicore.services.registry.IRegistry;
import eu.unicore.services.rest.registry.RegistryHandler;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.services.restclient.RegistryClient;
import eu.unicore.services.rest.jwt.JWTDelegation;
import eu.unicore.services.rest.jwt.JWTServerProperties;


/**
 * Federated metada search provider<br/>
 * 
 * @author Konstantine Muradov
 * @author schuller
 */
public class FederatedSearchProvider implements Callable<FederatedSearchResultCollection> {

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
		List<Pattern> res=new ArrayList<>();
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
			List<String> searchResult = sms.searchMetadata(keyWord);
			federatedSearchResult.addResourceURLs(searchResult);
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
