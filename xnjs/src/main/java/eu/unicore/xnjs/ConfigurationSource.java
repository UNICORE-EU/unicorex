package eu.unicore.xnjs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.codahale.metrics.Metric;
import com.google.inject.AbstractModule;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.UpdateableConfiguration;

/**
 * Holds the XNJS base configuration
 * 
 * @author schuller
 */
public class ConfigurationSource implements UpdateableConfiguration {

	protected final Map<String,ProcessorChain> processingChains = new HashMap<>();
	protected final List<AbstractModule> modules = new ArrayList<>();
	protected final Map<String, Metric> metrics = new HashMap<>();
	protected Properties properties = new Properties();

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

	public void addModule(AbstractModule m){
		modules.add(m);
		if(m instanceof MetricProvider) {
			getMetrics().putAll(((MetricProvider)m).getMetrics());
		}
	}
	
	/** read-only, use addModule() to add modules **/
	public List<AbstractModule> getModules(){
		return Collections.unmodifiableList(modules);
	}
	
	public Map<String, Metric> getMetrics(){
		return metrics;
	}
	
	@Override
	public void setProperties(Properties newProperties) throws ConfigurationException {
		this.properties = new Properties();
		for(AbstractModule m: modules) {
			if(m instanceof UpdateableConfiguration) {
				((UpdateableConfiguration)m).setProperties(newProperties);
			}
		}
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
	
	public static interface MetricProvider {
		public Map<String, Metric> getMetrics();
	}
}
