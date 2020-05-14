package de.fzj.unicore.xnjs.resources;

public abstract class BaseResource implements Resource {

	private static final long serialVersionUID = 1L;

	protected Category category;
	
	protected String name;
	
	protected String description;
	
	public BaseResource(String name, Category category) {
		this.name=name;
		this.category=category;
	}
	
	@Override
	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category=category;
	}
	
	@Override
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

}
