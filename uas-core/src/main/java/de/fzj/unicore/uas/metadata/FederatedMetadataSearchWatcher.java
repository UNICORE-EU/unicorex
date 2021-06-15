package de.fzj.unicore.uas.metadata;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.xmlbeans.XmlObject;
import org.unigrids.x2006.x04.services.metadata.FederatedSearchResultCollectionDocument;

import de.fzj.unicore.uas.impl.task.TaskImpl;
import eu.unicore.services.Kernel;
import eu.unicore.util.Log;

public class FederatedMetadataSearchWatcher implements Runnable {

	private final String taskID;
	private final Future<FederatedSearchResultCollection> future;
	private final Kernel kernel;

	public FederatedMetadataSearchWatcher(
			Future<FederatedSearchResultCollection> future, String taskID,
			Kernel kernel) {
		this.taskID = taskID;
		this.future = future;
		this.kernel = kernel;
	}

	@Override
	public void run() {
		XmlObject result = null;

		if (future.isDone()) {
			try {
				FederatedSearchResultCollection searchResultCollection = future.get();
				
				FederatedSearchResultCollectionDocument searchResultCollectionDocument = searchResultCollection.getTypeOfDocument();
				
				result = searchResultCollectionDocument;
				TaskImpl.putResult(kernel, taskID, result, "OK", 0);
				
			} catch (Exception ex) {
				try{
					String msg=Log.createFaultMessage("Error: ", ex);
					TaskImpl.failTask(kernel, taskID, msg, 1);
				}catch(Exception ignored){}
			}
			
		} else {

			kernel.getContainerProperties().getThreadingServices()
					.getScheduledExecutorService()
					.schedule(this, 5000, TimeUnit.MILLISECONDS);
		}
	}
}
