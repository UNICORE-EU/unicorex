package de.fzj.unicore.xnjs.tsi.remote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.ems.InternalManager;
import de.fzj.unicore.xnjs.ems.event.ContinueProcessingEvent;
import de.fzj.unicore.xnjs.ems.event.EventHandler;
import de.fzj.unicore.xnjs.idb.IDB;
import de.fzj.unicore.xnjs.tsi.TSIUnavailableException;
import de.fzj.unicore.xnjs.tsi.remote.Execution.BSSInfo;
import de.fzj.unicore.xnjs.tsi.remote.Execution.BSSSummary;
import de.fzj.unicore.xnjs.tsi.remote.Execution.BSS_STATE;
import de.fzj.unicore.xnjs.util.LogUtil;
import eu.unicore.util.Log;

@Singleton
public class BSSState implements IBSSState {

	private static final Logger log = LogUtil.getLogger(LogUtil.JOBS, BSSState.class);

	private final Map<String,BSSInfo> bssInfo = new ConcurrentHashMap<String, BSSInfo>();

	private TSIConnectionFactory connectionFactory;

	private TSIProperties tsiProperties;

	private IDB idb;

	private InternalManager eventHandler;

	private volatile BSSSummary summary = new BSSSummary();

	private final AtomicBoolean statusUpdatesEnabled = new AtomicBoolean(false);

	/**
	 * this lock protects against a race condition when submitting a job. Need to
	 * make sure that no status updates are run at the same time
	 */
	private final Lock jobSubmissionInProgressLock = new ReentrantLock(true);

	// timeout waiting for a TSI connection (before creating a new one)
	static final int timeout = 10000;

	/**
	 * delay (milliseconds) between runs of the qstat query
	 */
	private int updateInterval = 30000;

	@Override
	public void toggleStatusUpdates(boolean enable) {
		statusUpdatesEnabled.set(enable);
	}

	private long lastLoggedTSIFailure = -1;
	private final long interval = 60*60*1000;

	@Inject
	public BSSState(XNJS xnjs, TSIConnectionFactory connectionFactory, InternalManager eventHandler, IDB idb, TSIProperties tsiProperties) {
		this.connectionFactory = connectionFactory;
		this.eventHandler = eventHandler;
		this.idb = idb;
		this.tsiProperties = tsiProperties;

		updateConfigParameters();

		//schedule BSS status update
		Runnable r=new Runnable(){
			public void run(){
				try{
					if(statusUpdatesEnabled.get()) {
						updateBSSStates();
					}
					updateConfigParameters();
				}catch(TSIUnavailableException tue){
					if(System.currentTimeMillis()>lastLoggedTSIFailure+interval) {
						Log.logException("TSI is not available.",tue, log);
						lastLoggedTSIFailure = System.currentTimeMillis();
					}
				}catch(Throwable e){
					Log.logException("Problem updating BSS state", e, log);
				}
				xnjs.getScheduledExecutor().schedule(this, updateInterval, TimeUnit.MILLISECONDS);
			}
		};
		xnjs.getScheduledExecutor().schedule(r, updateInterval, TimeUnit.MILLISECONDS);
	}

	private void updateConfigParameters(){
		int newInterval = tsiProperties.getIntValue(TSIProperties.BSS_UPDATE_INTERVAL);
		if(newInterval!=updateInterval){
			updateInterval = newInterval;
			log.info("Batch system state will be queried every <"+updateInterval+"> milliseconds.");
		}
	}

	private final Map<String,Boolean>tsiNodeStates = new HashMap<>();

