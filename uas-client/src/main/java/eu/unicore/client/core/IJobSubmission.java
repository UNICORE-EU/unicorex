package eu.unicore.client.core;

import org.json.JSONObject;

import eu.unicore.client.Endpoint;

public interface IJobSubmission {

	public JobClient submitJob(JSONObject job) throws Exception;

	public Endpoint getEndpoint();

}
