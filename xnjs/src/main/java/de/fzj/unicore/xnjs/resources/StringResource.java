package de.fzj.unicore.xnjs.resources;

public class StringResource extends BaseResource{
	
	private static final long serialVersionUID=1L;
	
	private String value;
	
	public StringResource(String name, String value, Category category){
		super(name,category);
		this.value=value;
	}
	
	public StringResource(String name, String value){
		this(name,value,null);
	}
	
	public Resource copy() {
		return new StringResource(this.name, this.value, this.category);
	}

	public String getValue() {
		return value;
	}
	
	public void setStringValue(String val) {
		value=val;
	}
	
	public boolean isInRange(Object otherValue){
		if( ! (otherValue.getClass().isAssignableFrom(StringResource.class)
			|| otherValue instanceof String) )
		{
			throw new IllegalArgumentException("Need StringResource, found "+otherValue.getClass());
		}
		return true;
	}

	public String toString(){
		StringBuilder sb=new StringBuilder();
		sb.append(name).append("[string, category=").append(category).append("] ").append(value);
		return sb.toString();
	}
}
