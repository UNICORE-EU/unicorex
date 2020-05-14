package de.fzj.unicore.uas.lookup;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.StorageFactory;
import de.fzj.unicore.uas.StorageManagement;
import de.fzj.unicore.uas.TargetSystemFactory;
import de.fzj.unicore.uas.client.StorageClient;
import de.fzj.unicore.uas.client.StorageFactoryClient;
import de.fzj.unicore.uas.client.TSFClient;
import de.fzj.unicore.uas.client.TSSClient;
import de.fzj.unicore.uas.security.WSRFClientConfigurationProvider;
import de.fzj.unicore.uas.util.Pair;
import de.fzj.unicore.wsrflite.xmlbeans.client.IRegistryQuery;
import eu.unicore.security.wsutil.client.authn.DelegationSpecification;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * Lookup of all storages accessible to the user
 *
 * @author schuller
 */
public class StorageLister extends Lister<StorageClient>{

	final static Logger log = Log.getLogger(Log.CLIENT, StorageLister.class);

	private final IRegistryQuery registry;

	private final WSRFClientConfigurationProvider configurationProvider;

	private boolean includeSMF = true;
	private boolean includeTSS = true;
	private boolean includeSharedStorages = true;
	// TODO include jobs? ;-)

	/**
	 * @param registry
	 * @param configurationProvider
	 */
	public StorageLister(IRegistryQuery registry, WSRFClientConfigurationProvider configurationProvider){
		this(null,registry,configurationProvider,new AcceptAllFilter<StorageClient>());
	}
	
	/**
	 * @param executor
	 * @param registry
	 * @param configurationProvider
	 */
	public StorageLister(ExecutorService executor, IRegistryQuery registry, WSRFClientConfigurationProvider configurationProvider){
		this(executor,registry,configurationProvider,new AcceptAllFilter<StorageClient>());
	}

	/**
	 * 
	 * @param executor
	 * @param registry
	 * @param configurationProvider
	 * @param addressFilter - filter for accepting/rejecting service URLs 
	 */
	public StorageLister(ExecutorService executor, IRegistryQuery registry, WSRFClientConfigurationProvider configurationProvider, AddressFilter<StorageClient> addressFilter){
		super(executor,addressFilter,Integer.MAX_VALUE);
		this.registry=registry;
		this.configurationProvider=configurationProvider;
	}

	@Override
	public Iterator<StorageClient> iterator() {
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
		if(includeSMF){
			List<EndpointReferenceType>smfs=registry.listAccessibleServices(StorageFactory.SMF_PORT);
			for(EndpointReferenceType smf: smfs){
				if(addressFilter.accept(smf)){
					addProducer(new SMFProducer(smf,
							configurationProvider.getClientConfiguration(smf,DelegationSpecification.STANDARD),
							addressFilter));
				}
			}
		}

		// target system factories
		if(includeTSS){
			List<EndpointReferenceType>tsfs=registry.listAccessibleServices(TargetSystemFactory.TSF_PORT);
			for(EndpointReferenceType tsf: tsfs){
				if(addressFilter.accept(tsf)){
					addProducer(new TSSStorageProducer(tsf,
							configurationProvider.getClientConfiguration(tsf,DelegationSpecification.STANDARD),
							addressFilter));
				}
			}
		}

		// storages
		if(includeSharedStorages){
			List<EndpointReferenceType>smss=registry.listAccessibleServices(StorageManagement.SMS_PORT);
			for(EndpointReferenceType sms: smss){
				if(addressFilter.accept(sms)){
					addProducer(new StorageProducer(sms,
							configurationProvider.getClientConfiguration(sms,DelegationSpecification.STANDARD),
							addressFilter));
				}
			}
		}
	}

	/**
	 * whether to list dynamic storages (default: true)
	 */
	public void setIncludeSMF(boolean includeSMF) {
		this.includeSMF = includeSMF;
	}

	/**
	 * whether to list storages on TSS instances (default: true)
	 */
	public void setIncludeTSS(boolean includeTSS) {
		this.includeTSS = includeTSS;
	}

	/**
	 * whether to list storages registered directly in the registry (default: true)
	 */
	public void setIncludeSharedStorages(boolean includeSharedStorages) {
		this.includeSharedStorages = includeSharedStorages;
	}

	public static class StorageProducer implements Producer<StorageClient>{

		private final EndpointReferenceType epr;

		protected final IClientConfiguration securityProperties;

		protected final List<Pair<EndpointReferenceType,String>>errors=
				new ArrayList<Pair<EndpointReferenceType,String>>();

		private AtomicInteger runCount;

		protected BlockingQueue<StorageClient> target;

		protected AddressFilter<StorageClient> addressFilter;

		public StorageProducer(EndpointReferenceType epr, IClientConfiguration securityProperties, AddressFilter<StorageClient> addressFilter){
			this.epr=epr;
			this.securityProperties=securityProperties;
			this.addressFilter=addressFilter;
		}

		@Override
		public void run() {
			try{
				if(log.isDebugEnabled()){
					log.debug("Processing storage(s) at "+epr.getAddress().getStringValue());
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
				StorageClient c = new StorageClient(epr,securityProperties); 
				if(addressFilter.accept(c))
						target.put(c);
			}
		}

		@Override
		public void init(BlockingQueue<StorageClient> target,AtomicInteger runCount) {
			this.target=target;
			this.runCount=runCount;
		}
	}

	public static class SMFProducer extends StorageProducer {

		public SMFProducer(EndpointReferenceType epr, IClientConfiguration securityProperties, AddressFilter<StorageClient> filter){
			super(epr,securityProperties,filter);
		}

		public void handleEPR(EndpointReferenceType epr) throws Exception{
			StorageFactoryClient smf=new StorageFactoryClient(epr, securityProperties);
			for(EndpointReferenceType sEpr: smf.getStorages()){
				if(addressFilter.accept(sEpr)){
					StorageClient c = new StorageClient(sEpr,securityProperties); 
					if(addressFilter.accept(c))
							target.put(c);
				}
			}
		}
	}

	public static class TSSStorageProducer extends StorageProducer {

		public TSSStorageProducer(EndpointReferenceType epr, IClientConfiguration securityProperties,AddressFilter<StorageClient> filter){
			super(epr,securityProperties,filter);
		}

		public void handleEPR(EndpointReferenceType epr) throws Exception{
			TSFClient tsf=new TSFClient(epr, securityProperties);
			for(EndpointReferenceType tEpr: tsf.getAccessibleTargetSystems()){
				if(addressFilter.accept(tEpr)){
					TSSClient tss=new TSSClient(tEpr,securityProperties);
					for(EndpointReferenceType sEpr: tss.getStorages()){
						StorageClient c = new StorageClient(sEpr,securityProperties); 
						if(addressFilter.accept(c))
								target.put(c);
					}
				}
			}
		}
	}
}
