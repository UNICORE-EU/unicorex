package de.fzj.unicore.xnjs.resources;

import java.io.Serializable;

public interface Resource extends Serializable {

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
	
	public String getStringValue();
	
	/**
	 * set the selected value for this resource as a String
	 */
	public void setStringValue(String value);
	
	public Double getDoubleValue();

	public String getName();
	
	public String getDescription();
	
	public void setDescription(String description);
	
	public Object getValue();
	
	/**
	 * make a deep copy of this resource
	 */
	public Resource copy();
	
	public Category getCategory();
	
	public void setCategory(Category category);
	
	/**
	 * check if the given value is within the validity range of this resource
	 * @param otherValue
	 */
	public boolean isInRange(Object otherValue);

}
