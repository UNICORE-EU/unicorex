package de.fzj.unicore.xnjs.resources;

public class DoubleResource extends BaseResource implements RangeResource{

	private static final long serialVersionUID=1L;

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

	public Resource copy() {
		return new DoubleResource(this.name, this.value, this.upper, this.lower, this.category);
	}

	public Double getValue() {
		return value;
	}

	public void setStringValue(String v){
		value = v==null? null : Double.parseDouble(v);
	}
	
	public boolean isInRange(Object otherValue){
		Double v;

		if(otherValue instanceof String){
			try{
				v=Double.parseDouble((String)otherValue);
			}
			catch(NumberFormatException ex){
				return false;
			}
		}
		else if(otherValue instanceof Double){
			v=(Double)otherValue;
		}
		else{
			if(!(otherValue.getClass().isAssignableFrom(DoubleResource.class))){
				throw new IllegalArgumentException("Can't check range, need Double value, found "+otherValue.getClass());
			}
			v=((DoubleResource)otherValue).getValue();
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

	/**
	 * get the upper bound
	 */
	public Double getUpper() {
		return upper;
	}

	/**
	 * get the lower bound
	 */
	public Double getLower() {
		return lower;
	}

	public String toString(){
		StringBuilder sb=new StringBuilder();
		sb.append(name).append("[double, category=").append(category).append("] ").append(value);
		sb.append(" lower=").append(lower);
		sb.append(" upper=").append(upper);
		return sb.toString();
	}
}
