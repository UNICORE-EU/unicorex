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
 *********************************************************************************/


package de.fzj.unicore.xnjs;

import java.io.FileNotFoundException;
import java.io.Serializable;
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
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.json.JSONObject;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SlidingWindowReservoir;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

import de.fzj.unicore.persist.PersistenceProperties;
import de.fzj.unicore.persist.cluster.Cluster;
import de.fzj.unicore.xnjs.ConfigurationSource.ProcessorChain;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.ems.Manager;
import de.fzj.unicore.xnjs.ems.Processor;
import de.fzj.unicore.xnjs.ems.processors.AsyncCommandProcessor;
import de.fzj.unicore.xnjs.ems.processors.DataStagingProcessor;
import de.fzj.unicore.xnjs.ems.processors.UsageLogger;
import de.fzj.unicore.xnjs.fts.FTSProcessor;
import de.fzj.unicore.xnjs.io.IOProperties;
import de.fzj.unicore.xnjs.jsdl.JSDLProcessor;
import de.fzj.unicore.xnjs.jsdl.JSDLUtils;
import de.fzj.unicore.xnjs.json.JSONJobProcessor;
import de.fzj.unicore.xnjs.json.sweep.JSONSweepInstanceProcessor;
import de.fzj.unicore.xnjs.json.sweep.JSONSweepProcessor;
import de.fzj.unicore.xnjs.persistence.IActionStore;
import de.fzj.unicore.xnjs.persistence.IActionStoreFactory;
import de.fzj.unicore.xnjs.tsi.TSI;
import de.fzj.unicore.xnjs.tsi.TSIFactory;
import de.fzj.unicore.xnjs.util.LogUtil;
import eu.unicore.security.Client;
import eu.unicore.util.configuration.ConfigurationException;

/**
 * XNJS main class<br/>
 * 
 * @author schuller
 */
public class XNJS {

	private static final Logger logger=LogUtil.getLogger(LogUtil.XNJS,XNJS.class);

	private final Calendar startTime=Calendar.getInstance();

	private final ConfigurationSource config;

	private volatile boolean stopped=true;

	private Injector injector;

	private final Properties properties;

	private final XNJSProperties baseProperties;

	private final IOProperties ioProperties;

	private final PersistenceProperties persistenceProperties;

	private final ScheduledExecutorService es;

	private static final AtomicInteger id=new AtomicInteger(0);

	private final String uniqueID;

	private final MetricRegistry metricRegistry;

	private final TSIFactory tsiFactory;

	private final Map<Class<?>,Object>attributes=new HashMap<Class<?>,Object>();

