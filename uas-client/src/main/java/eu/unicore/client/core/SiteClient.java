package eu.unicore.client.core;

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
		String newJob = bc.create(job);
		return new JobClient(endpoint.cloneTo(newJob), security, auth);
	}

	public EnumerationClient getJobsList() throws Exception {
		String url = getLinkUrl("jobs");
		return new EnumerationClient(endpoint.cloneTo(url), security, auth);
	}

	public AllocationClient createAllocation(JSONObject allocation) throws Exception {
		String allocationURL = bc.create(allocation);
		return new AllocationClient( endpoint.cloneTo(allocationURL), security, auth);
	}

}
