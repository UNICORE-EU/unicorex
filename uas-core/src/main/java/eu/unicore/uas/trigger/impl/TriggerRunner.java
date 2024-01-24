package eu.unicore.uas.trigger.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.Logger;

import eu.unicore.security.Client;
import eu.unicore.uas.trigger.Action;
import eu.unicore.uas.trigger.MultiFileAction;
import eu.unicore.uas.trigger.Rule;
import eu.unicore.uas.trigger.RuleSet;
import eu.unicore.uas.util.LogUtil;
import eu.unicore.util.Log;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.xnjs.io.XnjsFile;

public class TriggerRunner implements Callable<TriggerStatistics>, TriggerContext {

	private static final Logger logger = LogUtil.getLogger(LogUtil.TRIGGER, TriggerRunner.class);
	private static final Logger usage = LogUtil.getLogger(LogUtil.TRIGGER+".USAGE", TriggerRunner.class);

	private final XnjsFile[] files;

	private final RuleSet rules;

	private final IStorageAdapter storage;

	private final Client client;

	private final XNJS xnjs;


	public TriggerRunner(XnjsFile[] files, RuleSet rules, IStorageAdapter storage, Client client, XNJS xnjs){
		this.files=files;
		this.rules=rules;
		this.storage=storage;
		this.client=client;
		this.xnjs=xnjs;
	}

	/**
	 * checks all files and (sequentially) invoke all the matching rules
	 * TODO bunching/chunking?!
	 */
	@Override
	public TriggerStatistics call() {
		TriggerStatistics ts=new TriggerStatistics();

		long time=System.currentTimeMillis();

		for(Rule r: rules){
			r.begin();
		}

		Map<MultiFileAction, List<String>>multifile=new HashMap<MultiFileAction, List<String>>();

		for(XnjsFile file: files){
			String path=file.getPath();
			boolean match=false;
			for(Rule r: rules){
				try{
					if(r.matches(path, this)){
						match=true;
						ts.ruleInvoked(r.getName());
						Action a=r.getAction();
						if(a instanceof MultiFileAction){
							MultiFileAction ma=(MultiFileAction)a;
							List<String>files=multifile.get(ma);
							if(files==null)files=new ArrayList<String>();
							files.add(path);
							multifile.put(ma, files);
						}
						else{
							a.fire(storage, path, client, xnjs);	
						}
						if(logger.isDebugEnabled()){
							logger.debug("Firing <"+r+"> on <"+path+"> for "+client.getDistinguishedName());
						}
					}
				}catch(Exception ex){
					Log.logException("Error firing <"+r+"> on <"+path+"> for "+client.getDistinguishedName(), ex, logger);
				}
			}
			if(match)ts.incrementNumberOfFiles();
		}
		// run multifile actions
		for(Map.Entry<MultiFileAction, List<String>> e: multifile.entrySet()){
			MultiFileAction ma = e.getKey();
			List<String> files = e.getValue();
			try{
				ma.fire(storage, files, client, xnjs);
			}catch(Exception ex){
				Log.logException("Error firing <"+ma+"> for "+client.getDistinguishedName() , ex, logger);
			}
		}

		for(Rule r: rules){
			r.commit();
		}
		time=System.currentTimeMillis()-time;
		ts.setDuration(time);

		logger.debug("Finished trigger run for client <"+client.getDistinguishedName()+"> "+ts.toString());
		if(usage.isInfoEnabled() && ts.getNumberOfFiles()>0){
			usage.info("Finished trigger run for client <"+client.getDistinguishedName()+"> "+ts.toString());
		}
		return ts;
	}

	public IStorageAdapter getStorage() {
		return storage;
	}

	public Client getClient() {
		return client;
	}

	public XNJS getXNJS() {
		return xnjs;
	}



}
