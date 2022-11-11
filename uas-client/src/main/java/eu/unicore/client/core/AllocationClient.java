package eu.unicore.client.core;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.json.JSONObject;

import eu.unicore.client.Endpoint;
import eu.unicore.services.rest.client.IAuthCallback;
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
		ClassicHttpResponse resp = bc.post(job);
		bc.checkError(resp);
		if(201 != resp.getCode()){
			throw new Exception("Unexpected return status: "+
					resp.getCode());
		}
		String url = resp.getFirstHeader("Location").getValue();
		Endpoint ep = endpoint.cloneTo(url);
		return new JobClient(ep, security, auth);
	}
}
