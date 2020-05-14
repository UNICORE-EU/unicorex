package eu.unicore.client.lookup;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import de.fzj.unicore.uas.util.Pair;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.CoreClient;
import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.client.registry.RegistryClient;
import eu.unicore.security.wsutil.client.authn.ClientConfigurationProvider;
import eu.unicore.security.wsutil.client.authn.DelegationSpecification;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;

public class CoreEndpointLister extends Lister<CoreClient>{

	final static Logger log = Log.getLogger(Log.CLIENT, CoreEndpointLister.class);
	
	private final IRegistryClient registry;

	private final ClientConfigurationProvider configurationProvider;
	
	private final IAuthCallback auth;
	
	public CoreEndpointLister(IRegistryClient registry, ClientConfigurationProvider configurationProvider, IAuthCallback auth){
		super();
		this.registry = registry;
		this.configurationProvider = configurationProvider;
		this.auth = auth;
	}

	@Override
	public Iterator<CoreClient> iterator() {
		try{
			setupProducers();
		}
		catch(Exception ex){
			throw new RuntimeException(ex);
		}
		return super.iterator();
	}

	protected void setupProducers()throws Exception {
		List<Endpoint>sites = registry.listEntries(new RegistryClient.ServiceTypeFilter("CoreServices"));
		for(Endpoint site: sites){
			if(addressFilter.accept(site)){
				addProducer(new CoreClientProducer(site, 
						configurationProvider.getClientConfiguration(site.getUrl(),null,DelegationSpecification.DO_NOT),
						auth, 
						addressFilter));
			}
		}
	}

	public static class CoreClientProducer implements Producer<CoreClient>{

		private final Endpoint epr;

		protected final IClientConfiguration securityProperties;
		protected final IAuthCallback auth;
		
		protected final List<Pair<Endpoint,String>>errors = new ArrayList<>();

		private AtomicInteger runCount;

		protected BlockingQueue<CoreClient> target;

		protected AddressFilter addressFilter;
		
		public CoreClientProducer(Endpoint epr, IClientConfiguration securityProperties, IAuthCallback auth, AddressFilter addressFilter) {
			this.epr = epr;
			this.securityProperties = securityProperties;
			this.auth = auth;
			this.addressFilter = addressFilter;
		}

		@Override
		public void run() {
			try{
				if(log.isDebugEnabled()){
					log.debug("Processing site at "+epr.getUrl());
				}
				handleEndpoint(epr);
			}
			catch(Exception ex){
				errors.add(new Pair<>(epr,Log.createFaultMessage("", ex)));
			}
			finally{
				runCount.decrementAndGet();
			}
		}

		public void handleEndpoint(Endpoint epr) throws Exception{
			CoreClient c = new CoreClient(epr, securityProperties, auth);
			if(addressFilter.accept(c)) {
				target.put(c);
			}
		}

		@Override
		public void init(BlockingQueue<CoreClient> target, AtomicInteger runCount) {
			this.target=target;
			this.runCount=runCount;
		}
	}
	
}
