/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
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

package de.fzj.unicore.uas.xnjs;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlObject;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ApplicationType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ResourcesDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ResourcesType;
import org.unigrids.services.atomic.types.PermissionsType;
import org.unigrids.services.atomic.types.TextInfoType;
import org.unigrids.x2006.x04.services.sms.FilterType;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import de.fzj.unicore.uas.UASProperties;
import de.fzj.unicore.uas.UASProperties.TSI_MODE;
import de.fzj.unicore.uas.trigger.xnjs.SharedTriggerProcessor;
import de.fzj.unicore.uas.trigger.xnjs.TriggerProcessor;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.xnjs.ConfigurationSource;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ActionStateChangeListener;
import de.fzj.unicore.xnjs.ems.InternalManager;
import de.fzj.unicore.xnjs.ems.Manager;
import de.fzj.unicore.xnjs.idb.ApplicationInfo;
import de.fzj.unicore.xnjs.idb.IDB;
import de.fzj.unicore.xnjs.idb.Incarnation;
import de.fzj.unicore.xnjs.io.CompositeFindOptions;
import de.fzj.unicore.xnjs.io.Permissions;
import de.fzj.unicore.xnjs.io.SimpleFindOptions;
import de.fzj.unicore.xnjs.io.http.IConnectionFactory;
import de.fzj.unicore.xnjs.jsdl.JSDLRenderer;
import de.fzj.unicore.xnjs.persistence.IActionStoreFactory;
import de.fzj.unicore.xnjs.persistence.JDBCActionStoreFactory;
import de.fzj.unicore.xnjs.ems.BasicManager;
import de.fzj.unicore.xnjs.ems.BudgetInfo;
import de.fzj.unicore.xnjs.tsi.IExecutionSystemInformation;
import de.fzj.unicore.xnjs.tsi.IReservation;
import de.fzj.unicore.xnjs.tsi.TSI;
import de.fzj.unicore.xnjs.tsi.local.LocalTSIModule;
import de.fzj.unicore.xnjs.tsi.remote.RemoteTSIModule;
import eu.unicore.security.Client;
import eu.unicore.services.ws.utils.WSServerUtilities;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * This facade class wrap some XNJS specifics to reduce 
 * clutter in the UAS implementation. Various helper methods
 * provide convenient access to XNJS functionality.<br/>
 * 
 * TODO document how to deal with multiple XNJS instances and
 * how the "default" XNJS instance works
 * 
 * @author schuller
 */
public class XNJSFacade {

	private static final Logger logger=LogUtil.getLogger(LogUtil.UNICORE,XNJSFacade.class);

	private static final String DEFAULT_INSTANCE=XNJSFacade.class.getName()+"_DEFAULT_XNJS";

	private XNJS xnjs;

	private String id;

	private TSIConnector tsiConnector;

	private InternalManager mgr;
	
	private Manager ems;
	
	private static synchronized XNJSInstancesMap getXNJSInstanceMap(Kernel kernel){
		XNJSInstancesMap map=kernel.getAttribute(XNJSInstancesMap.class);
		if(map==null){
			map=new XNJSInstancesMap();
			kernel.setAttribute(XNJSInstancesMap.class, map);
		}
		return map;
	}

	/**
	 * get an {@link XNJSFacade} instance
	 * 
	 * @param xnjsReference - the ID of the XNJS to use. Can be null, 
	 * then the default XNJS instance defined in the Kernel config is used
	 * @param kernel - the {@link Kernel}
	 * @return {@link XNJSFacade}
	 */
	public static synchronized XNJSFacade get(String xnjsReference, Kernel kernel){
		String ref=xnjsReference!=null?xnjsReference:DEFAULT_INSTANCE;

		XNJSFacade r=getXNJSInstanceMap(kernel).get(ref);
		if (r==null){
			r=new XNJSFacade(kernel);
			if(xnjsReference==null){
				r.doDefaultInit(kernel);
			}
			else{

			}
			getXNJSInstanceMap(kernel).put(ref, r);
			r.setID(ref);
		}
		return r;
	}

	private final Kernel kernel;

	private XNJSFacade(Kernel kernel){
		this.kernel=kernel;
	}

	private void setID(String id){
		this.id=id;
	}

