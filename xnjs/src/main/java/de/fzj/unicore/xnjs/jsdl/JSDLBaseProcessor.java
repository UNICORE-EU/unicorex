package de.fzj.unicore.xnjs.jsdl;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobNameDocument;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.ems.ProcessingException;
import de.fzj.unicore.xnjs.ems.processors.JobProcessor;
import de.fzj.unicore.xnjs.io.DataStageInInfo;
import de.fzj.unicore.xnjs.io.DataStageOutInfo;
import de.fzj.unicore.xnjs.resources.ResourceRequest;
import de.fzj.unicore.xnjs.util.ScriptEvaluator;
import de.fzj.unicore.xnjs.util.XmlBeansUtils;
import eu.unicore.util.Log;

/**
 * common JSDL stuff like parsing stage-ins 
 * 
 * @author schuller
 */
public abstract class JSDLBaseProcessor extends JobProcessor<JobDefinitionDocument> {

	protected final JSDLParser jsdlParser;
	
	public JSDLBaseProcessor(XNJS xnjs){
		super(xnjs);
		this.jsdlParser = new JSDLParser();
	}
	
	@Override
	protected void setupNotifications() {
		try {
			action.setNotificationURLs(JSDLParser.extractNotificationURLs(getJobDescriptionDocument()));
		}catch(Exception ex) {}
	}
	
	@Override
	protected boolean isEmptyJob() {
		JobDefinitionDocument jdd=getJobDescriptionDocument();
		return jdd==null || jdd.getJobDefinition()==null || jdd.getJobDefinition().getJobDescription()==null;
	}

	@Override
	protected String getJobName() {
		try{
			return XmlBeansUtils.getElementText(getJobDescriptionDocument(), JobNameDocument.type.getDocumentElementName());
		}catch(Exception e){
			return null;
		}
	}

	@Override
	protected List<DataStageInInfo> extractStageInInfo()throws Exception {
		if(action.getStageIns()==null){
			JobDefinitionDocument jd=(JobDefinitionDocument)action.getAjd();
			action.setStageIns(jsdlParser.parseImports(jd));
			action.setDirty();
		}
		return action.getStageIns();
	}

	@Override
	protected List<DataStageOutInfo> extractStageOutInfo()throws Exception {
		if(action.getStageOuts()==null){
			JobDefinitionDocument jd=(JobDefinitionDocument)action.getAjd();
			action.setStageOuts(jsdlParser.parseExports(jd));
			action.setDirty();
		}
		return action.getStageOuts();
	}

	@Override
	protected boolean hasStageIn() {
		return JSDLUtils.hasStageIn(getJobDescriptionDocument());
	}

	@Override
	protected boolean hasStageOut() {
		return JSDLUtils.hasStageOut(getJobDescriptionDocument());
	}

	protected List<ResourceRequest>extractRequestedResources() throws ExecutionException {
		List<ResourceRequest> resourceRequest = jsdlParser.parseRequestedResources(getJobDescriptionDocument());
		
		// evaluate any variables now
		ScriptEvaluator eval = null;
		for(ResourceRequest r: resourceRequest){
			if(ScriptEvaluator.isScript(r.getRequestedValue())){
				if(eval==null)eval=new ScriptEvaluator();
				String script = ScriptEvaluator.extractScript(r.getRequestedValue());
				try{
					Map<String,String>env = action.getExecutionContext().getEnvironment();
					String newValue = eval.evaluateToString(script, env);
					action.addLogTrace("Evaluated resource "+r.getName()+" as "+newValue);
					r.setRequestedValue(newValue);
				}catch(Exception e){
					String msg = Log.createFaultMessage("Could not evaluate script <"+script+">"
							+" for resource <"+r.getName()+">", e);
					throw new ExecutionException(msg);
				}
			}
		}
		
		return resourceRequest;
	}

	@Override
	protected void extractNotBefore() throws ProcessingException {
		String notBefore = JSDLParser.extractNotBefore(getJobDescriptionDocument());
		if(notBefore!=null){
			try{
				Date date=JSDLUtils.getDateFormat().parse(notBefore);
				action.setNotBefore(date.getTime());
			}catch(Exception ex) {
				throw new ProcessingException("Could not parse start time from <"+notBefore+">", ex);
			}
		}
		
	}

}
