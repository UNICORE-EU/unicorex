package eu.unicore.client.core;

import org.json.JSONObject;

public interface IJobSubmission {

	public JobClient submitJob(JSONObject job) throws Exception;

	public String getEndpoint();

}
