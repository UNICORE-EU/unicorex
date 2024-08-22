package eu.unicore.xnjs;

import java.lang.reflect.Constructor;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import com.codahale.metrics.Metric;
import com.google.inject.AbstractModule;
import com.google.inject.ConfigurationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

import eu.unicore.persist.PersistenceProperties;
import eu.unicore.persist.util.UUID;
import eu.unicore.security.Client;
import eu.unicore.util.configuration.UpdateableConfiguration;
import eu.unicore.xnjs.ConfigurationSource.ProcessorChain;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.ems.Manager;
import eu.unicore.xnjs.ems.Processor;
import eu.unicore.xnjs.ems.processors.AsyncCommandProcessor;
import eu.unicore.xnjs.ems.processors.DataStagingProcessor;
import eu.unicore.xnjs.ems.processors.UsageLogger;
import eu.unicore.xnjs.fts.FTSProcessor;
import eu.unicore.xnjs.io.IOProperties;
import eu.unicore.xnjs.json.JSONJobProcessor;
import eu.unicore.xnjs.json.sweep.JSONSweepInstanceProcessor;
import eu.unicore.xnjs.json.sweep.JSONSweepProcessor;
import eu.unicore.xnjs.persistence.IActionStore;
import eu.unicore.xnjs.persistence.IActionStoreFactory;
import eu.unicore.xnjs.tsi.TSI;
import eu.unicore.xnjs.tsi.TSIFactory;
import eu.unicore.xnjs.tsi.remote.TSIMessages;
import eu.unicore.xnjs.util.LogUtil;

/**
 * XNJS main class
 * 
 * @author schuller
 */
public class XNJS implements UpdateableConfiguration {

	private static final Logger logger=LogUtil.getLogger(LogUtil.XNJS,XNJS.class);

	private final Calendar startTime=Calendar.getInstance();

	private final ConfigurationSource config;

	private volatile boolean stopped=true;

	private Injector injector;

	private Properties properties;

	private final XNJSProperties baseProperties;

	private final IOProperties ioProperties;

	private final PersistenceProperties persistenceProperties;

	private final ScheduledExecutorService es;

	private static final AtomicInteger id=new AtomicInteger(0);

	private final String uniqueID;

	private final Map<String, Metric> metrics = new HashMap<>();

	private final TSIFactory tsiFactory;

	private final TSIMessages tsiMessages;

	public XNJS(ConfigurationSource configSource) throws Exception{
		this(configSource, null);
	}
	/**
	 * creates a new instance of the XNJS with its own configuration<br>
	 * (does not yet start the XNJS!)
	 * 
	 * @param configSource
	 * @throws Exception
	 */
	public XNJS(ConfigurationSource configSource, String identifier) throws Exception{
		assert configSource!=null;
		this.config = configSource;
		uniqueID = identifier!=null? identifier : String.valueOf(id.incrementAndGet());
		tsiFactory = new TSIFactory(this);
		properties = configSource.getProperties();
		baseProperties = new XNJSProperties(properties);
		persistenceProperties = new PersistenceProperties(properties);
		ioProperties = new IOProperties(properties);
		es=Executors.newScheduledThreadPool(1,
				new ThreadFactory() {
			final AtomicInteger threadNumber = new AtomicInteger(1);

			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setName("xnjs-"+uniqueID+"-sched-"+threadNumber.getAndIncrement());
				return t;
			}
		});
		tsiMessages = new TSIMessages(this);

