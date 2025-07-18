package eu.unicore.xnjs.json;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.unicore.security.Client;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.ems.processors.JobProcessor;
import eu.unicore.xnjs.idb.ApplicationInfo;
import eu.unicore.xnjs.idb.Incarnation;
import eu.unicore.xnjs.io.DataStageInInfo;
import eu.unicore.xnjs.io.DataStageOutInfo;
import eu.unicore.xnjs.io.DataStagingInfo;
import eu.unicore.xnjs.json.sweep.DocumentSweep;
import eu.unicore.xnjs.json.sweep.JSONSweepProcessor;
import eu.unicore.xnjs.json.sweep.ParameterSweep;
import eu.unicore.xnjs.json.sweep.StagingSweep;
import eu.unicore.xnjs.resources.ResourceRequest;
import eu.unicore.xnjs.resources.ResourceSet;
import eu.unicore.xnjs.util.ErrorCode;
import eu.unicore.xnjs.util.JSONUtils;

public class JSONJobProcessor extends JobProcessor<JSONObject> {

	private JSONObject jobDescription;

	public JSONJobProcessor(XNJS xnjs) {
		super(xnjs);
	}

	@Override
	protected JSONObject getJobDescriptionDocument(){
		if(jobDescription==null) {
			try{
				jobDescription = new JSONObject((String)action.getAjd());
			}catch(Exception ex) {}
		}
		return jobDescription;
	}

	@Override
	protected void rewriteJobDescription(JSONObject modified) {
		if(modified==null) {
			modified = new JSONObject();
		}
		action.setAjd(modified.toString());
	}

