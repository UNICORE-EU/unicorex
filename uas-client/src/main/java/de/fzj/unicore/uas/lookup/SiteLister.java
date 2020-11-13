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
import de.fzj.unicore.uas.client.TSSClient;
import de.fzj.unicore.uas.security.WSRFClientConfigurationProvider;
import de.fzj.unicore.uas.util.Pair;
import de.fzj.unicore.wsrflite.xmlbeans.client.IRegistryQuery;
import eu.unicore.security.wsutil.client.authn.DelegationSpecification;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * Lookup of all sites (target systems) accessible to the user
 *
 * @author schuller
 */
public class SiteLister extends Lister<TSSClient>{

	final static Logger log = Log.getLogger(Log.CLIENT, SiteLister.class);
	
	private final IRegistryQuery registry;

	private final WSRFClientConfigurationProvider configurationProvider;
	
	/**
	 * @param registry
	 * @param configurationProvider
	 */
	public SiteLister(IRegistryQuery registry, WSRFClientConfigurationProvider configurationProvider){
		this(null,registry,configurationProvider,new AcceptAllFilter<TSSClient>());
	}
	
	/**
	 * @param executor
	 * @param registry
	 * @param configurationProvider
	 */
	public SiteLister(ExecutorService executor, IRegistryQuery registry, WSRFClientConfigurationProvider configurationProvider){
		this(executor,registry,configurationProvider,new AcceptAllFilter<TSSClient>());
	}
	
	/**
	 * 
	 * @param executor
	 * @param registry
	 * @param configurationProvider
	 * @param addressFilter - filter for accepting/rejecting service URLs 
	 */
	public SiteLister(ExecutorService executor, IRegistryQuery registry, WSRFClientConfigurationProvider configurationProvider, AddressFilter<TSSClient> addressFilter){
		super(executor,addressFilter,Integer.MAX_VALUE);
		this.registry=registry;
		this.configurationProvider=configurationProvider;
	}

	@Override
	public Iterator<TSSClient> iterator() {
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
				addProducer(new TSSProducer(tsf, configurationProvider.getClientConfiguration(tsf, 
						DelegationSpecification.STANDARD), addressFilter));
			}
		}
	}

	public static class TSSProducer implements Producer<TSSClient>{

		private final EndpointReferenceType epr;

		protected final IClientConfiguration securityProperties;

		protected final List<Pair<EndpointReferenceType,String>>errors=
				new ArrayList<Pair<EndpointReferenceType,String>>();

		private AtomicInteger runCount;

		protected BlockingQueue<TSSClient> target;

		protected AddressFilter<TSSClient> addressFilter;
		
		public TSSProducer(EndpointReferenceType epr, IClientConfiguration securityProperties, AddressFilter<TSSClient> addressFilter){
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
			TSFClient tsf=new TSFClient(epr, securityProperties);
			for(EndpointReferenceType sEpr: tsf.getAccessibleTargetSystems()){
				if(addressFilter.accept(sEpr)){
					TSSClient c = new TSSClient(sEpr,securityProperties);
					if(addressFilter.accept(c))
						target.put(c);
				}
			}
		}

		@Override
		public void init(BlockingQueue<TSSClient> target, AtomicInteger runCount) {
			this.target=target;
			this.runCount=runCount;
		}
	}
	
	/**
	 * filter for searching TSS by name
	 */
	public static class NameFilter implements AddressFilter<TSSClient>{

		private final String name;
		
		public NameFilter(String name){
			this.name=name;
		}
		@Override
		public boolean accept(EndpointReferenceType epr) {
			return true;
		}

		@Override
		public boolean accept(String uri) {
			return true;
		}

		@Override
		public boolean accept(TSSClient client) throws Exception {
			return name==null || name.equals(client.getTargetSystemName());
		}
		
	}
	
}