	/**
	 * setup this XNJS instance
	 */
	public void configure(TSI_MODE mode, Properties properties, UASProperties uasProps)throws Exception{
		ConfigurationSource cs = new ConfigurationSource();
		cs.getProperties().putAll(properties);
		cs.setMetricRegistry(kernel.getMetricRegistry());
		cs.setMetricReporter(kernel.getMetricReporter());
		cs.addModule(new UASBaseModule(properties, kernel));
		if(TSI_MODE.embedded.equals(mode)){
			cs.addModule(new LocalTSIModule(properties));
		}
		else if(TSI_MODE.remote.equals(mode)){
			cs.addModule(new RemoteTSIModule(properties));
		}
		else if(TSI_MODE.custom.equals(mode)){
			String clazz = uasProps.getValue(UASProperties.TSF_TSI_CUSTOM_MODULE);
			AbstractModule m = (AbstractModule)(Class.forName(clazz).getConstructor(Properties.class).newInstance(properties));
			cs.addModule(m);
		}
		else throw new ConfigurationException("Invalid / unsupported TSI mode <"+mode+">");
			
		xnjs=new XNJS(cs);
		configure(xnjs);
		xnjs.start();
		ems = xnjs.get(Manager.class);
		mgr = xnjs.get(InternalManager.class);
		
		setupSystemConnector(mode);
	}
	
	public static class UASBaseModule extends AbstractModule {
		
		protected final Properties properties;
		
		protected final Kernel kernel;
		
		public UASBaseModule(Properties properties, Kernel kernel){
			this.properties = properties;
			this.kernel = kernel;
		}


		@Override
		protected void configure(){
			bind(InternalManager.class).to(BasicManager.class);
			bind(Manager.class).to(BasicManager.class);
			bind(IActionStoreFactory.class).to(JDBCActionStoreFactory.class);
			bind(ActionStateChangeListener.class).to(RESTNotificationSender.class);
		}
		
		@Provides
		public Kernel getKernel(){
			return kernel;
		}
		
		@Provides
		public IClientConfiguration getSecurityConfiguration(){
			return getKernel().getClientConfiguration();
		}
		
		@Provides
		public IConnectionFactory getConnectionFactory(){
			return new U6HttpConnectionFactory(getKernel());
		}
		
		
	}

	private void setupSystemConnector(TSI_MODE mode){
		tsiConnector = new TSIConnector(xnjs, mode);
		kernel.getExternalSystemConnectors().add(tsiConnector);
	}
	
	private void doDefaultInit(Kernel kernel){
		UASProperties uasConfig = kernel.getAttribute(UASProperties.class);
		TSI_MODE mode = uasConfig.getEnumValue(UASProperties.TSF_TSI_MODE, TSI_MODE.class);
		logger.info("Configuring XNJS using <"+mode+"> TSI.");
		Properties props = kernel.getContainerProperties().getRawProperties();
		try{
			configure(mode, props, uasConfig);
		}
		catch(Exception e){
			throw new RuntimeException("Error configuring XNJS.",e);
		}
	}

	/**
	 * do some UAS specific configuration
	 * 
	 * @param xnjs - the XNJS instance to configure
	 */
	protected void configure(XNJS xnjs){
		// setup special processors for data trigger processing
		if(!xnjs.haveProcessingFor(TriggerProcessor.actionType)){
			String[]chain=new String[]{TriggerProcessor.class.getName()};
			xnjs.setProcessingChain(TriggerProcessor.actionType, null, chain);
		}
		if(!xnjs.haveProcessingFor(SharedTriggerProcessor.actionType)){
			String[]chain=new String[]{SharedTriggerProcessor.class.getName()};
			xnjs.setProcessingChain(SharedTriggerProcessor.actionType, null, chain);
		}
	}

	public final Kernel getKernel(){
		return kernel;
	}

	/**
	 * helper method to get the workdir for a given action
	 */
	public String getWorkdir(String actionID){
		try{
			return ems.getAction(actionID).getExecutionContext().getWorkingDirectory();
		}catch(Exception e){
			return null;
		}
	}
	/**
	 * helper method to make an action from an JSDL doc
	 */
	public Action makeAction(JobDefinitionDocument doc){
		try{
			return xnjs.makeAction(doc);
		}catch(Exception e){
			return null;
		}
	}

	/**
	 * helper method to retrieve an action from the XNJS
	 */
	public final Action getAction(String id){
		try{
			return mgr.getAction(id);
		}catch(Exception e){
			LogUtil.logException("Error retrieving action <"+id+">", e);
			return null;
		}
	}

	/**
	 * Retrieve the status of an action
	 */
	public final Integer getStatus(String id, Client client){
		try{
			return ems.getStatus(id,client);
		}catch(Exception e){
			LogUtil.logException("Error retrieving action status for <"+id+">", e);
			return null;
		}
	}

	/**
	 * Retrieve the exit code of an action
	 */
	public final Integer getExitCode(String id, Client client){
		try{
			Action a = mgr.getAction(id);
			if(a!=null)return a.getExecutionContext().getExitCode();
			else return null;
		}catch(Exception e){
			LogUtil.logException("Error retrieving exit code for <"+id+">", e);
			return null;
		}
	}