	/**
	 * if the job is a parameter sweep job, change the action type
	 * so that the JSONSweepProcessor} can take over
	 */
	@Override
	protected void handleCreated() throws ExecutionException {
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
			throw ExecutionException.wrapped(ex);
		}
	}

	@Override
	protected void extractFromJobDescription()throws ExecutionException{
		if(isEmptyJob())return;
		Incarnation grounder = xnjs.get(Incarnation.class);
		Client client = action.getClient();
		try{
			JSONObject jd = getJobDescriptionDocument();
			ApplicationInfo fromUser = JSONParser.parseSubmittedApplication(jd);
			ApplicationInfo applicationInfo = grounder.incarnateApplication(fromUser, client);
			action.setApplicationInfo(applicationInfo);
			updateExecutionContext(applicationInfo);
			action.setJobName(getJobName());
			action.setUmask(getUmask());
			List<ResourceRequest>resourceRequest = JSONParser.parseResourceRequest(
					jd.optJSONObject("Resources"));
			action.getExecutionContext().setResourceRequest(resourceRequest);
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
			if(client!=null) {
				client.setUserEmail(jd.optString("User email", null));
			}
			action.setStageIns(extractStageInInfo());
			action.setStageOuts(extractStageOutInfo());
			rewriteJobDescription(jobDescription);
			action.setDirty();
		} catch (Exception e) {
			throw ExecutionException.wrapped(e);
		}
	}

	@Override
	protected void setupNotifications() {
		action.setNotificationURLs(JSONParser.parseNotificationURLs(getJobDescriptionDocument()));
		action.setNotifyStates(JSONParser.parseNotificationTriggers(getJobDescriptionDocument()));
		action.setNotifyBSSStates(JSONParser.parseNotificationBSSTriggers(getJobDescriptionDocument()));
	}

	@Override
	protected boolean isEmptyJob() {
		return getJobDescriptionDocument().keySet().isEmpty();
	}

	@Override
	protected String getJobName() {
		String n = JSONUtils.getString(getJobDescriptionDocument(), "Name");
		if(n==null) {
			n = JSONUtils.getOrDefault(getJobDescriptionDocument(), "ApplicationName", "UNICORE_Job");
		}
		return n;
	}

	@Override
	protected String getPreferredLoginNode() {
		if(action.getApplicationInfo()!=null && action.getApplicationInfo().getPreferredLoginNode()!=null) {
			return action.getApplicationInfo().getPreferredLoginNode();
		}
		else {
			return JSONUtils.getString(getJobDescriptionDocument(), "Login node");
		}
	}

	@Override
	protected String getUmask() {
		return JSONParser.parseUmask(getJobDescriptionDocument());
	}
	
	@Override
	protected boolean hasStageIn() {
		JSONArray imports = getJobDescriptionDocument().optJSONArray("Imports");
		return imports!=null && imports.length()>0;
	}

	@Override
	protected boolean hasStageOut() {
		JSONArray exports = getJobDescriptionDocument().optJSONArray("Exports");
		return exports!=null && exports.length()>0;
	}

	@Override
	protected void extractNotBefore() throws ExecutionException {
		Date notBefore = JSONParser.parseNotBefore(getJobDescriptionDocument());
		if(notBefore!=null){
			action.setNotBefore(notBefore.getTime());
		}
	}

	@Override
	protected List<DataStageInInfo> extractStageInInfo() throws Exception {
		List<DataStageInInfo>result = new ArrayList<>();
		Object imp = getJobDescriptionDocument().opt("Imports");
		if(imp!=null) {
			if(imp instanceof JSONArray) {
				JSONArray imports = (JSONArray)imp;
				result.addAll(doExtractStageIn(imports));
			}
			else if(imp instanceof JSONObject){
				JSONObject imports = (JSONObject)imp;
				for(String to: imports.keySet()) {
					JSONObject spec = imports.getJSONObject(to);
					result.add(JSONParser.parseStageIn(to, spec));
				}
			}
			else throw new ExecutionException(ErrorCode.ERR_JOB_DESCRIPTION,
					"'Imports' must be an array or a map");
		}
		String preferredLogin = getPreferredLoginNode();
		if(preferredLogin!=null) {
			for(DataStagingInfo i: result) {
				if (i.getPreferredLoginNode()==null) {
					i.setPreferredLoginNode(preferredLogin);
				}
			}
		}
		return result;
	}

	protected List<DataStageInInfo> doExtractStageIn(JSONArray imports) throws Exception {
		List<DataStageInInfo>result = new ArrayList<>();
		if(imports!=null) {
			for(int i = 0; i<imports.length(); i++) {
				JSONObject in = imports.getJSONObject(i);
				result.add(JSONParser.parseStageIn(null, in));
			}
		}
		return result;
	}

	@Override
	protected List<DataStageOutInfo> extractStageOutInfo() throws Exception {
		List<DataStageOutInfo>result = new ArrayList<>();
		Object exp = getJobDescriptionDocument().opt("Exports");
		if(exp!=null) {
			if(exp instanceof JSONArray) {
				JSONArray exports = (JSONArray)exp;
				for(int i = 0; i<exports.length(); i++) {
					JSONObject spec = exports.getJSONObject(i);
					result.add(JSONParser.parseStageOut(null, spec));
				}
			}
			else if(exp instanceof JSONObject){
				JSONObject exports = (JSONObject)exp;
				for(String from: exports.keySet()) {
					JSONObject out = exports.getJSONObject(from);
					result.add(JSONParser.parseStageOut(from, out));
				}
			}
			else throw new ExecutionException(ErrorCode.ERR_JOB_DESCRIPTION,
					"'Exports' must be an array or a map");
		}
		String preferredLogin = getPreferredLoginNode();
		if(preferredLogin!=null) {
			for(DataStagingInfo i: result) {
				if (i.getPreferredLoginNode()==null) {
					i.setPreferredLoginNode(preferredLogin);
				}
			}
		}
		return result;
	}

	protected boolean checkForSweeps() throws Exception {
		JSONObject j = getJobDescriptionDocument();
		JSONObject parameters = j.optJSONObject("Parameters");
		if(parameters!=null) {
			for(String name: parameters.keySet()) {
				if(parameters.get(name) instanceof JSONObject) {
					return true;
				}
			}
		}
		if(j.optJSONArray("Imports")!=null) {
			JSONArray imports = j.getJSONArray("Imports");
			for(int i=0; i<imports.length(); i++) {
				JSONObject im = imports.getJSONObject(i);
				Object f = im.opt("From");
				if(f!=null && f instanceof JSONArray) {
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
			for(String name: parameters.keySet()) {
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
