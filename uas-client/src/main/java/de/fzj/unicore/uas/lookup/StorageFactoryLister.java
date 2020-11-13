package de.fzj.unicore.uas.lookup;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Logger;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.StorageFactory;
import de.fzj.unicore.uas.client.StorageFactoryClient;
import de.fzj.unicore.uas.security.WSRFClientConfigurationProvider;
import de.fzj.unicore.uas.util.Pair;
import de.fzj.unicore.wsrflite.xmlbeans.client.IRegistryQuery;
import eu.unicore.security.wsutil.client.authn.DelegationSpecification;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * Lookup of all storage factory accessible to the user
 *
 * @author schuller
 */
public class StorageFactoryLister extends Lister<StorageFactoryClient>{

	final static Logger log = Log.getLogger(Log.CLIENT, StorageFactoryLister.class);
	
	private final IRegistryQuery registry;

	private final WSRFClientConfigurationProvider configurationProvider;
	
	/**
	 * @param registry
	 * @param configurationProvider
	 */
	public StorageFactoryLister(IRegistryQuery registry, WSRFClientConfigurationProvider configurationProvider){
		this(null,registry,configurationProvider,new AcceptAllFilter<StorageFactoryClient>());
	}
	
	/**
	 * @param executor
	 * @param registry
	 * @param configurationProvider
	 */
	public StorageFactoryLister(ExecutorService executor, IRegistryQuery registry, WSRFClientConfigurationProvider configurationProvider){
		this(executor,registry,configurationProvider,new AcceptAllFilter<StorageFactoryClient>());
	}
	
	/**
	 * @param executor
	 * @param registry
	 * @param configurationProvider
	 * @param addressFilter - filter for accepting/rejecting service URLs 
	 */
	public StorageFactoryLister(ExecutorService executor, IRegistryQuery registry, WSRFClientConfigurationProvider configurationProvider, AddressFilter<StorageFactoryClient> addressFilter){
		super(executor,addressFilter,Integer.MAX_VALUE);
		this.registry=registry;
		this.configurationProvider=configurationProvider;
	}

	@Override
	public Iterator<StorageFactoryClient> iterator() {
		try{
			setupProducers();
		}
		catch(Exception ex){
			throw new RuntimeException(ex);
		}
		return super.iterator();
	}

	protected void setupProducers()throws Exception {
		// factories first
		List<EndpointReferenceType>smfs=registry.listAccessibleServices(StorageFactory.SMF_PORT);
		for(EndpointReferenceType smf: smfs){
			if(addressFilter.accept(smf)){
				addProducer(new StorageFactoryProducer(smf,
						configurationProvider.getClientConfiguration(smf,DelegationSpecification.STANDARD),
						addressFilter));
			}
		}
	}

	public static class StorageFactoryProducer implements Producer<StorageFactoryClient>{

		private final EndpointReferenceType epr;

		protected final IClientConfiguration securityProperties;

		protected final List<Pair<EndpointReferenceType,String>>errors=
				new ArrayList<Pair<EndpointReferenceType,String>>();

		private AtomicInteger runCount;

		protected BlockingQueue<StorageFactoryClient> target;

		protected AddressFilter<StorageFactoryClient> addressFilter;
		
		public StorageFactoryProducer(EndpointReferenceType epr, IClientConfiguration securityProperties, AddressFilter<StorageFactoryClient> addressFilter){
			this.epr=epr;
			this.securityProperties=securityProperties;
			this.addressFilter=addressFilter;
		}

		@Override
		public void run() {
			try{
				if(log.isDebugEnabled()){
					log.debug("Processing storage factory at "+epr.getAddress().getStringValue());
				}
				handleEPR(epr);
			}
			catch(Exception ex){
				errors.add(new Pair<EndpointReferenceType,String>(epr,Log.createFaultMessage("", ex)));
			}
			finally{
				runCount.decrementAndGet();
			}
		}

		public void handleEPR(EndpointReferenceType epr) throws Exception{
			if(addressFilter.accept(epr)){
				StorageFactoryClient c = new StorageFactoryClient(epr,securityProperties); 
				if(addressFilter.accept(c))
						target.put(c);
			}
		}

		@Override
		public void init(BlockingQueue<StorageFactoryClient> target,AtomicInteger runCount) {
			this.target=target;
			this.runCount=runCount;
		}
	}
	
}
