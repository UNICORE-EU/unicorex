package de.fzj.unicore.uas.json;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Helper for dealing with JSDL argument sweep
 *
 * @author schuller
 */
public class ArgumentSweep {

	protected final int argNo;
	
	List<String>values = new ArrayList<>();
	
	public ArgumentSweep(int argNo){
		this.argNo=argNo;
	}
	
	public ArgumentSweep(int argNo, JSONObject json){
		this.argNo=argNo;
		sweep(json);
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

}
