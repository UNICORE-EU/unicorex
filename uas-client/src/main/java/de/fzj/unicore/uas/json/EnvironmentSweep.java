package de.fzj.unicore.uas.json;

import org.apache.xmlbeans.XmlString;
import org.json.JSONObject;
import org.ogf.schemas.jsdl.x2009.x03.sweep.AssignmentDocument;
import org.ogf.schemas.jsdl.x2009.x03.sweep.DocumentNodeDocument;
import org.ogf.schemas.jsdl.x2009.x03.sweep.NamespaceBindingDocument.NamespaceBinding;
import org.ogf.schemas.jsdl.x2009.x03.sweep.functions.ValuesDocument;

import eu.unicore.services.ws.WSUtilities;

/**
 * Helper for dealing with JSDL Environment sweep
 *
 * @author schuller
 */
public class EnvironmentSweep extends ArgumentSweep {
	
	public EnvironmentSweep(int argNo){
		super(argNo);
	}
	
	public EnvironmentSweep(int argNo, JSONObject json){
		super(argNo,json);
	}
	
	public AssignmentDocument render(){
		AssignmentDocument res=AssignmentDocument.Factory.newInstance();
		res.addNewAssignment();
		DocumentNodeDocument dn=DocumentNodeDocument.Factory.newInstance();
		NamespaceBinding nb=dn.addNewDocumentNode().addNewNamespaceBinding();
		nb.setNs("http://schemas.ggf.org/jsdl/2005/11/jsdl-posix");
		nb.setPrefix("jsdl-posix");
		dn.getDocumentNode().setMatch("//jsdl-posix:Environment["+argNo+"]");
		ValuesDocument vd=ValuesDocument.Factory.newInstance();
		vd.addNewValues();
		for(String v: values){
			XmlString xs=XmlString.Factory.newInstance();
			xs.setStringValue(v);
			vd.getValues().addNewValue().set(xs);
		}
		WSUtilities.append(dn, res);
		WSUtilities.append(vd, res);
		return res;
	}
}
