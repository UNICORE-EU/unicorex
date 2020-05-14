package de.fzj.unicore.xnjs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Reporter;
import com.google.inject.AbstractModule;

/**
 * Holds the XNJS base configuration
 * 
 * TODO track changes in the source and update
 * 
 * @author schuller
 */
public class ConfigurationSource {

	protected final Map<String,ProcessorChain> processingChains = new HashMap<>();
	protected final Properties properties = new Properties();
	protected final List<AbstractModule> modules = new ArrayList<>();
	
	private MetricRegistry metricRegistry;
	private Reporter metricReporter;
	
	/**
	 * get the processor chains keyed with the action type
	 */
	public Map<String, ProcessorChain> getProcessingChains() {
		return processingChains;
	}

	public void configureProcessing(ProcessorChain pc){
		processingChains.put(pc.getActionType(), pc);
	}
	
	public Properties getProperties() {
		return properties;
	}


	/**
	 * get the processor chain for the action type
	 */
	public ProcessorChain getProcessing(String actionType) {
		return processingChains.get(actionType);
	}
	
	public String getJobType(String jobDescriptionType){
		for(ProcessorChain pc: processingChains.values()){
			if(jobDescriptionType.equals(pc.getJobDescriptionType())){
				return pc.getActionType();
			}
		}
		return null;
	}
	
	public synchronized MetricRegistry getMetricRegistry(){
		if(metricRegistry==null){
			metricRegistry = new MetricRegistry();
		}
		return metricRegistry;
	}
	
	public void setMetricRegistry(MetricRegistry metricRegistry){
		this.metricRegistry = metricRegistry;
	}
	
	public Reporter getMetricReporter(){
		return metricReporter;
	}
	
	public void setMetricReporter(Reporter metricReporter){
		this.metricReporter = metricReporter;
	}
	
	public void addModule(AbstractModule m){
		modules.add(m);
	}
	
	/** read-only, use addModule() to add modules **/
	public List<AbstractModule> getModules(){
		return Collections.unmodifiableList(modules);
	}
	
	public static class ProcessorChain {
		
		private final String jobDescriptionType;
		private final String actionType;
		private final List<String>processorClasses = new ArrayList<>();
		
		public ProcessorChain(String actionType, String jobdescriptionType){
			this.jobDescriptionType = jobdescriptionType;
			this.actionType = actionType;
		}
		
		public ProcessorChain(String actionType, String jobdescriptionType, String[]processorClasses){
			this.jobDescriptionType = jobdescriptionType;
			this.actionType = actionType;
			if(processorClasses!=null)
				this.processorClasses.addAll(Arrays.asList(processorClasses));
		}
		
		public String getJobDescriptionType() {
			return jobDescriptionType;
		}
		
		public String getActionType() {
			return actionType;
		}
		
		public List<String> getProcessorClasses() {
			return processorClasses;
		}
		
		public String toString(){
			return "ProcessorChain: "+actionType+"<"+jobDescriptionType+"> "+processorClasses;
		}
	}
}
