package de.fzj.unicore.uas.impl.sms.ws;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.logging.log4j.Logger;
import org.oasisOpen.docs.wsrf.rl2.DestroyDocument;
import org.oasisOpen.docs.wsrf.rl2.DestroyResponseDocument;
import org.oasisOpen.docs.wsrf.rl2.SetTerminationTimeDocument;
import org.oasisOpen.docs.wsrf.rl2.SetTerminationTimeResponseDocument;
import org.unigrids.services.atomic.types.PropertyType;
import org.unigrids.x2006.x04.services.smf.CreateSMSDocument;
import org.unigrids.x2006.x04.services.smf.CreateSMSResponseDocument;
import org.unigrids.x2006.x04.services.smf.StorageBackendParametersDocument.StorageBackendParameters;
import org.unigrids.x2006.x04.services.smf.StorageDescriptionType;
import org.unigrids.x2006.x04.services.smf.StorageFactoryPropertiesDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.StorageFactory;
import de.fzj.unicore.uas.StorageManagement;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.impl.UASBaseFrontEnd;
import de.fzj.unicore.uas.impl.enumeration.EnumerationInitParameters;
import de.fzj.unicore.uas.impl.sms.StorageFactoryImpl;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.InitParameters.TerminationMode;
import de.fzj.unicore.wsrflite.xmlbeans.BaseFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceNotDestroyedFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnavailableFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnknownFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.TerminationTimeChangeRejectedFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.UnableToSetTerminationTimeFault;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.AddressRenderer;
import eu.unicore.services.ws.utils.WSServerUtilities;

/**
 * Implements the storage factory
 * 
 * @author schuller
 * @author daivandy
 * @since 6.3
 */
public class StorageFactoryFrontend extends UASBaseFrontEnd implements StorageFactory{

	private static final Logger logger = LogUtil.getLogger(LogUtil.DATA, StorageFactoryFrontend.class);

	private final StorageFactoryImpl resource;
	
	public StorageFactoryFrontend(StorageFactoryImpl r){
		super(r);
		this.resource = r;
		
		addRenderer(new AddressRenderer(this.resource,RPAccessibleSMSEnumeration,false) {
			@Override
			protected String getServiceSpec() {
				return UAS.ENUMERATION+"?res="+r.getModel().getAccessibleSMSEnumerationID();
			}
		});
		addRenderer(new StorageDescriptionRP(r));

		//internal use
		addRenderer(new AccessibleSMSReferenceRP(r));
	}

	public CreateSMSResponseDocument CreateSMS(CreateSMSDocument in) throws BaseFault {
		if(logger.isTraceEnabled())logger.trace("CreateSMS: "+in);
		try{
			//extra settings for the to-be-created SMS
			Map<String,String>extraProperties=new HashMap<String,String>();

			String storageBackendType = null;
			String storageName = null;

			// extract requested back-end type and parameters, if any
			StorageDescriptionType sdType = in.getCreateSMS().getStorageDescription();
			if(sdType!=null) {
				if(sdType.getStorageBackendType()!=null){
					storageBackendType = sdType.getStorageBackendType();
				}
				if(sdType.getStorageBackendParameters()!=null){
					extraProperties.putAll(getAdditionClientConfigurationItems(
							sdType.getStorageBackendParameters()));
				}
				storageName = sdType.getFileSystem()!=null ? sdType.getFileSystem().getName() : null; 
			}
			Calendar tt = null;
			if(in.getCreateSMS().getTerminationTime() != null){
				tt = in.getCreateSMS().getTerminationTime().getCalendarValue();
			}
			
			String smsID = resource.createSMS(storageBackendType,storageName,tt,extraProperties);
			EndpointReferenceType epr=WSServerUtilities.makeEPR(UAS.SMS, smsID, StorageManagement.SMS_PORT, true, kernel);
			CreateSMSResponseDocument resD=CreateSMSResponseDocument.Factory.newInstance();
			resD.addNewCreateSMSResponse().setSmsReference(epr);
			return resD;
			
		}catch(Exception ex){
			String clientName=(resource.getClient()!=null?resource.getClient().getDistinguishedName():"N/A");
			LogUtil.logException("Could not create Storage instance for <"+clientName+">", ex, logger);
			throw BaseFault.createFault("Could not create Storage instance. Please consult the site administrator.", ex);
		}
	}

	/**
	 * replaces prefix of the storage factory into a normal one of SMS
	 * @param params - parameters provided by the client
	 */
	private Map<String, String> getAdditionClientConfigurationItems(StorageBackendParameters params) {
		Map<String, String> ret = new HashMap<String, String>();
		for(PropertyType p: params.getPropertyArray()){
			ret.put(p.getName(), p.getValue());
		}
		return ret;
	}

	/**
	 * @return the UID of the new Enumeration
	 */
	protected String createEnumeration(QName rp)throws Exception{
		EnumerationInitParameters init = new EnumerationInitParameters(null, TerminationMode.NEVER);
		init.parentUUID = resource.getUniqueID();
		init.parentServiceName = resource.getServiceName();
		init.targetServiceRP = rp;
		Home h=kernel.getHome(UAS.ENUMERATION);
		if(h==null)throw new Exception("Enumeration service is not deployed!");
		return h.createResource(init);
	}

	@Override
	public QName getPortType() {
		return SMF_PORT;
	}

	@Override
	public QName getResourcePropertyDocumentQName() {
		return StorageFactoryPropertiesDocument.type.getDocumentElementName();
	}

	@Override
	public DestroyResponseDocument Destroy(DestroyDocument in) throws ResourceNotDestroyedFault, ResourceUnknownFault, ResourceUnavailableFault {
		throw ResourceNotDestroyedFault.createFault("Not destroyed."); 
	}

	@Override
	public SetTerminationTimeResponseDocument SetTerminationTime(
			SetTerminationTimeDocument in)
					throws UnableToSetTerminationTimeFault,
					TerminationTimeChangeRejectedFault, ResourceUnknownFault,
					ResourceUnavailableFault {
		throw TerminationTimeChangeRejectedFault.createFault("Not changed.");
	}

}
