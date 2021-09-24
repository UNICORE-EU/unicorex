package de.fzj.unicore.uas.metadata;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import de.fzj.unicore.uas.impl.task.TaskWatcher;
import eu.unicore.services.Kernel;

/**
 * monitors metadata extraction
 *
 * @author schuller
 */
public class ExtractionWatcher extends TaskWatcher<ExtractionStatistics>{

	public ExtractionWatcher(Future<ExtractionStatistics>future, String taskID, Kernel kernel){
		super(future, taskID, kernel);
	}
	
	@Override
	protected Map<String,String> createResult(ExtractionStatistics stats) {
		Map<String,String> res = new HashMap<>();
		res.put("documentsProcessed", String.valueOf(stats.getDocumentsProcessed()));
		res.put("durationMillis", String.valueOf(stats.getDurationMillis()));
		return res;
	}
	
}
