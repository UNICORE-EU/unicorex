package eu.unicore.uas.xnjs;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import com.codahale.metrics.Metric;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import eu.unicore.security.Client;
import eu.unicore.services.ExternalSystemConnector;
import eu.unicore.services.ISubSystem;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.security.UserPublicKeyCache;
import eu.unicore.services.rest.security.UserPublicKeyCache.UserInfoSource;
import eu.unicore.uas.UASProperties;
import eu.unicore.uas.UASProperties.TSI_MODE;
import eu.unicore.uas.trigger.xnjs.SharedTriggerProcessor;
import eu.unicore.uas.trigger.xnjs.TriggerProcessor;
import eu.unicore.uas.util.LogUtil;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.httpclient.IClientConfiguration;
import eu.unicore.xnjs.ConfigurationSource;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.XNJSProperties;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.ActionStateChangeListener;
import eu.unicore.xnjs.ems.BasicManager;
import eu.unicore.xnjs.ems.BudgetInfo;
import eu.unicore.xnjs.ems.InternalManager;
import eu.unicore.xnjs.ems.Manager;
import eu.unicore.xnjs.ems.event.INotificationSender;
import eu.unicore.xnjs.idb.ApplicationInfo;
import eu.unicore.xnjs.idb.IDB;
import eu.unicore.xnjs.idb.Incarnation;
import eu.unicore.xnjs.io.http.IConnectionFactory;
import eu.unicore.xnjs.persistence.IActionStoreFactory;
import eu.unicore.xnjs.persistence.JDBCActionStoreFactory;
import eu.unicore.xnjs.tsi.IExecutionSystemInformation;
import eu.unicore.xnjs.tsi.IReservation;
import eu.unicore.xnjs.tsi.TSI;
import eu.unicore.xnjs.tsi.local.LocalTSIModule;
import eu.unicore.xnjs.tsi.remote.RemoteTSIModule;

/**
 * This facade class wrap some XNJS specifics to reduce 
 * clutter in the UAS implementation. Various helper methods
 * provide convenient access to XNJS functionality.
 * 
 * @author schuller
 */
public class XNJSFacade implements ISubSystem {

	private static final Logger logger=LogUtil.getLogger(LogUtil.UNICORE,XNJSFacade.class);

	private static final String DEFAULT_INSTANCE = "DEFAULT";

	private XNJS xnjs;

	private String id;

	private TSIStatusChecker tsiConnector;

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
				//TODO
			}
			getXNJSInstanceMap(kernel).put(ref, r);
			r.setID(ref);
			kernel.register(r);
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

	private void configure(TSI_MODE mode, Properties properties, UASProperties uasProps)throws Exception{
		ConfigurationSource cs = new ConfigurationSource();
		cs.getProperties().putAll(properties);
		cs.addModule(new UASBaseModule(kernel));
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
		
		protected final Kernel kernel;
		
		public UASBaseModule(Kernel kernel){
			this.kernel = kernel;
		}


		@Override
		protected void configure(){
			bind(InternalManager.class).to(BasicManager.class);
			bind(Manager.class).to(BasicManager.class);
			bind(IActionStoreFactory.class).to(JDBCActionStoreFactory.class);
			bind(ActionStateChangeListener.class).to(RESTNotificationSender.class);
			bind(INotificationSender.class).to(NotificationSender.class);
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
		tsiConnector = new TSIStatusChecker(xnjs, mode);
	}
	
	@Override
	public Collection<ExternalSystemConnector>getExternalConnections(){
		return tsiConnector!=null ?
				Lists.newArrayList(tsiConnector) : Collections.emptyList();
	}
	
	@Override
	public String getName() {
		return "Target system access [XNJS "+id+"]";
	}

	@Override
	public String getStatusDescription() {
		return "OK";
	}
	
	@Override
	public Map<String, Metric> getMetrics(){
		return xnjs.getMetrics();
	}

	@Override
	public void reloadConfig(Kernel kernel) {
		xnjs.setProperties(kernel.getContainerProperties().getRawProperties());
	}

	private void doDefaultInit(Kernel kernel){
		UASProperties uasConfig = kernel.getAttribute(UASProperties.class);
		TSI_MODE mode = uasConfig.getEnumValue(UASProperties.TSF_TSI_MODE, TSI_MODE.class);
		logger.info("Configuring XNJS using <{}> TSI.", mode);
		Properties props = kernel.getContainerProperties().getRawProperties();
		try{
			configure(mode, props, uasConfig);
			UserPublicKeyCache cache = kernel.getAttribute(UserPublicKeyCache.class);
			if(cache!=null) {
				Collection<UserInfoSource>sources = cache.getUserInfoSources();
				for(UserInfoSource s: sources) {
					if(s instanceof TSIUserInfoLoader)return;
				}
				sources.add(new TSIUserInfoLoader(kernel));
			}
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

	public String getWorkdir(String actionID) throws Exception {
		return ems.getAction(actionID).getExecutionContext().getWorkingDirectory();
	}
	
	public Action makeAction(JSONObject doc) throws Exception {
		return xnjs.makeAction(doc);
	}

	public final Action getAction(String id) throws Exception {
		return mgr.getAction(id);
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
	 * Retrieve the progress of an action
	 */
	public final String getJobDescription(String id){
		try{
			Action a = mgr.getAction(id);
			if(a!=null)return String.valueOf(a.getAjd());
			else {
				logger.info("Can't get job description for action "+id+", not found on XNJS.");
				return "n/a";
			}
		}catch(Exception e){
			LogUtil.logException("Error retrieving job description for <"+id+">", e);
			return "n/a";
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

	public final Manager getManager(){
		return ems;
	}

	public final Incarnation getGrounder(){
		return xnjs.get(Incarnation.class);
	}

	public final IDB getIDB(){
		return xnjs.get(IDB.class);
	}

	/**
	 * returns the XNJS object
	 */
	public final XNJS getXNJS(){
		return xnjs;
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
	 * get a TSI for accessing files
	 * @param storageRoot -  the directory the TSI initally points to
	 * @param client -  the client object with authN/ authZ information
	 * @return TSI
	 */
	public final TSI getStorageTSI(String storageRoot, Client client, String preferredLoginNode){
		TSI tsi=xnjs.getTargetSystemInterface(client, preferredLoginNode);
		tsi.setStorageRoot(storageRoot);
		return tsi;
	}

	/**
	 * get a TSI
	 * @param client -  the client object with authN/ authZ information
	 * @return TSI
	 */
	public final TSI getTSI(Client client){
		return xnjs.getTargetSystemInterface(client, null);
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
	 * get the last instant that the IDB was updated
	 *  
	 * @return long - the last update time (in millis)
	 */
	public final long getLastIDBUpdate(){
		return getIDB().getLastUpdateTime();
	}

	/**
	 * list all jobs accessible by the particular client (may be more than actually wanted, so 
	 * a second, more detailed filtering step should be made...)
	 * @param client
	 */
	public Collection<String>listJobIDs(Client client)throws Exception{
		return ems.list(client);
	}
	
	public String getDefaultUmask() {
		return xnjs.getXNJSProperties().getValue(XNJSProperties.DEFAULT_UMASK);
	}

	public static class XNJSInstancesMap extends HashMap<String, XNJSFacade>{
		private static final long serialVersionUID = 1L;
	}

}
