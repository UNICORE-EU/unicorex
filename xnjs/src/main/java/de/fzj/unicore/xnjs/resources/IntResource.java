package de.fzj.unicore.xnjs.resources;

public class IntResource extends Resource implements RangeResource {

	private Long value;

	private final Long upper;

	private final Long lower;

	public IntResource(String name, Long value, Long upper, Long lower, Category category){
		super(name,category);
		this.value=value;
		this.upper=upper;
		this.lower=lower;
	}

	public IntResource(String name, Long value){
		this(name,value,null,null,null);
	}

	public Long getValue() {
		return value;
	}

	public void setStringValue(String v){
		value = v==null? null : Long.parseLong(v);
	}

	public boolean isInRange(Object otherValue){
		if(otherValue == null)return true;
		Long v;

		if(otherValue instanceof String){
			try{
				Double d = Double.parseDouble(String.valueOf(otherValue));
				v = Math.round(d);
			}
			catch(NumberFormatException ex){
				return false;
			}
		}
		else if(otherValue instanceof Long){
			v=(Long)otherValue;
		}
		else{
			if(!(otherValue.getClass().isAssignableFrom(IntResource.class))){
				throw new IllegalArgumentException("Can't check range, need Float value, found "+otherValue.getClass());
			}
			v=((IntResource)otherValue).getValue();
			if(v==null)return true;
		}

		if(upper!=null && upper.compareTo(v)<0){
			return false;
		}
		if(lower!=null && lower.compareTo(v)>0){
			return false;
		}
		return true;
	}

	@Override
	public Long getUpper() {
		return upper;
	}

	@Override
	public Long getLower() {
		return lower;
	}

	public String toString(){
		StringBuilder sb=new StringBuilder();
		sb.append(name).append("[int, category=").append(category).append("] ").append(value);
		sb.append(" lower=").append(lower);
		sb.append(" upper=").append(upper);
		return sb.toString();
	}
}