	/**
	 * creates a new instance of the XNJS with its own configuration<br>
	 * (does not yet start the XNJS!)
	 * 
	 * @param configSource
	 * @throws Exception
	 */
	public XNJS(ConfigurationSource configSource) throws Exception{
		assert configSource!=null;
		this.config = configSource;
		uniqueID=String.valueOf(id.incrementAndGet());

		metricRegistry = configSource.getMetricRegistry();
		tsiFactory = new TSIFactory(this);
		properties = configSource.getProperties();
		baseProperties = new XNJSProperties(properties);
		persistenceProperties = new PersistenceProperties(properties);
		String stateDir=baseProperties.getValue(XNJSProperties.STATEDIR);
		if(stateDir!=null){
			logger.warn("Deprecated property <XNJS.statedir> found, please use <persistence.directory> instead!");
			persistenceProperties.setProperty(PersistenceProperties.DB_DIRECTORY+".JOBS", stateDir);
			persistenceProperties.setProperty(PersistenceProperties.DB_DIRECTORY+".FINISHED_JOBS", stateDir);
		}
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
			public MetricRegistry getMetricRegistry(){
				return metricRegistry;
			}

			@Provides
			public TSIFactory getTSIFactory(){
				return tsiFactory;
			}

			@Provides
			public XNJS getXNJS(){
				return XNJS.this;
			}
		};
		configSource.addModule(common);
		List<AbstractModule> configuredModules = configSource.getModules();
		injector = Guice.createInjector(configuredModules.toArray(new AbstractModule[configuredModules.size()]));
	}

	public static final String getVersion(){
		String v=XNJS.class.getPackage().getImplementationVersion();
		if(v==null)v="(DEVELOPMENT version)";
		return v;
	}

	public static String writeShortHeader(){
		return "XNJS "+getVersion()+" (c) Forschungszentrum Juelich 2005-2020";
	}

	public Calendar getStartTime() throws Exception{
		return startTime;
	}

	/**
	 * make sure the processor chains for some default cases
	 * are configured, otherwise set the default
	 */
	protected void assureDefaultProcessingAvailable(){

		if(!haveProcessingFor("JSON")){
			setProcessingChain("JSON", "JSON", 
					new String[]{JSONJobProcessor.class.getName(),UsageLogger.class.getName(),
			});
		}
		if(!haveProcessingFor(XNJSConstants.jsdlStageInActionType)){
			setProcessingChain(XNJSConstants.jsdlStageInActionType, null, 
					new String[]{DataStagingProcessor.class.getName()});
		}
		if(!haveProcessingFor(XNJSConstants.jsdlStageOutActionType)){
			setProcessingChain(XNJSConstants.jsdlStageOutActionType, null, 
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

		// JSDL - TODO deprecated
		if(!haveProcessingFor("JSDL")){
			setProcessingChain("JSDL", JSDLUtils.JSDL_JOBDEFINITION, 
					new String[]{JSDLProcessor.class.getName(),UsageLogger.class.getName(),
			});
		}
	}

	private void registerDefaultMetrics(){
		if(!metricRegistry.getNames().contains(XNJSConstants.MEAN_TIME_QUEUED)){
			Metric mtq = new Histogram(new SlidingWindowReservoir(512));
			metricRegistry.register(XNJSConstants.MEAN_TIME_QUEUED, mtq);
		}
	}

	public synchronized void start() throws Exception {
		if(stopped==true){
			logger.info("STARTING "+writeShortHeader());
			assureDefaultProcessingAvailable();
			registerDefaultMetrics();
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
		}catch(RuntimeException ex){
			if(optional) {
				return null;
			}
			else {
				throw ex;
			}
		}
	}

	/**
	 * returns a typed attribute 
	 * @param key - the attribute class used as key
	 * @return the attribute
	 */
	public <T> T getAttribute(Class<T>key){
		Object o=attributes.get(key);
		return  key.cast(o);
	}

	/**
	 * store a typed kernel attribute
	 * @param key - the value's class as key
	 * @param value - the value
	 */
	public <T> void setAttribute(Class<T>key, T value){
		attributes.put(key, value);
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

	public Action makeAction(XmlObject job){
		return makeAction(job, java.util.UUID.randomUUID().toString());
	}

	public Action makeAction(XmlObject job, String uuid){
		Action a=new Action(uuid);
		XmlCursor c=job.newCursor();
		c.toFirstChild();
		String qn=c.getName().getNamespaceURI();
		String type=config.getJobType(qn);
		if(type==null)return null;
		a.setAjd((Serializable)job);
		a.setType(type);
		return a;
	}
	
	public Action makeAction(JSONObject job){
		return makeAction(job, "JSON", java.util.UUID.randomUUID().toString());
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

	public IActionStore getActionStore(String id) throws Exception {
		return get(IActionStoreFactory.class).getInstance(id,this);
	}

	public final ScheduledExecutorService getScheduledExecutor(){
		return es;
	}

	public MetricRegistry getMetricRegistry(){
		return metricRegistry;
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

	public boolean isClusterEnabled(){
		return persistenceProperties.getBooleanValue(PersistenceProperties.DB_LOCKS_DISTRIBUTED);
	}

	public Cluster getCluster() {
		if(!isClusterEnabled())return null;
		try{
			return Cluster.getInstance(persistenceProperties.getFileValueAsString(
					PersistenceProperties.DB_CLUSTER_CONFIG, false));
		}catch(FileNotFoundException fe){
			throw new ConfigurationException("Cannot setup cluster instance", fe);
		}
	}

	public String getProperty(String key){
		return properties.getProperty(key);
	}

	public String getProperty(String key, String defaultValue){
		return properties.getProperty(key,defaultValue);
	}

	public void setProperty(String key, String value){
		if(value==null){
			properties.remove(key);
		}
		else{
			properties.setProperty(key, value);
		}
	}

	public boolean isStopped(){
		return stopped;
	}

}
