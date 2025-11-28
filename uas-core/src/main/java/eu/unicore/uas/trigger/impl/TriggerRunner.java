package eu.unicore.uas.trigger.impl;

import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.Logger;

import eu.unicore.security.Client;
import eu.unicore.uas.trigger.MultiFileAction;
import eu.unicore.uas.trigger.Rule;
import eu.unicore.uas.trigger.SingleFileAction;
import eu.unicore.uas.trigger.TriggeredAction;
import eu.unicore.uas.util.LogUtil;
import eu.unicore.util.Log;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.xnjs.io.XnjsFile;

public class TriggerRunner implements Callable<TriggerStatistics>, TriggerContext {

	private static final Logger logger = LogUtil.getLogger(LogUtil.TRIGGER, TriggerRunner.class);

	private static final Logger usage = LogUtil.getLogger(LogUtil.TRIGGER+".USAGE", TriggerRunner.class);

	private final List<XnjsFile> files;

	private final List<Rule> rules;

	private final IStorageAdapter storage;

	private final Client client;

	private final XNJS xnjs;

	private final String logDirectory;

	public TriggerRunner(List<XnjsFile> files, List<Rule> rules, IStorageAdapter storage, Client client, XNJS xnjs, String logDirectory){
		this.files=files;
		this.rules=rules;
		this.storage=storage;
		this.client=client;
		this.xnjs=xnjs;
		this.logDirectory = logDirectory;
	}

	/**
	 * checks all files and (sequentially) invoke all the matching rules
	 */
	@Override
	public TriggerStatistics call() {
		TriggerStatistics ts = new TriggerStatistics();
		boolean logging = logDirectory!=null;
		List<String>log = new ArrayList<>();
		long time=System.currentTimeMillis();
		for(Rule r: rules){
			r.begin();
		}
		Map<MultiFileAction, List<String>>multifile = new HashMap<>();
		for(XnjsFile file: files){
			String path=file.getPath();
			if(logging && path.startsWith(logDirectory)){
				// be paranoid and skip it
				continue;
			}
			boolean match=false;
			for(Rule r: rules){
				try{
					if(r.matches(path, this)){
						match=true;
						ts.ruleInvoked(r.getName());
						TriggeredAction<?> a = r.getAction();
						if(a instanceof MultiFileAction){
							MultiFileAction ma=(MultiFileAction)a;
							List<String>files=multifile.get(ma);
							if(files==null)files = new ArrayList<>();
							files.add(path);
							multifile.put(ma, files);
						}
						else if(a instanceof SingleFileAction){
							SingleFileAction sfa=(SingleFileAction)a;
							sfa.setTarget(path);
							String id = a.run(storage, client, xnjs);	
							ts.addAction(id);
						}
						else throw new IllegalStateException("not implementeed: "+a.getClass());
						logger.debug("Running <{}> on <{}> for <{}>",r, path, client.getDistinguishedName());
						if(logging)log.add("Running <"+r+"> on <"+path+">");
					}
				}catch(Exception ex){
					Log.logException("Error running <"+r+"> on <"+path+"> for <"+client.getDistinguishedName()+">", ex, logger);
					if(logging)log.add(Log.createFaultMessage("ERROR running <"+r+"> on <"+path+">", ex));
				}
			}
			if(match)ts.incrementNumberOfFiles();
		}
		// run multifile actions
		for(Map.Entry<MultiFileAction, List<String>> e: multifile.entrySet()){
			MultiFileAction ma = e.getKey();
			List<String> files = e.getValue();
			try{
				if(logging)log.add("Running <"+ma+"> on <"+files+">");
				ma.setTarget(files);
				String id = ma.run(storage, client, xnjs);
				ts.addAction(id);
			}catch(Exception ex){
				Log.logException("Error running <"+ma+"> for "+client.getDistinguishedName() , ex, logger);
				if(logging)log.add(Log.createFaultMessage("ERROR running <"+ma+"> on <"+files+">", ex));
			}
		}

		for(Rule r: rules){
			r.commit();
		}
		time=System.currentTimeMillis()-time;
		ts.setDuration(time);

		logger.debug("Finished trigger run for <{}>. {}",client.getDistinguishedName(), ts.toString());
		if(ts.getNumberOfFiles()>0){
			usage.info("Finished trigger run for <{}>. {}", client.getDistinguishedName(), ts);
			log.add("Finished trigger run. "+ts);
		}
		try{
			if(logging)storeLog(log);
		}catch(Exception e) {
			Log.logException("Error storing trigger run log for "+client.getDistinguishedName(), e, logger);
		}
		return ts;
	}

	@Override
	public IStorageAdapter getStorage() {
		return storage;
	}

	@Override
	public Client getClient() {
		return client;
	}

	@Override
	public XNJS getXNJS() {
		return xnjs;
	}

	private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");

	private void storeLog(List<String>log) throws Exception {
		if(log.size()==0)return;
		storage.mkdir(logDirectory);
		String fName = logDirectory+"/"+"run-"+df.format(new Date())+".log";
		try(OutputStreamWriter os = new OutputStreamWriter(storage.getOutputStream(fName))) {
			for(String l: log) {
				os.write(l+"\n");
			}
		}
	}

}
