package de.fzj.unicore.uas.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.xmlbeans.XmlString;
import org.ogf.schemas.jsdl.x2009.x03.sweep.AssignmentDocument;
import org.ogf.schemas.jsdl.x2009.x03.sweep.DocumentNodeDocument;
import org.ogf.schemas.jsdl.x2009.x03.sweep.NamespaceBindingDocument.NamespaceBinding;
import org.ogf.schemas.jsdl.x2009.x03.sweep.functions.ValuesDocument;

import de.fzj.unicore.wsrflite.xmlbeans.WSUtilities;

/**
 * Helper for dealing with JSDL staging sweep
 *
 * @author schuller
 */
public class StagingSweep implements DocumentSweep{

	private final int number;

	private final boolean isExport;
	
	List<String>files=new ArrayList<String>();
	
	public StagingSweep(int number, boolean isExport){
		this.number=number;
		this.isExport=isExport;
	}

	public List<String>getFiles(){
		return files;
	}

	public void setFiles(String[]files){
		this.files.addAll(Arrays.asList(files));
	}
	
	public AssignmentDocument render(){
		AssignmentDocument res=AssignmentDocument.Factory.newInstance();
		res.addNewAssignment();
		DocumentNodeDocument dn=DocumentNodeDocument.Factory.newInstance();
		NamespaceBinding nb=dn.addNewDocumentNode().addNewNamespaceBinding();
		nb.setNs("http://schemas.ggf.org/jsdl/2005/11/jsdl");
		nb.setPrefix("jsdl");
		String what = isExport? "Target" : "Source";
		dn.getDocumentNode().setMatch("//jsdl:"+what+"/jsdl:URI["+number+"]");
		ValuesDocument vd=ValuesDocument.Factory.newInstance();
		vd.addNewValues();
		for(String v: files){
			XmlString xs=XmlString.Factory.newInstance();
			xs.setStringValue(v);
			vd.getValues().addNewValue().set(xs);
		}
		WSUtilities.append(dn, res);
		WSUtilities.append(vd, res);
		return res;
	}
	
}