	/**
	 * Retrieve the progress of an action
	 */
	public final Float getProgress(String id, Client client){
		try{
			Action a = mgr.getAction(id);
			if(a!=null)return a.getExecutionContext().getProgress();
			else {
				logger.info("Can't get progress for action "+id+", not found on XNJS.");
				return null;
			}
		}catch(Exception e){
			LogUtil.logException("Error retrieving progress for <"+id+">", e);
			return null;
		}
	}


	/**
	 * Destroy an action on the XNJS, removing its Uspace
	 * 
	 * @param id - the ID of the action to destroy 
	 */
	public void destroyAction(String id, Client client){
		try{
			ems.destroy(id,client);
		}catch(Exception e){
			LogUtil.logException("Error destroying job <"+id+">", e);
		}
	}

	/**
	 * get the XNJS manager object
	 * @return manager
	 */
	public final Manager getManager(){
		return ems;
	}

	/**
	 * get the XNJS Incarnation interface 
	 */
	public final Incarnation getGrounder(){
		return xnjs.get(Incarnation.class);
	}

	/**
	 * get the XNJS IDB for accessing resource info
	 */
	public final IDB getIDB(){
		return xnjs.get(IDB.class);
	}

	/**
	 * returns the XNJS object
	 */
	public final XNJS getXNJS(){
		return xnjs;
	}

	public TSI getTargetSystemInterface(Client client){
		return xnjs.getTargetSystemInterface(client);
	}
	
	/**
	 * Returns the number of active jobs (RUNNING/QUEUED) in the various queues. 
	 * The result may be <code>null</code>, if the system does not have queues 
	 */
	public Map<String,Integer>getQueueFill(){
		return xnjs.get(IExecutionSystemInformation.class).getQueueFill();
	}

	/**
	 * Returns the number of active jobs (RUNNING/QUEUED) on the XNJS 
	 */
	public int getNumberOfJobs(){
		return xnjs.get(IExecutionSystemInformation.class).getTotalNumberOfJobs();
	}

	/**
	 * Returns the remaining compute time (core hours) for the client 
	 */
	public List<BudgetInfo> getComputeTimeBudget(Client client) throws Exception {
		return xnjs.get(IExecutionSystemInformation.class).
				getComputeTimeBudget(client);
	}

	/**
	 * Returns the applications known to the XNJS
	 */
	public final Collection<ApplicationInfo> getDefinedApplications(Client client){
		return getIDB().getApplications(client);
	}

	/**
	 * Returns the applications known to the XNJS
	 * @deprecated consider using getDefinedApplications() instead
	 */
	public final ApplicationType[] getDefinedApplicationTypes(Client client){
		Collection<ApplicationInfo> apps = getDefinedApplications(client);
		ApplicationType[]at=new ApplicationType[apps.size()];
		int i=0;
		for(ApplicationInfo app: apps){
			at[i]=ApplicationType.Factory.newInstance();
			at[i].setApplicationName(app.getName());
			at[i].setApplicationVersion(app.getVersion());
			i++;
		}
		return at;
	}

	/**
	 * Returns the TextInfo resources configured in the XNJS IDB
	 */
	public final TextInfoType[] getDefinedTextInfo(){
		IDB idb = getIDB();
		Map<String,String> infos = idb.getTextInfoProperties();
		TextInfoType[] res=new TextInfoType[infos.keySet().size()];
		int i=0;
		for(String name: infos.keySet()){
			res[i]=TextInfoType.Factory.newInstance();
			res[i].setName(name);
			res[i].setValue(infos.get(name));
			i++;
		}
		return res;
	}

	/**
	 * Returns the system resources	
	 */
	public final ResourcesType getResources(Client c){
		try {
			IDB gr = getIDB();
			return new JSDLRenderer().render(gr.getDefaultPartition()).getResources();
		} catch (Exception e) {
			LogUtil.logException("Could not obtain resource information from XNJS.",e);
			return ResourcesType.Factory.newInstance();
		}
	}


	/**
	 * get a TSI for accessing files
	 * @param storageRoot -  the directory the TSI initally points to
	 * @param client -  the client object with authN/ authZ information
	 * @return TSI
	 */
	public final TSI getStorageTSI(String storageRoot, Client client){
		TSI tsi=xnjs.getTargetSystemInterface(client);
		tsi.setStorageRoot(storageRoot);
		return tsi;
	}

