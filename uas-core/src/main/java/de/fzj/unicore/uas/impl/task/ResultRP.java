package de.fzj.unicore.uas.impl.task;

import org.apache.xmlbeans.XmlObject;

import de.fzj.unicore.wsrflite.xmlbeans.renderers.ValueRenderer;
import eu.unicore.unicore6.task.ResultDocument;

public class ResultRP extends ValueRenderer {

	public ResultRP(TaskImpl parent){
		super(parent, ResultDocument.type.getDocumentElementName());
	}

	@Override
	protected ResultDocument getValue(){
		ResultDocument d=null;
		XmlObject o=((TaskImpl)parent).getResult();
		if(o!=null){
			d=ResultDocument.Factory.newInstance();
			d.addNewResult().set(o);
		}
		return d;
	}

}
