package de.fzj.unicore.uas.impl.enumeration;

import java.util.Calendar;

import javax.xml.namespace.QName;

import de.fzj.unicore.wsrflite.InitParameters;
import de.fzj.unicore.wsrflite.xmlbeans.XmlRenderer;

public class EnumerationInitParameters extends InitParameters {

	public EnumerationInitParameters(String uuid, Calendar terminationTime) {
		super(uuid, terminationTime);
	}

	public EnumerationInitParameters(String uuid, TerminationMode terminationMode) {
		super(uuid, terminationMode);
	}

	public QName targetServiceRP;

	// unit testing
	public XmlRenderer targetRP;
}
