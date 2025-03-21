package eu.unicore.xnjs.resources;

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
	public boolean isInRange(String otherValue) {
		return "true".equalsIgnoreCase(otherValue) || "false".equalsIgnoreCase(otherValue);
	}

	public String toString(){
		StringBuilder sb=new StringBuilder();
		sb.append(name).append("[boolean, category=").append(category).append("] ").append(value);
		return sb.toString();
	}

	// a bit more strict than Boolean.parseBoolean
	public static Boolean parse(String value) {
		if("true".equalsIgnoreCase(value))
			return Boolean.TRUE;
		else if("false".equalsIgnoreCase(value))
			return Boolean.FALSE;
		else throw new IllegalArgumentException("Resource request out of range: '"+value+"'");
	}
	
	public void setSelectedValue(String val) {
		if("true".equalsIgnoreCase(val))value = Boolean.TRUE;
		if("false".equalsIgnoreCase(val))value = Boolean.FALSE;
		throw new IllegalArgumentException("Out of range: '"+val+"'");
	}
}
