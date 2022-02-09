package de.fzj.unicore.xnjs.tsi.remote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
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

	private final Map<String,BSSInfo> bssInfo = new ConcurrentHashMap<>();

	@Inject
	private TSIConnectionFactory connectionFactory;

	@Inject
	private TSIProperties tsiProperties;

	@Inject
	private XNJS xnjs;

	@Inject
	private IDB idb;

	@Inject
	private TSIMessages tsiMessages;

	@Inject
	private InternalManager eventHandler;

	private volatile BSSSummary summary = new BSSSummary();

	private final AtomicBoolean statusUpdatesEnabled = new AtomicBoolean(false);

	/**
	 * these locks protects against a race condition when submitting a job. Need to
	 * make sure that no status updates are run at the same time.
	 * For better scalability, we use one lock per TSI node (for interactive jobs)
	 * plus one lock for batch jobs
	 */
	private final Map<String, ReentrantLock> perNodeLocks = new HashMap<>();

	private final ReentrantLock bssLock = new ReentrantLock(true);
	
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

	private boolean haveInit = false;
	
	@Override
	public synchronized void init() {
		if(haveInit)return;
		haveInit = true;
		updateConfigParameters();
		//schedule BSS status update
		Runnable r=new Runnable(){
			public void run(){
				try{
					if(statusUpdatesEnabled.get()) {
						updateBSSStates();
					}
					updateConfigParameters();
				}catch(Throwable e){
					Log.logException("Problem updating BSS state", e, log);
				}
				xnjs.getScheduledExecutor().schedule(this, updateInterval, TimeUnit.MILLISECONDS);
			}
		};
		xnjs.getScheduledExecutor().schedule(r, updateInterval, TimeUnit.MILLISECONDS);
	}

	private synchronized ReentrantLock getOrCreateLock(String tsiHost) {
		ReentrantLock lock = perNodeLocks.get(tsiHost);
		if(lock==null) {
			lock = new ReentrantLock(true);
			perNodeLocks.put(tsiHost, lock);
		}
		return lock;
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
		List<BSSSummary> parts = new ArrayList<>();
		
		// BSS jobs
		boolean bssLocked = false;
		try {
			bssLocked = bssLock.tryLock(120, TimeUnit.SECONDS);
			if(bssLocked) {
				String res = null;
				try(TSIConnection conn = connectionFactory.getTSIConnection(tsiProperties.getBSSUser(),"NONE", null, timeout)){
					res = conn.send(TSIUtils.makeStatusCommand(null));
					log.trace("BSS Status listing: \n{}", res);
				}
				parts.add(updateBatchJobStates(bssInfo, res, eventHandler));
			}else {
				log.error("Can't get BSS status listing: can't acquire lock (timeout)");
			}
		}
		catch(TSIUnavailableException tue) {
			if(System.currentTimeMillis()>lastLoggedTSIFailure+interval) {
				Log.logException("TSI is not available.",tue, log);
				lastLoggedTSIFailure = System.currentTimeMillis();
			}
		}catch(Exception ex) {
			Log.logException("Error updating batch job states", ex, log);
		}finally{
			if(bssLocked)bssLock.unlock();
		}

		// interactive processes on all our TSI hosts
		for(String tsiNode: connectionFactory.getTSIHosts()){
			Lock lock = getOrCreateLock(tsiNode);
			boolean locked = false;
			try {
				locked = lock.tryLock(120, TimeUnit.SECONDS);
				if(!locked) {
					log.error("Can't get process list from ["+tsiNode+"]: can't acquire lock (timeout)");
					continue;
				}	
				Set<String>pids = new HashSet<>();
				pids.addAll(getProcessList(tsiNode));
				parts.add(updateInteractiveJobs(bssInfo, tsiNode, pids, eventHandler));
				tsiNodeStates.put(tsiNode, Boolean.TRUE);
			}catch(TSIUnavailableException tue) {
				Boolean oldState = tsiNodeStates.getOrDefault(tsiNode, Boolean.TRUE);
				if(Boolean.TRUE.equals(oldState)) {
					Log.logException("Can't get process list from ["+tsiNode+"]", tue, log);
				}
				tsiNodeStates.put(tsiNode, Boolean.FALSE);
			}catch(Exception te){
				Log.logException("Can't updating job states on ["+tsiNode+"]", te, log);
			}finally {
				if(locked)lock.unlock();
			}
		}
		summary = new BSSSummary(parts);
	}
	
	public static BSSSummary updateBatchJobStates(final Map<String, BSSInfo> statesMap, String tsiReply, EventHandler handler)
	throws IOException {
		int running=0;
		int queued=0;
		int total=0;
		/*
		 * Format used by TSI:
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

		Set<String> bssIDs = new HashSet<>();
		bssIDs.addAll(statesMap.keySet());
		Map<String,Integer> queueFill = new HashMap<>();
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
			} else {
				String bssID = tok[0].trim();
				BSS_STATE newValue = null;
				try {
					newValue = BSS_STATE.valueOf(tok[1].trim());
				}catch(Exception ex) {
					throw new IOException("Unexpected status <"+tok[1]+"> Wrong format of QSTAT! Please check the TSI!");
				}
				// track some stats
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

				// track per-queue info if available
				String queue=null;
				if(active && tok.length>2){
					queue=tok[2];
					Integer fill=queueFill.get(queue);
					if(fill==null){
						fill = Integer.valueOf(0);
					}
					fill++;
					queueFill.put(queue,fill);
				}

				BSSInfo info=statesMap.get(bssID);
				if(info==null){
					continue;
				}
				BSS_STATE oldValue = info.bssState;
				String jobID = info.jobID;
				info.queue=queue;
				if (!newValue.equals(oldValue)) {
					info.bssState = newValue;
					if (handler != null) {
						try {
							log.debug("BSS status changed: {} -> {}, sending 'continue' for: {}",
									oldValue, newValue, jobID);
							handler.handleEvent(new ContinueProcessingEvent(jobID));
						} catch (Exception ee) {
							LogUtil.logException("Error sending change event",ee,log);
						}
					}
				}
				bssIDs.remove(bssID);
			}
		}

		// now check batch entries that do not have a new state
		// (ignoring processes running on the login nodes)
		for (String s : bssIDs) {
			BSSInfo info = statesMap.get(s);

			if(info.bssID.startsWith("INTERACTIVE_"))
				continue;

			info.bssState = BSS_STATE.CHECKING_FOR_EXIT_CODE;
			try {
				if (handler != null) {
					String uuid = info.jobID;
					if (uuid != null) {
						log.debug("Entry {} disappeared from QSTAT listing, sending 'continue' for uuid={}", s, uuid);
						handler.handleEvent(new ContinueProcessingEvent(uuid));
					}
				}
			} catch (Exception ee) {
				LogUtil.logException("Internal error updating status, bssID="+s,ee,log);
			}
		}
		return new BSSSummary(running,queued,total,queueFill);
	}

	public static BSSSummary updateInteractiveJobs(final Map<String, BSSInfo> statesMap, 
			String tsiNode, Collection<String>interactiveProcesses, EventHandler handler)throws IOException {
		int running=0;
		int queued=0;
		int total=0;

		Set<String> bssIDs = new HashSet<>();
		bssIDs.addAll(statesMap.keySet());
		Map<String,Integer> queueFill = new HashMap<>();

		// only check processes running on the given login node
		String marker = "INTERACTIVE_"+tsiNode+"_";
		
		for (String s : bssIDs) {
			BSSInfo info = statesMap.get(s);

			if(!info.bssID.startsWith(marker))continue;
			
			if(interactiveProcesses.contains(s)){
				// make sure status is RUNNING to try 
				// and recover in case of a transient error
				info.bssState = BSS_STATE.RUNNING;
				continue;
			}
			info.bssState = BSS_STATE.CHECKING_FOR_EXIT_CODE;
			try {
				if (handler != null) {
					String uuid = info.jobID;
					if (uuid != null) {
						log.debug("Entry {} disappeared from process list, sending 'continue' for uuid={}", s, uuid);
						handler.handleEvent(new ContinueProcessingEvent(uuid));
					}
				}
			} catch (Exception ee) {
				LogUtil.logException("Internal error updating status, bssID="+s,ee,log);
			}
		}
		return new BSSSummary(running,queued,total,queueFill);
	}

	/**
	 * execute a PS on the given TSI node and return a list of PIDs
	 * 
	 * @param tsiNode
	 * @throws IOException
	 * @throws TSIUnavailableException
	 */
	public Set<String> getProcessList(String tsiNode)throws IOException, TSIUnavailableException {
		Set<String>result = new HashSet<>();
		try(TSIConnection conn = connectionFactory.getTSIConnection(tsiProperties.getBSSUser(),"NONE", tsiNode, timeout)){
			String res = doGetProcessListing(conn);
			log.trace("Process listing on [{}]: \n{}", tsiNode, res);
			if(res==null || !res.startsWith("TSI_OK")){
				String msg = "Cannot retrieve process list. TSI reply: "+res;
				// if this does not work, something is wrong with the TSI node
				conn.markTSINodeUnavailable(msg);
				conn.shutdown();
				throw new TSIUnavailableException("TSI on "+tsiNode+": "+ msg);
			}
			result.addAll(parseTSIProcessList(res, tsiNode));
		}
		return result;
	}


	private static final Pattern psPattern=Pattern.compile("\\s*(\\d+)\\s*.*");

	public static Set<String> parseTSIProcessList(String processList, String tsiNode) throws IOException {
		Set<String>result = new HashSet<>();
		BufferedReader br = new BufferedReader(new StringReader(processList.trim()+"\n"));
		String line=null;
		while(true){
			line=br.readLine();
			if(line==null)break;
			Matcher m=psPattern.matcher(line);
			if(!m.matches())continue;
			result.add("INTERACTIVE_"+tsiNode+"_"+String.valueOf(m.group(1)));
		}
		return result;
	}

	public Lock getBSSLock() {
		return bssLock;
	}

	public Lock getNodeLock(String tsiHost) {
		return getOrCreateLock(tsiHost);
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

	private String doGetProcessListing(TSIConnection tsiConnection) throws IOException {
		String res = null;
		if(tsiConnection.compareVersion("8.3.0")) {
			res = tsiConnection.send(TSIUtils.makeGetProcessListCommand());
		}
		else {
			String script = tsiProperties.getValue(TSIProperties.BSS_PS);
			res = tsiConnection.send(TSIUtils.makeExecuteScript(script, null, idb, null));
		}
		return res;
	}

}
