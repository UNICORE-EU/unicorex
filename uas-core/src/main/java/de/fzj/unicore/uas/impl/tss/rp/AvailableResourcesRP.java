package de.fzj.unicore.uas.impl.tss.rp;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.unigrids.services.atomic.types.AvailableResourceDocument;
import org.unigrids.services.atomic.types.AvailableResourceType;
import org.unigrids.services.atomic.types.AvailableResourceTypeType;

import de.fzj.unicore.uas.xnjs.XNJSFacade;
import de.fzj.unicore.uas.xnjs.XNJSResource;
import de.fzj.unicore.xnjs.idb.IDB;
import de.fzj.unicore.xnjs.jsdl.JSDLResourceSet;
import de.fzj.unicore.xnjs.resources.BooleanResource;
import de.fzj.unicore.xnjs.resources.DoubleResource;
import de.fzj.unicore.xnjs.resources.IntResource;
import de.fzj.unicore.xnjs.resources.Resource;
import de.fzj.unicore.xnjs.resources.Resource.Category;
import de.fzj.unicore.xnjs.resources.ResourceSet;
import de.fzj.unicore.xnjs.resources.StringResource;
import de.fzj.unicore.xnjs.resources.ValueListResource;
import eu.unicore.security.Client;
import eu.unicore.services.Kernel;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.services.ws.renderers.ValueRenderer;
import eu.unicore.util.Log;

public class AvailableResourcesRP extends ValueRenderer {

	private static final Logger logger=Log.getLogger(Log.SERVICES, AvailableResourcesRP.class);
	
	private XNJSResource res;
	
	public AvailableResourcesRP(XNJSResource parent) {
		super(parent, AvailableResourceDocument.type.getDocumentElementName());
		this.res = parent;
	}

	/**
	 * updates information from NJS
	 */
	@Override
	protected AvailableResourceDocument[] getValue(){
		String xnjsReference = res.getXNJSReference();
		Kernel kernel=parent.getKernel();
		IDB idb=XNJSFacade.get(xnjsReference, kernel).getIDB();
		List<AvailableResourceDocument>result=new ArrayList<AvailableResourceDocument>();
		try {
			ResourceSet rs=idb.getDefaultPartition().getResources();

			for(Resource r: rs.getResources()){
				String name=r.getName();
				if(isJSDLResource(name))continue;
				//skip range-value things
				if(r.getCategory()==Category.RANGE_VALUE)continue;
				try{
					AvailableResourceDocument srd=convert(r);
					result.add(srd);
				}catch(Exception e){
					logger.error("Could not process resource : "+r,e);
				}
			}
		}catch(Exception e){
			logger.error("Could not process resources",e);
		}
		return (AvailableResourceDocument[]) result.toArray(new AvailableResourceDocument[result.size()]);
	}

	private boolean isJSDLResource(String name){
		return JSDLResourceSet.isJSDLResourceName(name);
	}

	private AvailableResourceDocument convert(Resource r){
		AvailableResourceDocument srd=AvailableResourceDocument.Factory.newInstance();
		AvailableResourceType sr=srd.addNewAvailableResource();
		String name=r.getName();
		sr.setName(name);
		AvailableResourceTypeType.Enum type=AvailableResourceTypeType.INT;
		if(r instanceof BooleanResource){
			type=AvailableResourceTypeType.BOOLEAN;
		}
		else if(r instanceof ValueListResource){
			type=AvailableResourceTypeType.CHOICE;
			ValueListResource vlr=(ValueListResource)r;
			String[] values = vlr.getValidValues();
			if(ResourceSet.QUEUE.equals(name) && haveValidQueues()){
				values = filter(values);
			}
			sr.setAllowedValueArray(values);
		}
		else if(r instanceof StringResource){
			type=AvailableResourceTypeType.STRING;
		}
		else{ // numerical
			if(r instanceof DoubleResource){
				type=AvailableResourceTypeType.DOUBLE;
				DoubleResource dr=(DoubleResource)r;
				sr.setMin(String.valueOf(dr.getLower()));
				sr.setMax(String.valueOf(dr.getUpper()));
			}
			else if(r instanceof IntResource){
				type=AvailableResourceTypeType.INT;
				IntResource dr=(IntResource)r;
				sr.setMin(String.valueOf(dr.getLower()));
				sr.setMax(String.valueOf(dr.getUpper()));
			}
		}
		sr.setType(type);
		sr.setDescription(r.getDescription());
		if(r.getStringValue()!=null){
			sr.setDefault(r.getStringValue());
		}
		return srd;
	}
	

	protected boolean haveValidQueues(){
		Client client = AuthZAttributeStore.getClient();
		return client!=null && 
				client.getQueue()!=null && 
				client.getQueue().getValidQueues()!=null && 
				client.getQueue().getValidQueues().length>0; 
	}

	protected String[] filter(String[]queues){
		Client client = AuthZAttributeStore.getClient();
		String[] valid = client.getQueue().getValidQueues(); 
		List<String>result = new ArrayList<>();
		for(String q: queues){
			for(String v: valid){
				if(v.equals(q))result.add(q);
			}
		}
		return result.toArray(new String[result.size()]);
	}

}
