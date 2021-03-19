package de.fzj.unicore.uas.fts.ws;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.unigrids.x2006.x04.services.fts.PropertyDocument;

import de.fzj.unicore.uas.fts.FileTransferImpl;
import de.fzj.unicore.wsrflite.Resource;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.ValueRenderer;

/**
 * publishes protocol-dependent parameters
 * #see {@link FileTransferImpl#getProtocolDependentParameters()}
 * 
 * @author schuller
 */
public class ParameterRenderer extends ValueRenderer {

	public ParameterRenderer(Resource parent){
		super(parent,PropertyDocument.type.getDocumentElementName());
	}

	@Override
	protected Object getValue() throws Exception {
		List<PropertyDocument>res=new ArrayList<PropertyDocument>();
		FileTransferImpl ft=(FileTransferImpl)parent;
		for(Map.Entry<String, String> e: ft.getProtocolDependentParameters().entrySet()){
			PropertyDocument pd=PropertyDocument.Factory.newInstance();
			pd.addNewProperty().setName(e.getKey());
			pd.getProperty().setValue(e.getValue());
			res.add(pd);
		}
		return res;
	}

}
