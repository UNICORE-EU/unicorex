package eu.unicore.xnjs.resources;

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

	@Override
	public Long getValue() {
		return value;
	}

	@Override
	public void setSelectedValue(String val) {
		value = Double.valueOf(val).longValue();
	}

	public boolean isInRange(String requestedValue){
		try{
			Double d = Double.parseDouble(requestedValue);
			long v = Math.round(d);
			if(upper!=null && upper.compareTo(v)<0){
				return false;
			}
			if(lower!=null && lower.compareTo(v)>0){
				return false;
			}
			return true;
		}
		catch(NumberFormatException ex){
			return false;
		}
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