	/**
	 * get a TSI
	 * @param client -  the client object with authN/ authZ information
	 * @return TSI
	 */
	public final TSI getTSI(Client client){
		return xnjs.getTargetSystemInterface(client);
	}

	/**
	 * check if the XNJS is configured to support reservation
	 * @return true if the XNJS supports reservation, false otherwise
	 */
	Boolean haveReservation;
	
	public synchronized boolean supportsReservation(){
		if(haveReservation==null){
			try{
				IReservation r = xnjs.get(IReservation.class);
				haveReservation = r!=null;
			}
			catch(Exception ex){
				haveReservation = false;
			}
		}
		return haveReservation;
	}

	/**
	 * get the reservation interface
	 * @return an {@link IReservation} for making reservations
	 */
	public final IReservation getReservation(){
		return xnjs.get(IReservation.class);
	}


	//TODO check xnjs.stop()
	public void shutdown()throws Exception{
		//do not shutdown the default instance
		if(DEFAULT_INSTANCE.equals(id)){
			logger.warn("Tried to shutdown default XNJS, ignoring...");
			return;
		}
		xnjs.stop();
		//allow instance to be GC'ed
		getXNJSInstanceMap(kernel).put(id, null);
	}

	/**
	 * insert a "site-specific" resource request into the JSDL Resources document
	 *  
	 */
	public static void insertXNJSResourceSpec(String name, double value, ResourcesDocument resources){
		String s="<xnjs:ResourceSetting xmlns:xnjs=\"http://www.fz-juelich.de/unicore/xnjs/idb\">"
				+"<xnjs:Name>"+name+"</xnjs:Name>"
				+"<xnjs:Value xmlns:jsdl=\"http://schemas.ggf.org/jsdl/2005/11/jsdl\"><jsdl:Exact>"+value+"</jsdl:Exact></xnjs:Value>"
				+"</xnjs:ResourceSetting>";
		try{
			XmlObject o=XmlObject.Factory.parse(s);
			//and append to resources doc...
			WSServerUtilities.append(o, resources);
		}catch(Exception e){
			LogUtil.logException("Error building site-specific resource",e);
		}
	}

	/**
	 * get the last instant that the IDB was updated
	 *  
	 * @return long - the last update time (in millis)
	 */
	public final long getLastIDBUpdate(){
		return getIDB().getLastUpdateTime();
	}

	/**
	 * recursively build the filter options for the find command
	 * @param filter
	 * @return {@link CompositeFindOptions}
	 */
	public final CompositeFindOptions getFindOptions(final FilterType filter){
		CompositeFindOptions cfo=new CompositeFindOptions();
		if(filter!=null){
			String name=filter.getNameMatch();
			if(name!=null){
				cfo.and(SimpleFindOptions.stringMatch(name, false));
			}
			String regExp=filter.getNameMatchRegExp();
			if(regExp!=null){
				cfo.and(SimpleFindOptions.regExpMatch(regExp, false));
			}
			Calendar before=filter.getBefore();
			if(before!=null){
				cfo.and(SimpleFindOptions.lastAccessBefore(before,false));
			}
			Calendar after=filter.getAfter();
			if(after!=null){
				cfo.and(SimpleFindOptions.lastAccessAfter(after,false));
			}
			//check sub filters...
			if(filter.getAndFilter()!=null){
				CompositeFindOptions andCFO=getFindOptions(filter.getAndFilter());
				cfo.and(andCFO);
			}
			if(filter.getOrFilter()!=null){
				CompositeFindOptions orCFO=getFindOptions(filter.getOrFilter());
				cfo.or(orCFO);
			}
		}
		return cfo;
	}

	public static Permissions getXNJSPermissions(PermissionsType uasPermissions){
		Permissions perm=new Permissions();
		perm.setExecutable(uasPermissions.getExecutable());
		perm.setReadable(true);//all else would be complete nonsense
		perm.setWritable(uasPermissions.getWritable());
		return perm;
	}

	public static PermissionsType getUASPermissions(Permissions xnjsPermissions){
		PermissionsType perm=PermissionsType.Factory.newInstance();
		perm.setExecutable(xnjsPermissions.isExecutable());
		perm.setReadable(true);//all else would be complete nonsense
		perm.setWritable(xnjsPermissions.isWritable());
		return perm;
	}

	/**
	 * list all jobs accessible by the particular client (may be more than actually wanted, so 
	 * a second, more detailed filtering step should be made...)
	 * @param client
	 */
	public List<String>listJobIDs(Client client)throws Exception{
		return Arrays.asList(ems.list(client));
	}

	public static class XNJSInstancesMap extends HashMap<String, XNJSFacade>{
		private static final long serialVersionUID = 1L;
	}

}
