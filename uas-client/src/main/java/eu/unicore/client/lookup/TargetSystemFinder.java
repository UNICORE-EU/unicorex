package eu.unicore.client.lookup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import de.fzj.unicore.uas.json.Builder;
import de.fzj.unicore.uas.json.Requirement;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.CoreClient;
import eu.unicore.client.core.SiteClient;
import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.security.wsutil.client.authn.ClientConfigurationProvider;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.util.Log;

/**
 * Very simple broker implementation: selects a suitable target system by looking
 * into the registry and matching job requirements
 * 
 * @author schuller
 */
public class TargetSystemFinder implements Broker {
	
	private final static Logger log = Log.getLogger(Log.CLIENT, TargetSystemFinder.class);
	
	public TargetSystemFinder(){

	}
	@Override
	public int getPriority(){
		return 1;
	}

	@Override
	public String getName(){
		return "LOCAL";
	}

	@Override
	public SiteClient findTSS(final IRegistryClient registry,
			ClientConfigurationProvider configurationProvider, IAuthCallback auth,
			Builder builder,
			SiteSelectionStrategy selectionStrategy)
					throws Exception{
		if(selectionStrategy==null)selectionStrategy = new RandomSelection();
		final List<SiteClient>available = listSites(registry, configurationProvider, auth, builder);
		SiteClient tss=null;
		if(available.size()==0){
			throw new Exception("No matching target system available");
		}
		else{
			tss=selectionStrategy.select(available);
		}
		return tss;
	}


	@Override
	public Collection<Endpoint>listCandidates(IRegistryClient registry, 
			ClientConfigurationProvider configurationProvider, IAuthCallback auth,
			Builder builder) 
					throws Exception{
		List<Endpoint>result=new ArrayList<>();
		List<SiteClient> sites = listSites(registry, configurationProvider, auth, builder);
		for(SiteClient site: sites){
			result.add(site.getEndpoint());
		}
		return result;
	}
	
	protected List<SiteClient> listSites(final IRegistryClient registry, 
			final ClientConfigurationProvider configurationProvider, IAuthCallback auth, Builder builder)
					throws Exception{
		final Collection<Requirement> requirements = builder.getRequirements();
		final String siteName = builder.getJSON().optString("Site name", null);
		
		final List<SiteClient>available=Collections.synchronizedList(new ArrayList<>());

		final String blackList=builder.getProperty("blacklist"); 
		final boolean checkResources=true;

		CoreEndpointLister siteList = new CoreEndpointLister(registry, configurationProvider, auth);
		if(blackList!=null){
			String[] blacklist=blackList.trim().split("[ ,]+");
			siteList.setAddressFilter(new Blacklist(blacklist));
		}

		if(siteName!=null){
			siteList.setAddressFilter(new SiteNameFilter(siteName));
		}

		for(CoreClient site: siteList){
			if(site == null){
				if(!siteList.isRunning())
					break;
			}
			else{
				String current = site.getEndpoint().getUrl();
				try{
					ErrorHolder err = new ErrorHolder();
					SiteClient sc = site.getSiteClient();
					if(matches(sc, requirements, err, checkResources)) {
						available.add(sc);
					}
				} catch(Exception ex) {
					Log.logException("Error on site "+current, ex, log);
				}
			}
		}
		return available;
	}

	/**
	 * check resource requirements
	 */
	public boolean matches(SiteClient tssClient, Collection<Requirement> requirements, ErrorHolder error, 
			boolean checkResources){
		try{
			if(!checkResources || requirements==null || requirements.size()==0){
				return true;
			}
			JSONObject props = tssClient.getProperties();
			for(Requirement r: requirements){
				if(r.isFulfilled(props))continue;
				error.code = "ERR_UNMET_REQUIREMENTS";
				error.message = "Requirement <"+r.getDescription()+"> not fulfilled on "+tssClient.getEndpoint().getUrl();
				return false;
			}
			return true;
		}catch(Exception e){
			error.code = "ERR_SITE_UNAVAILABLE";
			error.message = Log.createFaultMessage("Can't contact target system", e);
			return false;
		}
	}

	static class ErrorHolder {
		String code, message;
	}

}
