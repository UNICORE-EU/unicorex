package eu.unicore.uas.impl;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.InitParameters;
import eu.unicore.services.Kernel;
import eu.unicore.services.impl.ResourceImpl;
import eu.unicore.uas.UASProperties;
import eu.unicore.uas.xnjs.XNJSFacade;
import eu.unicore.uas.xnjs.XNJSResource;
import eu.unicore.util.Log;

/**
 * @author schuller
 */
public abstract class BaseResourceImpl extends ResourceImpl implements XNJSResource {
	
	protected static final Logger logger=Log.getLogger(Log.SERVICES,BaseResourceImpl.class);
	
	protected UASProperties uasProperties;
	
	public BaseResourceImpl(){
		super();
	}
	
	@Override
	public UASBaseModel getModel(){
		return (UASBaseModel)super.getModel(); 
	}

	@Override
	public void setKernel(Kernel kernel){
		super.setKernel(kernel);
		uasProperties = kernel.getAttribute(UASProperties.class);
	}

	@Override
	public void initialise(InitParameters initParams)throws Exception{
		UASBaseModel m = getModel();
		if(m==null){
			m = new UASBaseModel();
			setModel(m);
		}
		super.initialise(initParams);
		if(initParams instanceof BaseInitParameters){
			m.setXnjsReference(((BaseInitParameters)initParams).xnjsReference);
		}
	}

	private String xnjsReference;

	public synchronized String getXNJSReference(){
		if(xnjsReference==null){
			xnjsReference = getModel().getXnjsReference();
		}
		return xnjsReference;
	}

	private XNJSFacade xnjs;

	public synchronized XNJSFacade getXNJSFacade(){
		if(xnjs==null){
			xnjs = XNJSFacade.get(getXNJSReference(),kernel);
		}
		return xnjs;
	}

	public UASProperties getProperties() {
		return uasProperties;
	}
}
