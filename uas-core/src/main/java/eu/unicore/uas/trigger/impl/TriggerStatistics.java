package eu.unicore.uas.trigger.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class TriggerStatistics {

	private long duration;

	private final AtomicInteger numberOfFiles = new AtomicInteger();

	private final Set<String> rulesInvoked = new HashSet<>();

	private final List<String> actionsLaunched = new ArrayList<>();

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

	public void addAction(String id) {
		if(id!=null)actionsLaunched.add(id);
	}

	public Collection<String> getRulesInvoked(){
		return rulesInvoked;
	}

	public List<String> getActionsLaunched(){
		return actionsLaunched;
	}

	@Override
	public String toString(){
		return "Files processed: "+numberOfFiles.get()+", rules invoked: "+rulesInvoked+
				", actions launched: "+actionsLaunched+", time: "+duration+ " ms.";
	}

}