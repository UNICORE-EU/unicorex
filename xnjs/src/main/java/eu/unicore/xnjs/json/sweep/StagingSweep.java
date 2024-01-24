package eu.unicore.xnjs.json.sweep;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Helper for dealing with JSDL staging sweep
 *
 * @author schuller
 */
public class StagingSweep implements DocumentSweep{

	private final String marker;

	private final List<String>files = new ArrayList<>();
	private Iterator<String> it = null;
	
	private JSONObject parent;
	
	public StagingSweep(String marker){
		this.marker = marker;
	}

	@Override
	public void setParent(JSONObject parent) {
		this.parent = parent;
	}

	public List<String>getFiles(){
		return files;
	}

	public void setFiles(String[]files){
		this.files.addAll(Arrays.asList(files));
	}
	
	public void reset() {
		it = files.iterator();
	}

	@Override
	public boolean hasNext() {
		if(parent==null)throw new IllegalStateException();
		if(it==null)it = files.iterator();
		
		return it.hasNext();
	}
	
	@Override
	public JSONObject next() {
		if(parent==null)throw new IllegalStateException();
		if(it==null)it = files.iterator();
		try {
			JSONObject child = new JSONObject(parent.toString());
			JSONArray imports = child.getJSONArray("Imports");
			String value = it.next();
			String target = null;
			for(int i=0;i<imports.length();i++) {
				JSONObject o = imports.getJSONObject(i);
				if(marker.equals((o.getString("From")))){
					o.put("From", value);
					target = o.getString("To");
					break;
				}
			}
			String desc = parent.optString(JSONSweepProcessor.sweepDescription, "");
			child.put(JSONSweepProcessor.sweepDescription, desc+" stage-in:'" +value+"'->'"+target+"'");
			return child;
		}catch(JSONException je) {
			throw new RuntimeException(je);
		}
	}

}
