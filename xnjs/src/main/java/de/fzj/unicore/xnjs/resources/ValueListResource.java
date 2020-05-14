package de.fzj.unicore.xnjs.resources;

import java.util.ArrayList;
import java.util.List;

/**
 * a {@link Resource} whose valid values form a (finite) list
 * 
 * @author schuller
 */
public class ValueListResource extends BaseResource{

	private static final long serialVersionUID=1L;

	protected final List<String> validValues=new ArrayList<String>();

	protected String selectedValue;

	public ValueListResource(String name, String val, List<String>allowed,Category category){
		super(name,category);
		validValues.addAll(allowed);
		this.selectedValue=val;
	}

	public void setSelectedValue(String val){
		selectedValue=val;
	}
	
	public void setStringValue(String val){
		selectedValue=val;
	}

	public void setValidValues(List<String>values){
		validValues.clear();
		validValues.addAll(values);
	}

	public String[] getValidValues(){
		return validValues.toArray(new String[validValues.size()]);
	}

	@Override
	public Object getValue() {
		return selectedValue;
	}

	@Override
	public boolean isInRange(Object otherValue) {
		String otherSelected;

		if(otherValue instanceof String){
			otherSelected=(String)otherValue;
		}
		else if(otherValue instanceof StringResource){
			otherSelected = ((StringResource)otherValue).getStringValue();
		}
		else{
			if( !(otherValue instanceof ValueListResource) )return false;
			ValueListResource otherList=(ValueListResource)otherValue;
			otherSelected=otherList.selectedValue;
		}
		return otherSelected==null || validValues.contains(otherSelected);
	}

	@Override
	public Resource copy() {
		return new ValueListResource(name,selectedValue,validValues,category);
	}

	public String toString(){
		StringBuilder sb=new StringBuilder();
		sb.append(name).append("[choice, category=").append(category).append("] ").append(selectedValue);
		sb.append(" choices=").append(validValues);
		return sb.toString();
	}
}