	//updates the status hashmap. Called periodically from a scheduler thread
	private void updateBSSStates() throws Exception {
		//do not update while job is being submitted!
		jobSubmissionInProgressLock.tryLock(120, TimeUnit.SECONDS);
		try {
			String res=null;
			try (TSIConnection conn = connectionFactory.getTSIConnection(tsiProperties.getBSSUser(),"NONE", null, timeout)){
				res = conn.send(TSIUtils.makeStatusCommand(null));
				if(log.isTraceEnabled()){
					log.trace("BSS Status listing: \n"+res);
				}
			}
			List<String>pids=new ArrayList<String>();
			for(String tsiNode: connectionFactory.getTSIHosts()){
				try{
					pids.addAll(getProcessList(tsiNode));
					tsiNodeStates.put(tsiNode, Boolean.TRUE);
				}catch(TSIUnavailableException tue) {
					Boolean oldState = tsiNodeStates.getOrDefault(tsiNode, Boolean.TRUE);
					if(Boolean.TRUE.equals(oldState)) {
						Log.logException("Can't get process list from ["+tsiNode+"]", tue, log);
					}
					tsiNodeStates.put(tsiNode, Boolean.FALSE);
				}catch(Exception te){
					Log.logException("Can't get process list from ["+tsiNode+"]", te, log);
				}
			}
			summary = updateStatusListing(bssInfo, res, pids, eventHandler);
		}
		finally{
			jobSubmissionInProgressLock.unlock();
		}
	}


	private static final Pattern psPattern=Pattern.compile("\\s?(\\d+)\\s*.*");

	/**
	 * execute a PS on the given TSI node and return a list of PIDs
	 * 
	 * @param tsiNode
	 * @throws IOException
	 * @throws TSIUnavailableException
	 */
	public Set<String> getProcessList(String tsiNode)throws IOException, TSIUnavailableException, ExecutionException {
		Set<String>result=new HashSet<String>();
		try(TSIConnection conn = connectionFactory.getTSIConnection(tsiProperties.getBSSUser(),"NONE", tsiNode, timeout)){
			String script=tsiProperties.getValue(TSIProperties.BSS_PS);
			String res=conn.send(TSIUtils.makeExecuteScript(script, null, idb, null));
			if(log.isTraceEnabled()){
				log.trace("Process listing on ["+tsiNode+"]: \n"+res);
			}
			if(res==null || !res.startsWith("TSI_OK")){
				String msg = "Cannot retrieve process list. TSI reply: "+res;
				// if this does not work, something is wrong with the TSI node
				conn.markTSINodeUnavailable(msg);
				conn.shutdown();
				throw new ExecutionException(msg);
			}
			BufferedReader br = new BufferedReader(new StringReader(res.trim()+"\n"));
			String line=null;
			while(true){
				line=br.readLine();
				if(line==null)break;
				Matcher m=psPattern.matcher(line);
				if(!m.matches())continue;
				result.add("INTERACTIVE_"+tsiNode+"_"+String.valueOf(m.group(1)));
			}
		}
		return result;
	}

	@Override
	public Map<String,BSSInfo> getBSSInfo(){
		return bssInfo;
	}

	public boolean lock() throws InterruptedException {
		return jobSubmissionInProgressLock.tryLock(120, TimeUnit.SECONDS);
	}

	public void unlock() {
		jobSubmissionInProgressLock.unlock();
	}

	public BSSSummary getBSSSummary() {
		return summary;
	}

	public BSSInfo getBSSInfo(String bssid) {
		return bssInfo.get(bssid);
	}

	public void putBSSInfo(BSSInfo info) {
		bssInfo.put(info.bssID, info);
	}

	public void removeBSSInfo(String bssid) {
		bssInfo.remove(bssid);
	}

