package de.fzj.unicore.uas.json;

import org.json.JSONArray;
import org.json.JSONObject;

public class ApplicationRequirement implements Requirement {

	private final String appName, appVersion;
	
	public ApplicationRequirement(String appName, String appVersion){
		this.appName=appName;
		this.appVersion=appVersion;
	}
	
	public String getDescription() {
		return "Application: "+appName+(appVersion!=null?" v"+appVersion:"");
	}

	public boolean isFulfilled(JSONObject props) {
		JSONArray apps = props.optJSONArray("applications");
		if(apps==null || apps.length()==0)return false;
		try {
			
			for(int i=0; i<apps.length(); i++){
				String[] desc = apps.getString(i).split("---v");
				String name = desc[0];
				String ver  = desc[1];
				if(!appName.equalsIgnoreCase(name))continue;
				if(appVersion==null)return true;
				if(appVersion.equalsIgnoreCase(ver))return true;
			}
		}catch(Exception ex) {}
		return false;
	}

	@Override
	public int hashCode() {
		int result = 1;
		result = 31 * result + ((appName == null) ? 0 : appName.hashCode());
		result = 31 * result
				+ ((appVersion == null) ? 0 : appVersion.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final ApplicationRequirement other = (ApplicationRequirement) obj;
		if (appName == null) {
			if (other.appName != null)
				return false;
		} else if (!appName.equals(other.appName))
			return false;
		if (appVersion == null) {
			if (other.appVersion != null)
				return false;
		} else if (!appVersion.equals(other.appVersion))
			return false;
		return true;
	}

}
