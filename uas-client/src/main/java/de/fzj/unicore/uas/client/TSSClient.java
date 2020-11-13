/*********************************************************************************
 * Copyright (c) 2006-2012 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/


package de.fzj.unicore.uas.client;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.OperatingSystemType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ResourcesDocument;
import org.unigrids.services.atomic.types.AvailableResourceType;
import org.unigrids.services.atomic.types.SiteResourceType;
import org.unigrids.services.atomic.types.StatusType;
import org.unigrids.services.atomic.types.StorageReferenceDocument;
import org.unigrids.services.atomic.types.TextInfoType;
import org.unigrids.x2006.x04.services.reservation.ResourceReservationRequestDocument;
import org.unigrids.x2006.x04.services.reservation.ResourceReservationResponseDocument;
import org.unigrids.x2006.x04.services.tss.AllocationDocument;
import org.unigrids.x2006.x04.services.tss.ApplicationResourceType;
import org.unigrids.x2006.x04.services.tss.DeleteJobsDocument;
import org.unigrids.x2006.x04.services.tss.GetJobsStatusDocument;
import org.unigrids.x2006.x04.services.tss.GetJobsStatusResponseDocument;
import org.unigrids.x2006.x04.services.tss.JobReferenceDocument;
import org.unigrids.x2006.x04.services.tss.JobStatusDocument.JobStatus;
import org.unigrids.x2006.x04.services.tss.SubmitDocument;
import org.unigrids.x2006.x04.services.tss.SubmitResponseDocument;
import org.unigrids.x2006.x04.services.tss.SupportsReservationDocument;
import org.unigrids.x2006.x04.services.tss.TargetSystemPropertiesDocument;
import org.unigrids.x2006.x04.services.tss.TargetSystemPropertiesDocument.TargetSystemProperties;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.TargetSystem;
import de.fzj.unicore.uas.TargetSystemFactory;
import de.fzj.unicore.uas.faults.AutoStartNotSupportedException;
import de.fzj.unicore.uas.lookup.AddressFilter;
import de.fzj.unicore.uas.util.StorageFilters;
import de.fzj.unicore.wsrflite.xmlbeans.client.RegistryClient;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * client for the TargetSystem Service
 *
 * @author schuller
 */
public class TSSClient extends BaseClientWithStatus {

	private static final Logger logger=Log.getLogger(Log.CLIENT,TSSClient.class);

	private final TargetSystem tss;

	private String name;

	public TSSClient(String endpointUrl, EndpointReferenceType epr, IClientConfiguration sec)throws Exception {
		super(endpointUrl, epr,sec);
		tss=makeProxy(TargetSystem.class);
	}

	public TSSClient(EndpointReferenceType epr, IClientConfiguration sec)throws Exception {
		this(epr.getAddress().getStringValue(), epr,sec);
	}

	/**
	 * submit a job
	 */
	public SubmitResponseDocument Submit(SubmitDocument in) throws Exception, AutoStartNotSupportedException{
		if(logger.isDebugEnabled())logger.debug("Calling target system service at "+getEPR().getAddress().getStringValue());
		boolean autoStart=in.getSubmit().getAutoStartWhenReady();
		if(autoStart && !checkVersion("1.4.1")){
			throw new AutoStartNotSupportedException();
		}
		return tss.Submit(in);
	}

	/**
	 * Submit a job, and return a JMS client
	 * @param in
	 * @throws Exception
	 */
	public JobClient submit(SubmitDocument in) throws Exception {
		SubmitResponseDocument res=Submit(in);
		EndpointReferenceType epr=res.getSubmitResponse().getJobReference();
		return new JobClient(epr.getAddress().getStringValue(), epr, getSecurityConfiguration());
	}


	/**
	 * returns the service's TargetSystemPropertiesDocument
	 */
	public TargetSystemPropertiesDocument getResourcePropertiesDocument()throws Exception{
		return TargetSystemPropertiesDocument.Factory.parse(GetResourcePropertyDocument().getGetResourcePropertyDocumentResponse().newInputStream());
	}

	/**
	 * return the service's uptime
	 */
	public Calendar getUpSince()throws Exception{
		return getResourcePropertiesDocument().getTargetSystemProperties().getUpSince();
	}

