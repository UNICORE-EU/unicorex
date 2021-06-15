package de.fzj.unicore.uas.metadata;

import java.math.BigInteger;
import java.util.concurrent.Future;

import org.apache.xmlbeans.XmlObject;
import org.unigrids.x2006.x04.services.metadata.ExtractionStatisticsDocument;

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
	protected XmlObject createResultXML(ExtractionStatistics stats) {
		ExtractionStatisticsDocument sd=ExtractionStatisticsDocument.Factory.newInstance();
		sd.addNewExtractionStatistics().setDocumentsProcessed(BigInteger.valueOf(stats.getDocumentsProcessed()));
		sd.getExtractionStatistics().setDurationMillis(BigInteger.valueOf(stats.getDurationMillis()));
		return sd;
	}
	
}
