package eu.unicore.client.core;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.uas.json.JSONUtil;
import eu.unicore.uas.util.UnitParser;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * get info about site's capabilities and creates job submission clients
 * 
 * @author schuller
 */
public class SiteFactoryClient extends BaseServiceClient {

	public SiteFactoryClient(String endpoint, IClientConfiguration security, IAuthCallback auth) {
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
		return new SiteClient(bc.create(json), security, auth);
	}

	public SiteClient getOrCreateSite() throws Exception {
		// "global" sync to avoid creating more sites than required
		// TODO more controlled locking behavior
		synchronized (getClass()) {
			List<String>urls = getSiteList().getUrls(0, 1);
			if(urls.size()==0) {
				return createSite();
			}
			else {
				return new SiteClient(urls.get(0), security, auth);
			}
		}
	}

	public EnumerationClient getSiteList() throws Exception {
			return new EnumerationClient(getLinkUrl("sites"), security, auth);
	}
}