	/**
	 * parse the status listing returned by TSI GetStatusListing and modify the
	 * map holding the states<br/> 
	 * 
	 * A {@link ContinueProcessingEvent} is
	 * generated for an action, if
	 * <ul>
	 * <li>a new state entry appears</li>
	 * <li>the state entry disappears</li>
	 * </ul>
	 * 
	 * @param statesMap - a map containing the states, with key=bssid, value=BSSInfo
	 * @param tsiReply - the TSI reply to a "get status listing" command
	 * @param interactiveProcesses - the list of PIDs on the TSI node(s)
	 * @param handler - an event handler for receiving change events (can be null)
	 * 
	 * @throws Exception
	 */
	public static BSSSummary updateStatusListing(Map<String, BSSInfo> statesMap, 
			String tsiReply, List<String>interactiveProcesses, EventHandler handler)throws IOException {
		int running=0;
		int queued=0;
		int total=0;
		/*
		 * From TSI GetStatusListing.pm: 
		 * 
		 * First line is "QSTAT", followed by a
		 * line per found job, first word is BSS job identifier and the second
		 * word is one of RUNNING, QUEUED, COMPLETED, SUSPENDED
		 * 
		 * Optionally a third word indicates the queue name (careful, since many batch systems
		 * do not list full queue names). Not all TSIs may support this.
		 */
		BufferedReader br = new BufferedReader(new StringReader(tsiReply.trim()+"\n"));
		String line = br.readLine();
		if (line == null)
			throw new IOException("Empty reply from TSI");
		line = line.trim();
		if (!line.equalsIgnoreCase("QSTAT")) {
			throw new IOException("No valid QSTAT listing received. TSI replied: " + line);
		}

		Set<String> bssIDs = new HashSet<String>();
		bssIDs.addAll(statesMap.keySet());
		Set<String> haveStatusForIDs = new HashSet<String>();
		Map<String,Integer> queueFill = new HashMap<String,Integer>();
		boolean active;

		String inner = "";
		while (inner != null) {
			inner = br.readLine();
			if (inner == null)
				break;
			String[] tok = inner.trim().split(" ");
			if (tok.length < 2) {
				String msg="Wrong format of QSTAT! Please check the TSI!";
				throw new IOException(msg);
			} else { // store in map
				String bssID = tok[0].trim();
				if(haveStatusForIDs.contains(bssID)){
					//duplicate entry in QSTAT
					continue;
				}
				BSS_STATE newValue = null;
				try {
					newValue = BSS_STATE.valueOf(tok[1].trim());
				}catch(Exception ex) {
					throw new IOException("Unexpected status <"+tok[1]+"> Wrong format of QSTAT! Please check the TSI!");
				}
				//track some stats
				total++;
				active=false;
				if(BSS_STATE.RUNNING.equals(newValue)){
					running++;
					active=true;
				}
				else if(BSS_STATE.QUEUED.equals(newValue)){
					queued++;
					active=true;
				}
				String queue=null;
				//track per-queue info if available
				if(active && tok.length>2){
					queue=tok[2];
					Integer fill=queueFill.get(queue);
					if(fill==null){
						fill = Integer.valueOf(0);
					}
					fill++;
					queueFill.put(queue,fill);
				}

				BSSInfo oldInfo=statesMap.get(bssID);
				if(oldInfo==null){
					continue;
				}
				BSS_STATE oldValue = oldInfo.bssState;
				String jobID = oldInfo.jobID;

				BSSInfo newState = new BSSInfo(bssID, jobID, newValue);
				newState.queue=queue;

				statesMap.put(bssID,newState);
				haveStatusForIDs.add(bssID);
				if (!newValue.equals(oldValue)) {
					if (handler != null) {
						try {
							if (log.isDebugEnabled()) {
								log.debug("BSS status changed: "+oldValue + " -> "
										+ newValue
										+ ", sending 'continue' for : "
										+ jobID);
							}
							handler.handleEvent(new ContinueProcessingEvent(jobID));

						} catch (Exception ee) {
							LogUtil.logException("Error sending change event",ee,log);
						}
					}
				}
				bssIDs.remove(bssID);
			}
		}

		// now check for entries that do not have a new state -> remove old state
		for (String s : bssIDs) {
			BSSInfo state=statesMap.get(s);

			if(interactiveProcesses.contains(s)){
				//still running, nothing needs to be done
				continue;
			}

			try {
				if (handler != null) {
					String uuid = state.jobID;
					if (uuid != null) {
						if (log.isDebugEnabled()) {
							log.debug("Entry '"+s+"' disappeared from QSTAT listing, sending 'continue' for uuid=" + uuid);
						}
						handler.handleEvent(new ContinueProcessingEvent(uuid));
					}
				}
				if(!BSS_STATE.COMPLETED.equals(state.bssState)){
					statesMap.remove(s);
				}
			} catch (Exception ee) {
				LogUtil.logException("Internal error updating status, bssID="+s,ee,log);
			}
		}

		return new BSSSummary(running,queued,total,queueFill);
	}

}
