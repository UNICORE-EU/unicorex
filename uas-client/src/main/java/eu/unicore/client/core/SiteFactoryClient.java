package eu.unicore.client.core;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.json.JSONObject;

import de.fzj.unicore.uas.json.JSONUtil;
import de.fzj.unicore.uas.util.UnitParser;
import eu.unicore.client.Endpoint;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * get info about site's capabilities and creates job submission clients
 * 
 * @author schuller
 */
public class SiteFactoryClient extends BaseServiceClient {

	private EnumerationClient siteList;
	
	public SiteFactoryClient(Endpoint endpoint, IClientConfiguration security, IAuthCallback auth) {
		super(endpoint, security, auth);
	}
	
	public SiteClient createSite() throws Exception {
		return createSite(null,null);
	}

	public SiteClient createSite(Map<String,String> parameters, Calendar tt) throws Exception {
		JSONObject json = new JSONObject();
		if(tt!=null) {
			json.put("terminationTime", UnitParser.getISO8601().format(tt.getTime()));
		}
		json.put("parameters", JSONUtil.asJSON(parameters));
		HttpResponse resp = bc.post(json);
		bc.checkError(resp);
		if(201 != resp.getStatusLine().getStatusCode()){
			throw new Exception("Unexpected return status: "+
					resp.getStatusLine().getStatusCode());
		}
		String url = resp.getFirstHeader("Location").getValue();
		Endpoint ep = endpoint.cloneTo(url);
		return new SiteClient(ep, security, auth);
	}

	public SiteClient getOrCreateSite() throws Exception {
		// "global" sync to avoid creating more sites than required
		// TODO more controlled locking behavior
		synchronized (getClass()) {
			EnumerationClient ec = getOrCreateSiteList();
			List<String>urls = ec.getUrls(0, 1);
			if(urls.size()==0) {
				return createSite();
			}
			else {
				return new SiteClient(endpoint.cloneTo(urls.get(0)), security, auth);
			}
		}
	}
	
	public EnumerationClient getSiteList() throws Exception {
		return getOrCreateSiteList();
	}
	
	protected EnumerationClient getOrCreateSiteList() throws Exception{
		if(siteList==null) {
			Endpoint sitesEp = endpoint.cloneTo(getLinkUrl("sites"));
			siteList = new EnumerationClient(sitesEp, security, auth);
		}
		return siteList;
	}
}
