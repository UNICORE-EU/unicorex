package eu.unicore.uas.json;

import org.json.JSONArray;
import org.json.JSONObject;

public class ApplicationRequirement implements Requirement {

	private final String appName, appVersion;

	public ApplicationRequirement(String appName, String appVersion){
		this.appName=appName;
		this.appVersion=appVersion;
	}

	@Override
	public String getDescription() {
		return "Application: "+appName+(appVersion!=null?" v"+appVersion:"");
	}

	@Override
	public boolean isFulfilled(JSONObject props) {
		JSONArray apps = props.optJSONArray("applications");
		if(apps==null || apps.length()==0)return false;
		try {
			for(int i=0; i<apps.length(); i++){
				String[] desc = apps.getString(i).split("---v");
				String name = desc[0];
				String ver  = desc[1];
				if(!appName.equalsIgnoreCase(name))continue;
				if(versionMatches(ver)) {
					return true;
				}
			}
		}catch(Exception ex) {}
		return false;
	}

	/**
	 * simple version match
	 * @param toCheck - version to check against
	 * @return <code>false</code> if "toCheck" is less than ours
	 */
	private boolean versionMatches(String toCheck) {
		if(appVersion==null || toCheck.equals(appVersion))return true;
		String[]parts = appVersion.split("\\.");
		String[]toCheckParts = toCheck.split("\\.");
		for(int i=0; i<parts.length; i++) {
			if(i>=toCheckParts.length) {
				return true;
			}
			String request = parts[i];
			String avail = toCheckParts[i];
			if(avail.compareTo(request)>0) {
				return true;
			}
			else if(avail.compareTo(request)<0) {
				return false;
			}
		}
		return false;
	}
}
