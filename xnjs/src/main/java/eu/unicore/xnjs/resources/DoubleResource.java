package eu.unicore.xnjs.resources;

public class DoubleResource extends Resource implements RangeResource {

	private Double value;

	private final Double upper;

	private final Double lower;

	public DoubleResource(String name, Double value, Double upper, Double lower, Category category){
		super(name,category);
		this.value=value;
		this.upper=upper;
		this.lower=lower;
	}

	public DoubleResource(String name, Double value){
		this(name,value,null,null,null);
	}

	public Double getValue() {
		return value;
	}
	
	public boolean isInRange(String requestedValue){
		try{
			Double v = Double.parseDouble((String)requestedValue);
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
	public Double getUpper() {
		return upper;
	}

	@Override
	public Double getLower() {
		return lower;
	}

	public void setSelectedValue(String val) {
		value = Double.parseDouble(val);
	}
	
	public String toString(){
		StringBuilder sb=new StringBuilder();
		sb.append(name).append("[double, category=").append(category).append("] ").append(value);
		sb.append(" lower=").append(lower);
		sb.append(" upper=").append(upper);
		return sb.toString();
	}
}
