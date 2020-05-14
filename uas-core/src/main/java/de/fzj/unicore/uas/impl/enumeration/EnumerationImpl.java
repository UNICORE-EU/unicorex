package de.fzj.unicore.uas.impl.enumeration;

import java.util.List;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlObject;

import de.fzj.unicore.uas.Enumeration;
import de.fzj.unicore.uas.impl.UASWSResourceImpl;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.InitParameters;
import de.fzj.unicore.wsrflite.Resource;
import de.fzj.unicore.wsrflite.xmlbeans.BaseFault;
import de.fzj.unicore.wsrflite.xmlbeans.WSUtilities;
import de.fzj.unicore.wsrflite.xmlbeans.XmlRenderer;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.AddressRenderer;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.ValueRenderer;
import eu.unicore.services.ws.WSFrontEnd;
import eu.unicore.services.ws.cxf.CXFKernel;
import eu.unicore.unicore6.enumeration.EnumerationPropertiesDocument;
import eu.unicore.unicore6.enumeration.GetResultsRequestDocument;
import eu.unicore.unicore6.enumeration.GetResultsResponseDocument;
import eu.unicore.unicore6.enumeration.GetResultsResponseDocument.GetResultsResponse;
import eu.unicore.unicore6.enumeration.NumberOfResultsDocument;
import eu.unicore.unicore6.enumeration.ResultsDocument;

public class EnumerationImpl extends UASWSResourceImpl implements Enumeration {

	//for unit testing
	private XmlRenderer rp;
	static final String INIT_TARGETSERVICE_RP_OBJECT="targetProperty";

	public EnumerationImpl(){
		super();
		addRenderer(new ValueRenderer(this, NumberOfResultsDocument.type.getDocumentElementName()){
			@Override
			protected NumberOfResultsDocument getValue() throws Exception {
				NumberOfResultsDocument d=NumberOfResultsDocument.Factory.newInstance();
				d.setNumberOfResults(getTargetResourceProperty().getNumberOfElements());
				return d;
			}
		});

		addRenderer(new AddressRenderer(this,RPParentServiceReference,true){
			@Override
			protected String getServiceSpec(){
				EnumerationModel m = getModel();
				return m.getTargetServiceName()+"?res="+m.getTargetServiceUUID();
			}
		});		
	}

	public GetResultsResponseDocument GetResults(GetResultsRequestDocument in)
			throws BaseFault {
		try{
			int offset=(int)in.getGetResultsRequest().getOffset();
			int length=(int)in.getGetResultsRequest().getNumberOfResults();
			GetResultsResponseDocument resD=GetResultsResponseDocument.Factory.newInstance();
			GetResultsResponse res=resD.addNewGetResultsResponse();
			ResultsDocument d=ResultsDocument.Factory.newInstance();
			d.addNewResults();
			List<XmlObject>xmls=getTargetResourceProperty().render(offset, length);
			for(XmlObject o: xmls){
				WSUtilities.append(o, d);
			}
			res.setResults(d.getResults());
			return resD;
		}catch(Exception ex){
			String msg=LogUtil.createFaultMessage("Error creating result list", ex);
			throw BaseFault.createFault(msg, ex);
		}
	}

	@Override 
	public EnumerationModel getModel(){
		return (EnumerationModel)super.getModel();
	}

	@Override
	public void initialise(InitParameters initParams)
			throws Exception {
		EnumerationModel m = getModel();
		if(m==null){
			m = new EnumerationModel();
			setModel(m);
		}
		super.initialise(initParams);
		
		EnumerationInitParameters init = (EnumerationInitParameters)initParams;
		m.setTargetProperty(init.targetServiceRP);
		m.setTargetServiceName(init.parentServiceName);
		m.setTargetServiceUUID(init.parentUUID);
		
		//for unittesting
		this.rp=init.targetRP;
	}


	protected XmlRenderer getTargetResourceProperty()throws Exception{
		if(rp!=null)return rp;
		checkAndUpdateParent();
		EnumerationModel m = getModel();
		Home h=kernel.getHome(m.getTargetServiceName());
		Resource resource=(Resource)h.get(m.getTargetServiceUUID());
		WSFrontEnd frontEnd = CXFKernel.createFrontEnd(resource);
		return frontEnd.getRenderer(m.getTargetProperty());
	}

	protected void checkAndUpdateParent() throws Exception {
		EnumerationModel m = getModel();
		if(kernel.getMessaging().hasMessages(m.getTargetServiceUUID())){
			Home h = kernel.getHome(m.getTargetServiceName());
			h.refresh(m.getTargetServiceUUID());
		}
	}

	@Override
	public QName getResourcePropertyDocumentQName() {
		return EnumerationPropertiesDocument.type.getDocumentElementName();
	}

}