		AbstractModule common = new AbstractModule() {

			@Override
			protected void configure() {}

			@Provides
			PersistenceProperties getPersistenceProperties(){
				return persistenceProperties;
			}

			@Provides
			XNJSProperties getXNJSProperties(){
				return baseProperties;
			}

			@Provides
			IOProperties getIOProperties(){
				return ioProperties;
			}

			@Provides
			public ScheduledExecutorService getExecutor(){
				return es;
			}

			@Provides
			public TSIFactory getTSIFactory(){
				return tsiFactory;
			}

			@Provides
			public TSIMessages getTSIMessages(){
				return tsiMessages;
			}

			@Provides
			public XNJS getXNJS(){
				return XNJS.this;
			}
		};
		configSource.addModule(common);
		List<AbstractModule> configuredModules = configSource.getModules();
		injector = Guice.createInjector(configuredModules.toArray(new AbstractModule[configuredModules.size()]));
		configSource.getMetrics().entrySet().forEach(entry ->
				metrics.put(entry.getKey()+"-"+uniqueID, entry.getValue()));
	}

	public static final String getVersion(){
		String v=XNJS.class.getPackage().getImplementationVersion();
		if(v==null)v="(DEVELOPMENT version)";
		return v;
	}

	public static String writeShortHeader(){
		return "XNJS "+getVersion()+" (c) Forschungszentrum Juelich GmbH";
	}

	public Calendar getStartTime() throws Exception{
		return startTime;
	}

	/**
	 * make sure the processor chains for some default cases
	 * are configured, otherwise set the default
	 */
	protected void assureDefaultProcessingAvailable(){

		if(!haveProcessingFor(XNJSConstants.jobActionType)){
			setProcessingChain(XNJSConstants.jobActionType, "JSON", 
					new String[]{JSONJobProcessor.class.getName(),UsageLogger.class.getName(),
			});
		}
		if(!haveProcessingFor(XNJSConstants.jobStageInActionType)){
			setProcessingChain(XNJSConstants.jobStageInActionType, null, 
					new String[]{DataStagingProcessor.class.getName()});
		}
		if(!haveProcessingFor(XNJSConstants.jobStageOutActionType)){
			setProcessingChain(XNJSConstants.jobStageOutActionType, null, 
					new String[]{DataStagingProcessor.class.getName()});
		}
		if(!haveProcessingFor(XNJSConstants.asyncCommandType)){
			setProcessingChain(XNJSConstants.asyncCommandType, null, 
					new String[]{AsyncCommandProcessor.class.getName()});
		}
		if(!haveProcessingFor(JSONSweepProcessor.sweepActionType)){
			setProcessingChain(JSONSweepProcessor.sweepActionType, null, 
					new String[]{JSONSweepProcessor.class.getName()});
		}
		if(!haveProcessingFor(JSONSweepProcessor.sweepInstanceType)){
			setProcessingChain(JSONSweepProcessor.sweepInstanceType, null, 
					new String[]{JSONSweepInstanceProcessor.class.getName()});
		}

		if(!haveProcessingFor("FTS")){
			setProcessingChain("FTS", "FTS", 
					new String[]{FTSProcessor.class.getName(),
			});
		}
	}

	public synchronized void start() throws Exception {
		if(stopped==true){
			logger.info("STARTING "+writeShortHeader());
			assureDefaultProcessingAvailable();
			get(Manager.class).start();
			stopped=false;
		}else logger.info("Already started.");

	}

	public synchronized void stop(){
		if(stopped==true){
			logger.info("Already stopped.");
			return;
		}
		stopped=true;
		es.shutdown();
		try{
			get(Manager.class).stop();
		}catch(Exception ce){}
	}
	
	public synchronized <T> T get(Class<T> clazz) {
		return get(clazz, false);
	}

	public synchronized <T> T get(Class<T> clazz, boolean optional){
		if(clazz==null)throw new NullPointerException("Component class cannot be null!");

		try{
			return injector.getInstance(clazz);
		}catch(ConfigurationException ex){
			if(optional) {
				return null;
			}
			else {
				throw ex;
			}
		}
	}

	public Processor createProcessor(String actionType){
		try{
			return getProcessor(actionType);
		}catch(Exception ex){
			LogUtil.logException("Can't create processor for action type "+actionType+", continuing...",ex,logger);
			return null;
		}
	}

	/**
	 * create/return a fully configured processor for the given Action type 
	 */
	private Processor getProcessor(String actionType)throws ExecutionException{
		try{
			//get a chain of processor class names from config
			List<String> chain = getProcessorChain(actionType);
			int size = chain.size();
			Processor[] ps=new Processor[size];
			for(int i=0; i<size;i++){
				ps[i]=createInstance(chain.get(i));
				if(i>0){
					ps[i-1].setNext(ps[i]);
				}
			}
			return ps[0];
		}catch(Exception e){
			throw new ExecutionException("Can't create processor for action type <"+actionType+">",e);
		}
	}

	/**
	 * create a processor instance 
	 */
	@SuppressWarnings("unchecked")
	private Processor createInstance(String classname) throws Exception {
		Class<? extends Processor> pClass=(Class<? extends Processor>)Class.forName(classname);
		Constructor<Processor> c=(Constructor<Processor>)pClass.getConstructor(new Class[]{XNJS.class});
		return c.newInstance(new Object[]{this});
	}

	public List<String> getProcessorChain(String type)
	{
		ProcessorChain pc = config.getProcessing(type);
		return pc!=null ? pc.getProcessorClasses() : null;
	}

	public boolean haveProcessingFor(String actionType){
		return config.getProcessing(actionType)!=null;
	}

	public void setProcessingChain(String actionType, String jdType, String[] chain){
		ProcessorChain pc = new ProcessorChain(actionType, jdType, chain);
		config.getProcessingChains().put(actionType, pc);
	}
	
	public Action makeAction(JSONObject job){
		return makeAction(job, "JSON", UUID.newUniqueID());
	}

	public Action makeAction(JSONObject job, String type, String uuid){
		Action a = new Action(uuid);
		a.setAjd(job.toString());
		a.setType(type);
		return a;
	}

	public TSI getTargetSystemInterface(Client client){
		return getTargetSystemInterface(client, null);
	}

	public TSI getTargetSystemInterface(Client client, String preferredTSINode){
		return tsiFactory.createTSI(client, preferredTSINode);
	}

	public IActionStore getActionStore(String id) {
		return get(IActionStoreFactory.class).getInstance(id,this);
	}

	public final ScheduledExecutorService getScheduledExecutor(){
		return es;
	}

	public Map<String, Metric> getMetrics(){
		return metrics;
	}

	public String getID(){
		return uniqueID;
	}

	public XNJSProperties getXNJSProperties(){
		return baseProperties;
	}

	public IOProperties getIOProperties(){
		return ioProperties;
	}

	public PersistenceProperties getPersistenceProperties(){
		return persistenceProperties;
	}

	public void setProperty(String key, String value){
		if(value==null){
			properties.remove(key);
		}
		else{
			properties.setProperty(key, value);
		}
	}
	
	@Override
	public void setProperties(Properties newProperties) {
		this.properties = newProperties;
		config.setProperties(newProperties);
		baseProperties.setProperties(newProperties);
		ioProperties.setProperties(newProperties);
	}

	public Properties getRawProperties() {
		return properties;
	}

	public boolean isStopped(){
		return stopped;
	}

}