	/**
	 * return the number of jobs on the resource
	 */
	public long getNumberOfJobs()throws Exception{
		return getResourcePropertiesDocument().getTargetSystemProperties().getTotalNumberOfJobs().longValue();
	}

	/**
	 * get the current list of jobs on this target system
	 */
	private List<EndpointReferenceType> getJobsWithoutEnumeration()throws Exception{
		EndpointReferenceType[] eprs=getResourcePropertiesDocument().getTargetSystemProperties().getJobReferenceArray();
		return Arrays.asList(eprs);
	}

	/**
	 * returns the list of job references.br/>
	 * To handle very large lists, you should use {@link #getJobReferenceEnumeration()}
	 * instead. 
	 * Note that that method returns null in case you're accessing an
	 * older server.
	 * @throws Exception
	 */
	public List<EndpointReferenceType> getJobs()throws Exception{
		EnumerationClient<JobReferenceDocument>c=getJobReferenceEnumeration();
		if(c==null)return getJobsWithoutEnumeration();
		List<EndpointReferenceType>res=new ArrayList<EndpointReferenceType>();
		Iterator<JobReferenceDocument>iter=c.iterator();
		while(iter.hasNext()){
			res.add(iter.next().getJobReference());
		}
		return res;
	}

	/**
	 * @since 6.3.0
	 * @return an {@link EnumerationClient} for accessing the job list, or null in case of
	 * an older server that does not support enumerations
	 */
	public EnumerationClient<JobReferenceDocument>getJobReferenceEnumeration()throws Exception{
		EndpointReferenceType epr=getResourcePropertiesDocument().getTargetSystemProperties().getJobReferenceEnumeration();
		if(epr!=null){
			EnumerationClient<JobReferenceDocument>c=new EnumerationClient<JobReferenceDocument>(epr, 
					getSecurityConfiguration(), JobReferenceDocument.type.getDocumentElementName());
			c.setUpdateInterval(-1);
			return c;
		}
		return null;
	}
	/**
	 * get the current list of reservations
	 * @return List of job eprs
	 */
	public List<EndpointReferenceType> getReservations() throws Exception {
		EndpointReferenceType[] eprs=getResourcePropertiesDocument().getTargetSystemProperties().getReservationReferenceArray();
		return Arrays.asList(eprs);
	}

	/**
	 * get the current list of storages attached to this target system
	 * @return List of storage eprs
	 */
	public List<EndpointReferenceType> getStorages() throws Exception{
		List<StorageReferenceDocument>smsRefs=
				getResourceProperty(StorageReferenceDocument.class);
		List<EndpointReferenceType>res=new ArrayList<EndpointReferenceType>();
		for(StorageReferenceDocument x: smsRefs){
			res.add(x.getStorageReference().getStorageEndpointReference());
		}
		return res;
	}

	/**
	 * get the storage attached to this target system which matches the filter
	 * @return StorageClient matching the supplied filter, or null if none found
	 */
	public StorageClient getStorage(AddressFilter<StorageClient>filter) throws Exception {
		List<StorageReferenceDocument>smsRefs=getResourceProperty(StorageReferenceDocument.class);
		for(StorageReferenceDocument x: smsRefs){
			EndpointReferenceType epr=x.getStorageReference().getStorageEndpointReference();
			if(filter.accept(epr)){
				StorageClient c=new StorageClient(epr,getSecurityConfiguration());
				if(filter.accept(c)){
					return c;
				}
			}
		}
		return null;
	}

	/**
	 * get the named storage attached to this target system 
	 * @return StorageClient matching the supplied storagename, or null if none found
	 */
	public StorageClient getStorage(String name) throws Exception {
		return getStorage(new StorageFilters.ByName(name));
	}
	

	/**
	 * get the "Name" of this target system
	 */
	public String getTargetSystemName() throws Exception {
		if(name==null){
			name=getResourcePropertiesDocument().getTargetSystemProperties().getName();
		}
		return name;
	}

