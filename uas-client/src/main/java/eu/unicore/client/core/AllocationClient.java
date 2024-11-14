package eu.unicore.client.core;

import org.json.JSONObject;

import eu.unicore.client.Endpoint;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * An allocation is a special type of batch job, representing
 * a set of nodes/resources that are available to a particular user,
 * who can then submit jobs "into" the allocation.
 * 
 * @author schuller
 */
public class AllocationClient extends JobClient implements IJobSubmission {

	public AllocationClient(Endpoint endpoint, IClientConfiguration security, IAuthCallback auth) {
		super(endpoint, security, auth);
	}

	public JobClient submitJob(JSONObject job) throws Exception {
		String url = bc.create(job);
		return new JobClient(endpoint.cloneTo(url), security, auth);
	}
}
