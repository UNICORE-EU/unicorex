package de.fzj.unicore.xnjs.tsi.remote;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.tsi.TSIUnavailableException;
import de.fzj.unicore.xnjs.tsi.remote.Execution.BSSInfo;
import de.fzj.unicore.xnjs.tsi.remote.Execution.BSSSummary;

public interface IBSSState {

	public void toggleStatusUpdates(boolean enable);

	public Map<String, BSSInfo> getBSSInfo();

	public boolean lock() throws InterruptedException;
	
	public void unlock();
	
	public Set<String> getProcessList(String tsiNode)throws IOException, TSIUnavailableException, ExecutionException;

	public BSSSummary getBSSSummary();
	
	public BSSInfo getBSSInfo(String bssid);
	
	public void putBSSInfo(BSSInfo info);
	
	public void removeBSSInfo(String bssID);

}