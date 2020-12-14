package de.fzj.unicore.xnjs.json;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.ems.JobProcessor;
import de.fzj.unicore.xnjs.ems.ProcessingException;
import de.fzj.unicore.xnjs.idb.ApplicationInfo;
import de.fzj.unicore.xnjs.idb.Incarnation;
import de.fzj.unicore.xnjs.io.DataStageInInfo;
import de.fzj.unicore.xnjs.io.DataStageOutInfo;
import de.fzj.unicore.xnjs.json.sweep.DocumentSweep;
import de.fzj.unicore.xnjs.json.sweep.JSONSweepProcessor;
import de.fzj.unicore.xnjs.json.sweep.ParameterSweep;
import de.fzj.unicore.xnjs.json.sweep.StagingSweep;
import de.fzj.unicore.xnjs.resources.ResourceRequest;
import de.fzj.unicore.xnjs.resources.ResourceSet;
import de.fzj.unicore.xnjs.util.JSONUtils;
import eu.unicore.security.Client;

public class JSONJobProcessor extends JobProcessor<JSONObject> {

	public JSONJobProcessor(XNJS xnjs) {
		super(xnjs);
	}

	private JSONObject jobDescription;

	@Override
	protected JSONObject getJobDescriptionDocument(){
		if(jobDescription==null) {
			try{
				jobDescription = new JSONObject((String)action.getAjd());
			}catch(Exception ex) {}
		}
		return jobDescription;
	}
	
	/**
	 * if the job is a parameter sweep job, change the action type
	 * so that the JSONSweepProcessor} can take over
	 */
	@Override
	protected void handleCreated() throws ProcessingException {
		try{
			if(!action.getType().equals(JSONSweepProcessor.sweepActionType) && checkForSweeps()){
				action.setType(JSONSweepProcessor.sweepActionType);
				action.addLogTrace("This is a parameter sweep job, changing type to '"
						+JSONSweepProcessor.sweepActionType+"'");
			}
			else{
				super.handleCreated();
			}	
		}catch(Exception ex){
			throw new ProcessingException(ex);
		}
	}

	@Override
	protected void extractFromJobDescription()throws ExecutionException{
		Incarnation grounder = xnjs.get(Incarnation.class);
		Client client=action.getClient();
		ecm.getContext(action);
		try{
			//do an incarnation now...
			JSONObject jd = getJobDescriptionDocument();
			ApplicationInfo fromUser = new JSONParser().parseSubmittedApplication(jd);
			ApplicationInfo applicationInfo = grounder.incarnateApplication(fromUser, client);
			action.setApplicationInfo(applicationInfo);
			updateExecutionContext(applicationInfo);

			// resources
			List<ResourceRequest>resourceRequest = new JSONParser().parseResourceRequest(
					jd.optJSONObject("Resources"));
			action.getExecutionContext().setResourceRequest(resourceRequest);

			//job name
			action.setJobName(getJobName());
			//project
			String requestedProject = jd.optString("Project", null);
			if(requestedProject!=null){
				ResourceRequest projectRequest = ResourceRequest.find(resourceRequest, ResourceSet.PROJECT);
				if(projectRequest==null){
					projectRequest = new ResourceRequest(ResourceSet.PROJECT, requestedProject);
					resourceRequest.add(projectRequest);
				}
				else{
					projectRequest.setRequestedValue(requestedProject);
				}
			}

			String email = jd.optString("User email", null);
			Client c=action.getClient();
			if(c!=null && email!=null)c.setUserEmail(email);

			extractStageInInfo();
			extractStageOutInfo();

			action.setDirty();

		} catch (Exception e) {
			if(e instanceof ExecutionException){
				throw (ExecutionException)e;
			}
			else{
				throw new ExecutionException(e);
			}
		}
	}

	@Override
	protected void setupNotifications() {
		try {
			String u = JSONUtils.getString(getJobDescriptionDocument(), "Notification");
			if(u!=null) {
				List<String> urls = new ArrayList<>();
				urls.add(u);
				action.setNotificationURLs(urls);
			}
		}catch(Exception ex) {}
	}

	@Override
	protected boolean isEmptyJob() {
		return JSONObject.getNames(getJobDescriptionDocument()).length==0;
	}

