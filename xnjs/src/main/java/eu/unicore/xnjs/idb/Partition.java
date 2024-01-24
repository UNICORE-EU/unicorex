package eu.unicore.xnjs.idb;

import eu.unicore.xnjs.resources.ResourceSet;

/**
 * a compute partition which has a set of resources associated, 
 * e.g. min/max number of nodes
 * 
 * @author schuller
 */
public class Partition {

	private String name;
	
	private String description;

	private final ResourceSet resources = new ResourceSet();
	
	private String operatingSystem = "LINUX";
	private String operatingSystemVersion = "";
	
	private String cpuArchitecture = "x86";
		
	private boolean isDefault = false;
	
	private String submitScriptTemplate;
	
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

	/**
	 * resources associated with this queue, e.g. number of nodes
	 */
	public ResourceSet getResources(){
		return resources;
	}

	public String getOperatingSystem() {
		return operatingSystem;
	}

	public void setOperatingSystem(String operatingSystem) {
		this.operatingSystem = operatingSystem;
	}

	public String getOperatingSystemVersion() {
		return operatingSystemVersion;
	}

	public void setOperatingSystemVersion(String operatingSystemVersion) {
		this.operatingSystemVersion = operatingSystemVersion;
	}

	public String getCPUArchitecture() {
		return cpuArchitecture;
	}

	public void setCpuArchitecture(String cpuArchitecture) {
		this.cpuArchitecture = cpuArchitecture;
	}

	public String toString(){
		return "Partition["+name+":"+description+":"+resources+"]";
	}
	
	public boolean isDefaultPartition() {
		return isDefault;
	}
	
	public void setDefaultPartition(boolean isDefault) {
		this.isDefault = isDefault;
	}

	public String getSubmitScriptTemplate() {
		return submitScriptTemplate;
	}

	public void setSubmitScriptTemplate(String submitScriptTemplate) {
		this.submitScriptTemplate = submitScriptTemplate;
	}

}
