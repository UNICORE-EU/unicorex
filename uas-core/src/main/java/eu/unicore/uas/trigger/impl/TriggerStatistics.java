package eu.unicore.uas.trigger.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class TriggerStatistics {

	private long duration;
	
	private final AtomicInteger numberOfFiles = new AtomicInteger();
	
	private final Set<String> rulesInvoked = new HashSet<String>();
	
	public TriggerStatistics(){
		
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public int getNumberOfFiles() {
		return numberOfFiles.get();
	}

	public void setNumberOfFiles(int numberOfFiles) {
		this.numberOfFiles.set(numberOfFiles);
	}

	public void incrementNumberOfFiles(){
		numberOfFiles.incrementAndGet();
	}
	
	public void ruleInvoked(String ruleName){
		rulesInvoked.add(ruleName);
	}
	
	public Collection<String>getRulesInvoked(){
		return rulesInvoked;
	}
	
	public String toString(){
		return "Files processed: "+numberOfFiles.get()+", rules invoked: "+rulesInvoked+", total time: "+duration+ " ms.";
	}
}