	/**
	 * get the list of applications installed on the targetsystem
	 */
	public List<ApplicationResourceType> getApplications() throws Exception {
		ApplicationResourceType[] apps=getResourcePropertiesDocument().getTargetSystemProperties().getApplicationResourceArray();
		return Arrays.asList(apps);
	}


	/**
	 * get the list of TextInfo for this TSS
	 * @return List of TextInfo
	 */
	public List<TextInfoType> getTextInfo() throws Exception {
		TextInfoType[] info=getResourcePropertiesDocument().getTargetSystemProperties().getTextInfoArray();
		return Arrays.asList(info);
	}

	/**
	 * Returns site specific resources --- new servers will rather use AvailableResources 
	 * @return the list of site specific resources
	 */
	public List<SiteResourceType> getSiteSpecificResources() throws Exception {
		SiteResourceType[] info = getResourcePropertiesDocument().getTargetSystemProperties().getSiteResourceArray();
		return Arrays.asList(info);
	}

	/**
	 * returns (non-jsdl) resources that are available on the tss 
	 */
	public List<AvailableResourceType> getAvailableResources() throws Exception {
		AvailableResourceType[] info = getResourcePropertiesDocument().getTargetSystemProperties().getAvailableResourceArray();
		return Arrays.asList(info);
	}

	public String getOperatingSystemInfo() throws Exception {
		OperatingSystemType os=getResourcePropertiesDocument().getTargetSystemProperties().getOperatingSystem();
		if(os==null)return "UNKNOWN";
		try{
			String name=os.getOperatingSystemType().getOperatingSystemName().toString();
			String version=os.getOperatingSystemVersion();
			String descrString=os.getDescription();
			return name+" "+ ( version!=null? version: "") + (descrString!=null?" ("+descrString+")": "");
		}catch(Exception e){
			logger.warn("Error retrieving OS from target system.");
			return "UNKNOWN";
		}
	}

	public OperatingSystemType getOperatingSystem() throws Exception {
		OperatingSystemType os=getResourcePropertiesDocument().getTargetSystemProperties().getOperatingSystem();
		return os;
	}

	/**
	 * get the remaining compute time (typically given in core hours)
	 */
	public List<TSFClient.Allocation> getComputeTimeBudget() throws Exception {
		List<TSFClient.Allocation> budget = new ArrayList<>();
		TargetSystemProperties p = getResourcePropertiesDocument().getTargetSystemProperties();
		if(p.isSetComputeTimeBudget()){
			for(AllocationDocument.Allocation alloc: p.getComputeTimeBudget().getAllocationArray()) {
				try{
					budget.add(new TSFClient.Allocation(alloc.getName(), alloc.getRemaining().longValue(),
							alloc.getPercentRemaining().intValue(), alloc.getUnits()));
				}catch(Exception ex) {
					Log.logException("Invalid allocation: "+alloc, ex, logger);
				}
			}
		}
		return budget;
	}

	/**
	 * helper for retrieving a TSSClient from a given registry 
	 * @param registryURL
	 * @param sec
	 * @return TSSClient
	 */
	public static TSSClient getOrCreateTSS(String registryURL, IClientConfiguration sec) throws Exception {
		EndpointReferenceType epr=EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue(registryURL);
		RegistryClient c=new RegistryClient(registryURL,epr,sec);
		List<EndpointReferenceType> tss=c.listAccessibleServices(TargetSystem.TSS_PORT);
		if(tss.size()==0){
			return createTSS(c,sec);
		}
		else if(tss.get(0).getAddress().getStringValue().contains("WorkflowTargetSystem")){
			if(tss.size()>0){
				return new TSSClient(tss.get(1).getAddress().getStringValue(),tss.get(1),sec);
			}
			else return createTSS(c,sec);
		}
		else
		{
			return new TSSClient(tss.get(0).getAddress().getStringValue(),tss.get(0),sec);
		}
	} 

	private static TSSClient createTSS(RegistryClient c,IClientConfiguration sec) throws Exception {
		List<EndpointReferenceType> tsfs=c.listAccessibleServices(TargetSystemFactory.TSF_PORT);
		if(tsfs.size()==0)return null;
		return new TSFClient(tsfs.get(0).getAddress().getStringValue(),tsfs.get(0),sec).createTSS();
	}

