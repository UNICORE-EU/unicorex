package eu.unicore.xnjs.resources;

import java.util.ArrayList;
import java.util.List;

/**
 * a {@link Resource} with a list of valid values
 * 
 * @author schuller
 */
public class ValueListResource extends Resource {

	protected final List<String> validValues = new ArrayList<>();

	protected String selectedValue;

	protected boolean allowWildcards = true;

	public ValueListResource(String name, String val, List<String>allowed, Category category){
		super(name,category);
		validValues.addAll(allowed);
		this.selectedValue=val;
	}

	public void setAllowWildcards(boolean allowWildcards){
		this.allowWildcards = allowWildcards;
	}

	public void setSelectedValue(String val){
		selectedValue=val;
	}

	public String[] getValidValues(){
		return validValues.toArray(new String[validValues.size()]);
	}

	@Override
	public Object getValue() {
		return selectedValue;
	}

	@Override
	public boolean isInRange(String requestedValue) {
			if(allowWildcards) {
			return (validValues.contains(requestedValue) || validValues.contains("*")) 
					&& !requestedValue.contains("*");
		}
		else {
			return validValues.contains(requestedValue);
		}
	}

	public String toString(){
		StringBuilder sb=new StringBuilder();
		sb.append(name).append("[choice, category=").append(category).append("] ").append(selectedValue);
		sb.append(" choices=").append(validValues);
		return sb.toString();
	}
}
