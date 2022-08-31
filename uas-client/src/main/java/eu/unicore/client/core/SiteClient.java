package eu.unicore.client.core;

import org.apache.http.HttpResponse;
import org.json.JSONObject;

import eu.unicore.client.Endpoint;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * Provides info about a site's capabilities (resources, available storages, applications,...) 
 * and allows to create and manage jobs
 * 
 * @author schuller
 */
public class SiteClient extends BaseServiceClient implements IJobSubmission {

	public SiteClient(Endpoint endpoint, IClientConfiguration security, IAuthCallback auth) {
		super(endpoint, security, auth);
	}

	public JobClient submitJob(JSONObject job) throws Exception {
		HttpResponse resp = bc.post(job);
		bc.checkError(resp);
		if(201 != resp.getStatusLine().getStatusCode()){
			throw new Exception("Unexpected return status: "+
					resp.getStatusLine().getStatusCode());
		}
		String url = resp.getFirstHeader("Location").getValue();
		Endpoint ep = endpoint.cloneTo(url);
		return new JobClient(ep, security, auth);
	}

	public EnumerationClient getJobsList() throws Exception {
		String url = getLinkUrl("jobs");
		Endpoint ep = endpoint.cloneTo(url);
		return new EnumerationClient(ep, security, auth);
	}

	public AllocationClient createAllocation(JSONObject allocation) throws Exception {
		HttpResponse resp = bc.post(allocation);
		bc.checkError(resp);
		if(201 != resp.getStatusLine().getStatusCode()){
			throw new Exception("Unexpected return status: "+
					resp.getStatusLine().getStatusCode());
		}
		String url = resp.getFirstHeader("Location").getValue();
		Endpoint ep = endpoint.cloneTo(url);
		return new AllocationClient(ep, security, auth);
	}

}
