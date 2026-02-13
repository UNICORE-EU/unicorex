package eu.unicore.uas.fts.http;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import eu.unicore.services.Kernel;

/**
 * holds transferred bytes for the HTTP transfers
 *
 * @author schuller
 */
public class FileAccessStatus {

	private final Map<String,Long> transferredBytes = new ConcurrentHashMap<>();

	public static synchronized void initialise(Kernel kernel){
		if(kernel.getAttribute(FileAccessStatus.class)!=null)return;
		FileAccessStatus fs = new FileAccessStatus();
		kernel.setAttribute(FileAccessStatus.class, fs);
	}

	public void cleanup(String id){
		transferredBytes.remove(id);
	}

	public Long getTransferredBytes(String id){
		return transferredBytes.get(id);
	}

	public void setTransferredBytes(String id, Long transferred){
		transferredBytes.put(id,transferred);
	}

}
