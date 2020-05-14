package de.fzj.unicore.xnjs.idb;

/**
 * an option or input value to an application
 * 
 * @author schuller
 */
public class OptionDescription {

	public static enum Type {
		STRING, CHOICE, BOOLEAN, INT, DOUBLE, FILENAME, FILESET;
	}
	
	private String name;
	
	private String description;
	
	private Type type;
	
	// for type FILENAME (CHOICE), mimetypes (valid values) can be defined
	private String[] validValues;
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String[] getValidValues() {
		return validValues;
	}

	public void setValidValues(String[] validValues) {
		this.validValues = validValues;
	}

	public String toString(){
		return name+":"+description+":"+type;
	}
}