	@Override
	protected String getJobName() {
		String n = JSONUtils.getString(getJobDescriptionDocument(), "Name");
		if(n==null) {
			n = JSONUtils.getString(getJobDescriptionDocument(), "ApplicationName");
		}
		if(n==null) {
			n = "UNICORE_Job";
		}
		return n;
	}

	@Override
	protected boolean hasStageIn() {
		return getJobDescriptionDocument().optJSONArray("Imports")!=null;
	}

	@Override
	protected boolean hasStageOut() {
		return getJobDescriptionDocument().optJSONArray("Exports")!=null;
	}

	@Override
	protected void extractNotBefore() throws ProcessingException {
		String notBefore = JSONUtils.getString(getJobDescriptionDocument(), "Not before");
		if(notBefore!=null){
			try{
				Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(notBefore);
				action.setNotBefore(date.getTime());
			}catch(Exception ex) {
				throw new ProcessingException("Could not parse start time from <"+notBefore+">", ex);
			}
		}
	}

	@Override
	protected List<DataStageInInfo> extractStageInInfo() throws Exception {
		return doExtractStageIn(getJobDescriptionDocument().optJSONArray("Imports"));
	}
	
	protected List<DataStageInInfo> doExtractStageIn(JSONArray imports) throws Exception {
		List<DataStageInInfo>result = new ArrayList<>();
		if(imports!=null) {
			for(int i = 0; i<imports.length(); i++) {
				JSONObject in = imports.getJSONObject(i);
				DataStageInInfo dsi = new DataStageInInfo();
				String to = JSONUtils.getString(in, "To");
				String source = JSONUtils.getString(in, "From");
				dsi.setFileName(to);
				dsi.setSources(new URI[]{new URI(source)});
				if(source.startsWith("inline:")) {
					dsi.setInlineData(JSONUtils.readMultiLine("Data", "", in));
				}
				new JSONParser().extractDataStagingOptions(in, dsi);
				result.add(dsi);
			}
		}
		return result;
	}

	@Override
	protected List<DataStageOutInfo> extractStageOutInfo() throws Exception {
		List<DataStageOutInfo>result = new ArrayList<>();
		JSONArray exports = getJobDescriptionDocument().optJSONArray("Exports");
		if(exports!=null) {
			for(int i = 0; i<exports.length(); i++) {
				JSONObject ex = exports.getJSONObject(i);
				DataStageOutInfo dso = new DataStageOutInfo();
				String from = JSONUtils.getString(ex, "From");
				String target = JSONUtils.getString(ex, "To");
				dso.setFileName(from);
				dso.setTarget(new URI(target));
				new JSONParser().extractDataStagingOptions(ex, dso);
				result.add(dso);
			}
		}
		return result;
	}

	protected boolean checkForSweeps() throws Exception {
		JSONObject j = getJobDescriptionDocument();
		JSONObject parameters = j.optJSONObject("Parameters");
		if(parameters!=null) {
			for(String name: JSONObject.getNames(parameters)) {
				if(parameters.get(name) instanceof JSONObject) {
					return true;
				}
			}
		}
		if(j.optJSONArray("Imports")!=null) {
			JSONArray imports = j.getJSONArray("Imports");
			for(int i=0; i<imports.length(); i++) {
				JSONObject im = imports.getJSONObject(i);
				if(im.get("From")instanceof JSONArray) {
					return true;
				}
			}
		}
		return false;
	}
	
	protected DocumentSweep createSweep() throws Exception {
		DocumentSweep sweepSpec = null;
		JSONObject j = getJobDescriptionDocument();
		JSONObject parameters = j.optJSONObject("Parameters");
		if(parameters!=null) {
			for(String name: JSONObject.getNames(parameters)) {
				if(parameters.get(name) instanceof JSONObject) {
					sweepSpec = new ParameterSweep(name, parameters.getJSONObject(name));
				}
			}
		}
		if(j.optJSONArray("Imports")!=null) {
			JSONArray imports = j.getJSONArray("Imports");
			for(int i=0; i<imports.length(); i++) {
				JSONObject im = imports.getJSONObject(i);
				if(im.getString("From").equals(JSONSweepProcessor.sweepFileMarker)) {
					StagingSweep sSweep = new StagingSweep(JSONSweepProcessor.sweepFileMarker);
					sSweep.setFiles(JSONUtils.asStringArray(j.getJSONArray(JSONSweepProcessor.sweepFileMarker)));
					sweepSpec = sSweep;
				}
			}
		}
		return sweepSpec;
	}
}
