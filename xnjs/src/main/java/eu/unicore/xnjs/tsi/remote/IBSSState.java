package eu.unicore.xnjs.tsi.remote;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.tsi.TSIUnavailableException;
import eu.unicore.xnjs.tsi.remote.Execution.BSSInfo;
import eu.unicore.xnjs.tsi.remote.Execution.BSSSummary;

public interface IBSSState {

	public void init();

	public void toggleStatusUpdates(boolean enable);

	public Lock getBSSLock();
	
	public Lock getNodeLock(String node);
	
	public Set<String> getProcessList(String tsiNode)throws IOException, TSIUnavailableException, ExecutionException;

	public BSSSummary getBSSSummary();
	
	public BSSInfo getBSSInfo(String bssid);
	
	public void putBSSInfo(BSSInfo info);
	
	public void removeBSSInfo(String bssID);

}