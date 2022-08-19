package de.fzj.unicore.xnjs.resources;


public class BooleanResource extends Resource {

	private Boolean value;
	
	public BooleanResource(String name, Boolean value, Category category) {
		super(name, category);
		this.value=value;
	}

	@Override
	public Boolean getValue() {
		return value;
	}

	@Override
	public boolean isInRange(Object otherValue) {
		if(otherValue instanceof String){
			String v=(String)otherValue;
			return "true".equalsIgnoreCase(v) || "false".equalsIgnoreCase(v);
		}
		else {
			return otherValue instanceof BooleanResource || otherValue instanceof Boolean;
		}
	}

	public String toString(){
		StringBuilder sb=new StringBuilder();
		sb.append(name).append("[boolean, category=").append(category).append("] ").append(value);
		return sb.toString();
	}
	
}
