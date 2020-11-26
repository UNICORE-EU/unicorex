/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *********************************************************************************/


package de.fzj.unicore.xnjs.jsdl;

import java.util.List;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobIdentificationType;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.ems.ProcessingException;
import de.fzj.unicore.xnjs.idb.ApplicationInfo;
import de.fzj.unicore.xnjs.idb.Incarnation;
import de.fzj.unicore.xnjs.resources.ResourceRequest;
import de.fzj.unicore.xnjs.resources.ResourceSet;
import eu.unicore.security.Client;

/**
 * processor for JSDL actions
 * 
 * @author schuller
 */
public class JSDLProcessor extends JSDLBaseProcessor {

	/**
	 * default constructor
	 */
	public JSDLProcessor(XNJS xnjs){
		super(xnjs);
	}
	
	/**
	 * if the job is a parameter sweep job, change the action type
	 * so that the {@link SweepProcessor} can take over
	 */
	@Override
	protected void handleCreated() throws ProcessingException {
		try{
			if(JSDLUtils.hasSweep(getJobDescriptionDocument())){
				action.setType(SweepProcessor.sweepActionType);
				action.addLogTrace("This is a JSDL ParameterSweep job, changing type to '"
						+SweepProcessor.sweepActionType+"'");
			}
			else{
				super.handleCreated();
			}	
		}catch(Exception ex){
			throw new ProcessingException(ex);
		}
	}


	/**
	 * this method extracts JSDL specific info like {@link ApplicationInfo} from
	 * the job description and fills the proper fields in the current action
	 * 
	 * @throws Exception
	 */
	@Override
	protected void extractFromJobDescription()throws ExecutionException{
		Incarnation grounder = xnjs.get(Incarnation.class);
		Client client=action.getClient();
		ecm.getContext(action);
		try{
			//do an incarnation now...
			JobDefinitionDocument jd=(JobDefinitionDocument)action.getAjd();
			ApplicationInfo orig=new JSDLParser().parseApplicationInfo(jd);
			ApplicationInfo applicationInfo=grounder.incarnateApplication(orig,client);
			action.setApplicationInfo(applicationInfo);
			updateExecutionContext(applicationInfo);

			// resources
			List<ResourceRequest>resourceRequest = extractRequestedResources();
			action.getExecutionContext().setResourceRequest(resourceRequest);
			
			//job name
			JobIdentificationType ji=jd.getJobDefinition().getJobDescription().getJobIdentification();
			if(ji!=null){
				String jobName=ji.getJobName();
				action.setJobName(jobName);
				//project
				String[]requestedProjects=ji.getJobProjectArray();
				if(requestedProjects.length>0){
					ResourceRequest projectRequest = ResourceRequest.find(resourceRequest, ResourceSet.PROJECT);
					String project=requestedProjects[0];
					if(!ResourceRequest.contains(resourceRequest, ResourceSet.PROJECT)){
						projectRequest=new ResourceRequest(ResourceSet.PROJECT, project);
						resourceRequest.add(projectRequest);
					}
					else{
						projectRequest.setRequestedValue(project);
					}
				}
				if(requestedProjects.length>1){
					action.addLogTrace("Warning: extra projects are ignored!");
				}
			}
			
			String email=null;
			//get user email from job annotation
			try{
				email = JSDLParser.extractEmail(jd);
			}catch(Exception e){}
			Client c=action.getClient();
			if(c!=null && email!=null)c.setUserEmail(email);
			
			extractStageInInfo();
			extractStageOutInfo();
			
			action.setDirty();

		} catch (Exception e) {
			if(e instanceof ExecutionException){
				throw (ExecutionException)e;
			}
			else{
				throw new ExecutionException(e);
			}
		}
	}

}
