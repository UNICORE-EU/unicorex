package de.fzj.unicore.uas.impl.enumeration;

import javax.xml.namespace.QName;

import de.fzj.unicore.uas.impl.UASBaseModel;

public class EnumerationModel extends UASBaseModel {

	private static final long serialVersionUID = 1L;

	private QName targetProperty;

	private String targetServiceName;
	
	private String targetServiceUUID;

	public QName getTargetProperty() {
		return targetProperty;
	}

	public void setTargetProperty(QName targetProperty) {
		this.targetProperty = targetProperty;
	}

	public String getTargetServiceName() {
		return targetServiceName;
	}

	public void setTargetServiceName(String targetServiceName) {
		this.targetServiceName = targetServiceName;
	}

	public String getTargetServiceUUID() {
		return targetServiceUUID;
	}

	public void setTargetServiceUUID(String targetServiceUUID) {
		this.targetServiceUUID = targetServiceUUID;
	}
	
}
