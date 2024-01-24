package eu.unicore.xnjs.resources;

public class StringResource extends Resource{
	
	private String value;
	
	public StringResource(String name, String value, Category category){
		super(name,category);
		this.value=value;
	}
	
	public StringResource(String name, String value){
		this(name,value,null);
	}

	@Override
	public String getValue() {
		return value;
	}

	@Override
	public void setSelectedValue(String val) {
		value = val;
	}
	
	@Override
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
