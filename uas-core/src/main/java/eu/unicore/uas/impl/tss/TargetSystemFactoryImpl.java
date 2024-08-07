package eu.unicore.uas.impl.tss;

import java.util.Calendar;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.InitParameters;
import eu.unicore.services.InitParameters.TerminationMode;
import eu.unicore.services.messaging.Message;
import eu.unicore.services.messaging.PullPoint;
import eu.unicore.services.messaging.ResourceDeletedMessage;
import eu.unicore.uas.UAS;
import eu.unicore.uas.UASProperties;
import eu.unicore.uas.impl.BaseInitParameters;
import eu.unicore.uas.impl.BaseResourceImpl;
import eu.unicore.uas.util.LogUtil;

/**
 * Implements the TargetSystemFactory for creating new TargetSystem instances
 *
 * @author schuller
 */
public class TargetSystemFactoryImpl extends BaseResourceImpl {

	private static final Logger logger = LogUtil.getLogger(LogUtil.JOBS, TargetSystemFactoryImpl.class);

	public TargetSystemFactoryImpl(){
		super();
	}

	@Override
	public TSFModel getModel(){
		return (TSFModel)model;
	}
	
	@Override
	public void initialise(InitParameters initArgs)throws Exception{
		if(model == null){
			setModel(new TSFModel());
		}
		TSFModel model=getModel();
		super.initialise(initArgs);
		model.supportsReservation=getXNJSFacade().supportsReservation();
	}
	
	/**
	 * create a new TSS with default settings, add the ID to the model and return the ID
	 */
	public String createTargetSystem() throws Exception {
		return createTargetSystem(null,null);
	}
	
	/**
	 * create a new TSS, add the ID to the model and return the ID
	 * @param tt - initial termination time - null for default TT
	 * @param parameters - user-specified parameters
	 */
	public String createTargetSystem(Calendar tt, Map<String,String> parameters)throws Exception{
		BaseInitParameters initObjs= tt!=null?
				new BaseInitParameters(null, tt): new BaseInitParameters(null, TerminationMode.NEVER);
		initObjs.parentUUID = getUniqueID();
		initObjs.xnjsReference = getXNJSReference();
		UASProperties props = kernel.getAttribute(UASProperties.class);
		Class<?>tssClass = props.getClassValue(UASProperties.TSS_CLASS, TargetSystemImpl.class);
		initObjs.resourceClassName = tssClass.getName();
		if(parameters!=null)initObjs.extraParameters.putAll(parameters);
		String id = kernel.getHome(UAS.TSS).createResource(initObjs);
		getModel().addChild(UAS.TSS, id);
		return id;
	}

	@Override
	public void processMessages(PullPoint p){
		while(p.hasNext()){
			Message msg = p.next();
			if(msg instanceof ResourceDeletedMessage) {
				String id = ((ResourceDeletedMessage)msg).getDeletedResource();
				logger.debug("Removing TSS with ID <{}>", id);
				getModel().removeChild(id);
			}
		}
	}

}
