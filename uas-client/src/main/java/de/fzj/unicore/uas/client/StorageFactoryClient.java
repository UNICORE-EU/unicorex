package de.fzj.unicore.uas.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.oasisOpen.docs.wsrf.rl2.TerminationTimeDocument.TerminationTime;
import org.unigrids.services.atomic.types.PropertyType;
import org.unigrids.x2006.x04.services.smf.AccessibleStorageEnumerationDocument;
import org.unigrids.x2006.x04.services.smf.AccessibleStorageReferenceDocument;
import org.unigrids.x2006.x04.services.smf.CreateSMSDocument;
import org.unigrids.x2006.x04.services.smf.CreateSMSResponseDocument;
import org.unigrids.x2006.x04.services.smf.StorageBackendParametersDocument.StorageBackendParameters;
import org.unigrids.x2006.x04.services.smf.StorageDescriptionType;
import org.unigrids.x2006.x04.services.smf.StorageFactoryPropertiesDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.StorageFactory;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * Client for the StorageFactory service
 * 
 * @author schuller
 */
public class StorageFactoryClient extends BaseClientWithStatus {

	private static final Logger logger=Log.getLogger(Log.CLIENT,StorageFactoryClient.class);

	private final StorageFactory smf;
	
	private EnumerationClient<AccessibleStorageReferenceDocument>accessibleStorageEnumeration;
	
	/**
	 * @param endpointUrl - the URL to connect to
	 * @param epr - the EPR of the target service
	 * @param sec - the security settings to use
	 * @throws Exception
	 */
	public StorageFactoryClient(String endpointUrl, EndpointReferenceType epr, IClientConfiguration sec)throws Exception {
		super(endpointUrl, epr,sec);
		smf=makeProxy(StorageFactory.class);
	}

	/**
	 * @param epr - the EPR of the target service
	 * @param sec - the security settings to use
	 * @throws Exception
	 */
	public StorageFactoryClient(EndpointReferenceType epr, IClientConfiguration sec)throws Exception {
		this(epr.getAddress().getStringValue(), epr,sec);
	}

	/**
	 * create an SMS instance as described by the given {@link CreateSMSDocument}
	 * @param in - describes the instance to be created
	 * @return a StorageClient for accessing the newly created SMS
	 * @throws Exception
	 */
	public StorageClient createSMS(CreateSMSDocument in) throws Exception {
		logger.info("Calling storage factory service at: "+getEPR().getAddress().getStringValue());
		CreateSMSResponseDocument res=smf.CreateSMS(in);
		EndpointReferenceType epr=res.getCreateSMSResponse().getSmsReference();
		return new StorageClient(epr.getAddress().getStringValue(),epr,getSecurityConfiguration());
	}

	/**
	 * create a SMS
	 *  
	 * @param type - the type of SMS to create. If <code>null</code>, the server default is used
	 * @param name - the name of the new SMS. If <code>null</code>, the server default is used
	 * @param settings - any extra settings for the new SMS. If <code>null</code>  or empty, the server default is used
	 * @param initialTerminationTime - the initial termination time. If <code>null</code>, the server default is used
	 * @return a StorageClient for accessing the newly created SMS
	 * @throws Exception
	 */
	public StorageClient createSMS(String type, String name, Map<String,String> settings, Calendar initialTerminationTime) throws Exception {
		CreateSMSDocument in=CreateSMSDocument.Factory.newInstance();
		in.addNewCreateSMS();
		if(initialTerminationTime!=null){
			TerminationTime tt=TerminationTime.Factory.newInstance();
			tt.setCalendarValue(initialTerminationTime);
			in.getCreateSMS().setTerminationTime(tt);
		}
		StorageDescriptionType sd=in.getCreateSMS().addNewStorageDescription();
		if(type!=null)sd.setStorageBackendType(type);
		if(name!=null){
			sd.addNewFileSystem().setName(name);
		}
		if(settings!=null && !settings.isEmpty()){
			StorageBackendParameters p = sd.addNewStorageBackendParameters();
			for(Map.Entry<String, String>e: settings.entrySet()){
				PropertyType pt = p.addNewProperty();
				pt.setName(e.getKey());
				pt.setValue(e.getValue());
			}
		}
		return createSMS(in);
	}
	
	public StorageClient createSMS(String type, String name, Calendar initialTerminationTime) throws Exception {
		return createSMS(type, name, null, initialTerminationTime);
	}

	public StorageClient createSMS(Calendar initialTerminationTime) throws Exception {
		return createSMS(null,null,initialTerminationTime);
	}
	
	/**
	 * create a new SMS with server default settings
	 */
	public StorageClient createSMS() throws Exception {
		return createSMS(null,null,null,null);
	}

	/**
	 * returns the service's resource properties doc
	 */
	public StorageFactoryPropertiesDocument getResourcePropertiesDocument()throws Exception{
		return StorageFactoryPropertiesDocument.Factory.parse(GetResourcePropertyDocument().getGetResourcePropertyDocumentResponse().newInputStream());
	}

	/**
	 * gets the SMSSs that are accessible for this client
	 * 
	 * @return accessible SMS addresses
	 * @deprecated use getStorages()
	 */
	public List<EndpointReferenceType> getAccessibleStorages()throws Exception{
		return getStorages();
	}

	/**
	 * gets the SMSSs that are accessible for this client
	 * 
	 * @return accessible SMS addresses
	 */
	public List<EndpointReferenceType> getStorages()throws Exception{
		EnumerationClient<AccessibleStorageReferenceDocument>c=getAccessibleSMSEnumeration();
		if(c==null)return getAccessibleSMSWithoutEnumeration();
		c.setUpdateInterval(-1);
		List<EndpointReferenceType>res=new ArrayList<EndpointReferenceType>();
		Iterator<AccessibleStorageReferenceDocument>iter=c.iterator();
		while(iter.hasNext()){
			res.add(iter.next().getAccessibleStorageReference());
		}
		return res;
	}

	private synchronized EnumerationClient<AccessibleStorageReferenceDocument>getAccessibleSMSEnumeration()throws Exception{
		if(accessibleStorageEnumeration==null){
			AccessibleStorageEnumerationDocument enumRef=getSingleResourceProperty(AccessibleStorageEnumerationDocument.class);
			if(enumRef!=null){
				EndpointReferenceType epr=enumRef.getAccessibleStorageEnumeration();
				accessibleStorageEnumeration=new EnumerationClient<AccessibleStorageReferenceDocument>(epr,
						getSecurityConfiguration(),
						AccessibleStorageReferenceDocument.type.getDocumentElementName());
			}
		}
		return accessibleStorageEnumeration;
	}
	
	private List<EndpointReferenceType> getAccessibleSMSWithoutEnumeration(){
		try{
			EndpointReferenceType[] eprs=getResourcePropertiesDocument().getStorageFactoryProperties().
			getAccessibleStorageReferenceArray();
			return Arrays.asList(eprs);
		}catch(Exception e){
			logger.error("Can't get accessible storage list.",e);
		}
		return null;
	}
	
	public StorageDescriptionType[] getBackendStorageDescription()throws Exception{
		return getResourcePropertiesDocument().getStorageFactoryProperties().getStorageDescriptionArray();
	}
	
}
