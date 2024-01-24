package eu.unicore.xnjs.json.sweep;

import java.util.Iterator;

import org.json.JSONObject;

public interface DocumentSweep extends Iterator<JSONObject>, Iterable<JSONObject>{

	public void setParent(JSONObject parent);

	default public Iterator<JSONObject>iterator(){
		return this;
	}

}
