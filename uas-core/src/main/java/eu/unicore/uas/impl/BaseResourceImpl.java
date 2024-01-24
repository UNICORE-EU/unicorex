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
	
	public void setKernel(Kernel kernel){
		super.setKernel(kernel);
		uasProperties = kernel.getAttribute(UASProperties.class);
	}

	@Override
	public final void activate() {
		super.activate();
		customPostActivate();
	}

	/**
	 * add special post-activation behaviour by overriding this method 
	 */
	protected void customPostActivate(){}

	/**
	 * sets XNJS reference, setups WSRF base profile RPs and the server's version RP
	 */
	@Override
	public void initialise(InitParameters initParams)throws Exception{
		UASBaseModel m = getModel();
		if(m==null){
			m = new UASBaseModel();
			setModel(m);
		}
		super.initialise(initParams);
		uasProperties = kernel.getAttribute(UASProperties.class);
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

	
	public void refreshSystemInfo(){
		if(getModel().getLastSystemInfoRefreshInstant()+30000 
				< System.currentTimeMillis()){
			return;
		}
		getModel().setLastSystemInfoRefreshInstant(System.currentTimeMillis());
		try{
			doRefreshSystemInfo();
		}catch(Exception ex){
			Log.logException("Error getting info from TSI", ex, logger);
		}
	}
	
	/**
	 * perform any updates of system-level info, invoked from
	 * refreshSystemInfo() if necessary
	 */
	protected void doRefreshSystemInfo() throws Exception {}

	public UASProperties getProperties() {
		return uasProperties;
	}
}
