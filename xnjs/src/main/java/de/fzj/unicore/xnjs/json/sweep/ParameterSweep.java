package de.fzj.unicore.xnjs.json.sweep;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Handle argument sweeps
 *
 * @author schuller
 */
public class ParameterSweep implements DocumentSweep {

	private final String name;
	
	private final List<String>values = new ArrayList<>();
	private Iterator<String> it = null;
	
	private JSONObject parent;
	
	public ParameterSweep(String name, JSONObject parameterSpec){
		this.name = name;
		sweep(parameterSpec);
	}
	
	@Override
	public void setParent(JSONObject parent) {
		this.parent = parent;
	}
	
	public void setValues(String[]values){
		this.values.addAll(Arrays.asList(values));
	}
	
	public void sweep(JSONObject json){
		JSONArray valueArray = json.optJSONArray("Values");
		if(valueArray!=null){
			for(int i=0; i<valueArray.length();i++){
				try{
					values.add(String.valueOf(valueArray.get(i)));
				}catch(JSONException e){
					break;
				}
			}
		}
		else{
			try{
				BigDecimal from=new BigDecimal(json.getString("From"));
				BigDecimal to=new BigDecimal(json.getString("To"));
				BigDecimal step=new BigDecimal(json.optString("Step","1"));
				sweep(from,to,step);
			}catch(JSONException je){
				throw new IllegalArgumentException("Must give From and To values");
			}
		}
	}
	
	public void sweep(long from, long to, long step){
		sweep(new BigDecimal(from),new BigDecimal(to),new BigDecimal(step));
	}
	
	public void sweep(double from, double to, double step){
		sweep(new BigDecimal(from),new BigDecimal(to),new BigDecimal(step));
	}
	
	private void sweep(BigDecimal from, BigDecimal to, BigDecimal step){
		if(step.signum()>0){
			while(from.compareTo(to)<=0){
				values.add(String.valueOf(from));
				from=from.add(step);
			}
		}
		else if(step.signum()<0){
			while(from.compareTo(to)>=0){
				values.add(String.valueOf(from));
				from=from.add(step);
			}
		}
		else throw new IllegalArgumentException("Step must be non-zero");
	}
	
	public void reset() {
		it = values.iterator();
	}

	@Override
	public boolean hasNext() {
		if(parent==null)throw new IllegalStateException();
		if(it==null)it = values.iterator();
		return it.hasNext();
	}

	@Override
	public JSONObject next() {
		if(parent==null)throw new IllegalStateException();
		if(it==null)it = values.iterator();
		
		try {
			JSONObject child = new JSONObject(parent.toString());
			String value = it.next();
			child.getJSONObject("Parameters").put(name, value);
			String desc = parent.optString(JSONSweepProcessor.sweepDescription, "");
			child.put(JSONSweepProcessor.sweepDescription, desc+" param:'" +name+"'='"+value+"'");
			return child;
		}catch(JSONException je) {
			throw new RuntimeException(je);
		}
	}

}
