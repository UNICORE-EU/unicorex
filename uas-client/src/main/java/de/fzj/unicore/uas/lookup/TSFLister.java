package de.fzj.unicore.uas.lookup;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Logger;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.TargetSystemFactory;
import de.fzj.unicore.uas.client.TSFClient;
import de.fzj.unicore.uas.security.WSRFClientConfigurationProvider;
import de.fzj.unicore.uas.util.Pair;
import eu.unicore.security.wsutil.client.authn.DelegationSpecification;
import eu.unicore.services.ws.client.IRegistryQuery;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * Lookup of all sites (target systems) accessible to the user
 *
 * @author schuller
 */
public class TSFLister extends Lister<TSFClient>{

	final static Logger log = Log.getLogger(Log.CLIENT, TSFLister.class);
	
	private final IRegistryQuery registry;

	private final WSRFClientConfigurationProvider configurationProvider;
	
	/**
	 * @param registry
	 * @param configurationProvider
	 */
	public TSFLister(IRegistryQuery registry, WSRFClientConfigurationProvider configurationProvider){
		this(null,registry,configurationProvider,new AcceptAllFilter<TSFClient>());
	}
	
	/**
	 * @param executor
	 * @param registry
	 * @param configurationProvider
	 */
	public TSFLister(ExecutorService executor, IRegistryQuery registry, WSRFClientConfigurationProvider configurationProvider){
		this(executor,registry,configurationProvider,new AcceptAllFilter<TSFClient>());
	}
	
	/**
	 * 
	 * @param executor
	 * @param registry
	 * @param configurationProvider
	 * @param addressFilter - filter for accepting/rejecting service URLs 
	 */
	public TSFLister(ExecutorService executor, IRegistryQuery registry, WSRFClientConfigurationProvider configurationProvider, AddressFilter<TSFClient> addressFilter){
		super(executor,addressFilter,Integer.MAX_VALUE);
		this.registry=registry;
		this.configurationProvider=configurationProvider;
	}

	@Override
	public Iterator<TSFClient> iterator() {
		try{
			setupProducers();
		}
		catch(Exception ex){
			throw new RuntimeException(ex);
		}
		return super.iterator();
	}

	protected void setupProducers()throws Exception {
		// target system factories
		List<EndpointReferenceType>tsfs=registry.listAccessibleServices(TargetSystemFactory.TSF_PORT);
		for(EndpointReferenceType tsf: tsfs){
			if(addressFilter.accept(tsf)){
				addProducer(new TSFProducer(tsf, configurationProvider.getClientConfiguration(tsf, 
						DelegationSpecification.STANDARD), addressFilter));
			}
		}
	}

	public static class TSFProducer implements Producer<TSFClient>{

		private final EndpointReferenceType epr;

		protected final IClientConfiguration securityProperties;

		protected final List<Pair<EndpointReferenceType,String>>errors=
				new ArrayList<Pair<EndpointReferenceType,String>>();

		private AtomicInteger runCount;

		protected BlockingQueue<TSFClient> target;

		protected AddressFilter<TSFClient> addressFilter;
		
		public TSFProducer(EndpointReferenceType epr, IClientConfiguration securityProperties, AddressFilter<TSFClient> addressFilter){
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
				TSFClient c = new TSFClient(epr,securityProperties);
				if(addressFilter.accept(c))
					target.put(c);
			}
		}

		@Override
		public void init(BlockingQueue<TSFClient> target,AtomicInteger runCount) {
			this.target=target;
			this.runCount=runCount;
		}
	}
	
	/**
	 * filter for searching TSF by site name
	 */
	public static class SiteNameFilter implements AddressFilter<TSFClient>{

		private final String name;
		
		public SiteNameFilter(String name){
			this.name=name;
		}
		@Override
		public boolean accept(EndpointReferenceType epr) {
			return accept(epr.getAddress().getStringValue());
		}

		@Override
		public boolean accept(String uri) {
			return uri.contains("/"+name+"/");
		}

		@Override
		public boolean accept(TSFClient client) throws Exception {
			return true;
		}
		
	}
	
}
