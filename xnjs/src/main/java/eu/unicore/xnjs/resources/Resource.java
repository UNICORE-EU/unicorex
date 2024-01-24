package eu.unicore.xnjs.resources;

public abstract class Resource {

	public static enum Category {
		PROCESSING, 
		MEMORY, 
		TIME,
		STORAGE, 
		BANDWIDTH, 
		NETWORK_TOPOLOGY, 
		QOS_ATTRIBUTE,
		RESERVATION,
		QUEUE,
		RANGE_VALUE,
		OTHER
	}
	
	
	protected Category category = Category.OTHER;
	
	protected String name;
	
	protected String description;
	
	public Resource(String name, Category category) {
		this.name=name;
		this.category=category;
	}
	
	public abstract Object getValue();
	
	/**
	 * check if the given value is within the validity range of this resource
	 * @param otherValue
	 */
	public abstract boolean isInRange(Object otherValue);
	
	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category=category;
	}
	
	public String getName() {
		return name;
	}

	public String getStringValue() {
		return getValue()!=null?String.valueOf(getValue()):null;
	}

	public Double getDoubleValue(){
		return getStringValue()!=null?Double.valueOf(getStringValue()):null;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public abstract void setSelectedValue(String value);
}