	/**
	 * does this target system support reservation?
	 * @throws Exception
	 */
	public boolean supportsReservation()throws Exception{
		SupportsReservationDocument resSupport = getSingleResourceProperty(SupportsReservationDocument.class);
		return resSupport.getSupportsReservation();
	}

	/**
	 * Create a reservation
	 * 
	 * @return {@link EndpointReferenceType} of the new reservation
	 * @throws Exception
	 */
	public EndpointReferenceType createReservation(ResourcesDocument resources, Calendar startTime)throws Exception{
		ResourceReservationRequestDocument req=ResourceReservationRequestDocument.Factory.newInstance();
		req.addNewResourceReservationRequest().setResources(resources.getResources());
		req.getResourceReservationRequest().setStartTime(startTime);
		TargetSystem tss=makeProxy(TargetSystem.class);
		ResourceReservationResponseDocument res=tss.ReserveResources(req);
		return res.getResourceReservationResponse().getReservationReference();
	}

	/**
	 * Create a reservation and return a client for managing the reservation
	 * 
	 * @return {@link ReservationClient}
	 * @throws Exception
	 */
	public ReservationClient createReservationClient(ResourcesDocument resources, Calendar startTime)throws Exception{
		EndpointReferenceType epr=createReservation(resources, startTime);
		ReservationClient client=new ReservationClient(epr.getAddress().getStringValue(),epr, 
				getSecurityConfiguration());
		return client;
	}

	/**
	 * Delete multiple jobs - if available, the multi-argument method will be used <br/>
	 * On older servers, the jobs will be deleted one by one. You can check whether the
	 * server supports multi-delete using
	 * <code>checkVersion("1.7.0")</code>
	 * 
	 * @param jobs - the jobs to delete
	 * @throws Exception
	 * @since 1.7.0
	 */
	public void deleteJobs(Collection<String>jobs) throws Exception{
		if(checkVersion("1.7.0")){
			DeleteJobsDocument djd=DeleteJobsDocument.Factory.newInstance();
			djd.addNewDeleteJobs();
			for(String j: jobs){
				djd.getDeleteJobs().addJobID(j);
			}
			tss.DeleteJobs(djd);
		}
		else{
			for(String job: jobs){
				String url=getUrl().split("TargetSystem")[0]+"JobManagement?res="+job;
				EndpointReferenceType epr=EndpointReferenceType.Factory.newInstance();
				epr.addNewAddress().setStringValue(url);
				JobClient j=new JobClient(epr,getSecurityConfiguration());
				j.destroy();
			}
		}
	}

	/**
	 * Get status for multiple jobs - if available, the multi-argument method will be used <br/>
	 * On older servers, the jobs will be checked one by one. You can check whether the
	 * server supports multi-delete using
	 * <code>checkVersion("1.7.0")</code>
	 * 
	 * @param jobs - the jobs for which to get the status 
	 * @throws Exception
	 * @since 1.7.0
	 */
	public Map<String,StatusType.Enum>getJobsStatus(Collection<String>jobs) throws Exception{
		Map<String,StatusType.Enum>result=new HashMap<String, StatusType.Enum>();
		if(checkVersion("1.7.0")){
			GetJobsStatusDocument gjs=GetJobsStatusDocument.Factory.newInstance();
			gjs.addNewGetJobsStatus();
			for(String j: jobs){
				gjs.getGetJobsStatus().addJobID(j);
			}
			GetJobsStatusResponseDocument response = tss.GetJobsStatus(gjs);
			for(JobStatus s: response.getGetJobsStatusResponse().getJobStatusArray()){
				result.put(s.getJobID(),s.getStatus());
			}
		}
		else{
			for(String job: jobs){
				String url=getUrl().split("TargetSystem")[0]+"JobManagement?res="+job;
				EndpointReferenceType epr=EndpointReferenceType.Factory.newInstance();
				epr.addNewAddress().setStringValue(url);
				JobClient j=new JobClient(epr,getSecurityConfiguration());
				result.put(job,j.getStatus());
				j.destroy();
			}
		}
		return result;
	}
}
