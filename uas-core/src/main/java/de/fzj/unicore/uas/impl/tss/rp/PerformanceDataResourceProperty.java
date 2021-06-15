package de.fzj.unicore.uas.impl.tss.rp;

import java.math.BigInteger;
import java.util.Map;

import org.unigrids.x2006.x04.services.tsf.PerformanceDataDocument;
import org.unigrids.x2006.x04.services.tsf.QueueInfoDocument.QueueInfo;

import com.codahale.metrics.Histogram;

import de.fzj.unicore.uas.impl.BaseResourceImpl;
import de.fzj.unicore.uas.xnjs.XNJSFacade;
import de.fzj.unicore.xnjs.XNJSConstants;
import eu.unicore.services.ws.renderers.ValueRenderer;

/**
 * publishes performance data
 *
 * @author schuller
 * @since 1.4.0
 */
public class PerformanceDataResourceProperty extends ValueRenderer {
	
	BaseResourceImpl parent;
	
	public PerformanceDataResourceProperty(BaseResourceImpl parent) {
		super(parent, PerformanceDataDocument.type.getDocumentElementName());
		this.parent=parent;
	}

	@Override
	protected PerformanceDataDocument getValue()throws Exception {
		XNJSFacade f=parent.getXNJSFacade();
		Histogram stats = f.getXNJS().getMetricRegistry().getHistograms().get(XNJSConstants.MEAN_TIME_QUEUED);
		double mtq=stats.getSnapshot().getMean();
		PerformanceDataDocument res=PerformanceDataDocument.Factory.newInstance();
		res.addNewPerformanceData();
		res.getPerformanceData().setMeanTimeQueued(BigInteger.valueOf((int)mtq));
		res.getPerformanceData().setTSSType("DEFAULT");
		res.getPerformanceData().setQueueInfoArray(new QueueInfo[0]);
		Map<String,Integer>queueFill = f.getQueueFill();
		if(queueFill!=null && queueFill.size()>0){
			for(Map.Entry<String, Integer>e: queueFill.entrySet()){
				QueueInfo qi=res.getPerformanceData().addNewQueueInfo();
				qi.setQueueName(e.getKey());
				qi.setActiveJobs(BigInteger.valueOf(e.getValue()));
			}
		}
		return res;
	}
	
}
