package de.fzj.unicore.xnjs.resources;


public class BooleanResource extends BaseResource {

	private static final long serialVersionUID = 1L;
	
	private Boolean value;
	
	public BooleanResource(String name, Boolean value, Category category) {
		super(name, category);
		this.value=value;
	}

	@Override
	public Resource copy() {
		return new BooleanResource(name,value,category);
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
		sb.append(name).append("=").append(value);
		return sb.toString();
	}
	
	public void setStringValue(String value){
		this.value = value==null? null : Boolean.parseBoolean(value);
	}
	
}
