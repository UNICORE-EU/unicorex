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
 ********************************************************************************/

package de.fzj.unicore.uas.impl.job.ws;

import java.math.BigInteger;

import org.unigrids.services.atomic.types.StatusInfoDocument;
import org.unigrids.services.atomic.types.StatusType;
import org.unigrids.services.atomic.types.StatusType.Enum;

import de.fzj.unicore.uas.impl.UASWSResourceImpl;
import de.fzj.unicore.uas.impl.job.JobManagementImpl;
import de.fzj.unicore.uas.xnjs.XNJSFacade;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.security.util.AuthZAttributeStore;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.ValueRenderer;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ActionResult;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import eu.unicore.security.Client;

/**
 * renders the job status
 */
public class StatusInfoResourceProperty extends ValueRenderer {

	public StatusInfoResourceProperty(JobManagementImpl parent){
		super(parent, StatusInfoDocument.type.getDocumentElementName());
	}

	@Override
	protected StatusInfoDocument getValue() throws Exception{
		Kernel k=parent.getKernel();
		String xnjsReference=((UASWSResourceImpl)parent).getXNJSReference();
		String actionID=parent.getUniqueID();
		Client client=AuthZAttributeStore.getClient();
		XNJSFacade xnjs=XNJSFacade.get(xnjsReference,k);
		Action a=((JobManagementImpl)parent).getXNJSAction();
		ActionResult result=a.getResult();
		StatusInfoDocument status=StatusInfoDocument.Factory.newInstance();
		status.addNewStatusInfo().setStatus(convertStatus(a.getStatus(),result.isSuccessful()));
		status.getStatusInfo().setDescription("");
		Integer exitCode=xnjs.getExitCode(actionID,client);
		if(exitCode!=null){
			status.getStatusInfo().setExitCode(BigInteger.valueOf(exitCode.longValue()));
		}
		Float progress=xnjs.getProgress(actionID,client);
		if(progress!=null){
			status.getStatusInfo().setProgress(progress);
		}
		if(!result.isSuccessful()){
			String errorMessage=result.getErrorMessage();
			if(errorMessage==null)errorMessage="";
			status.getStatusInfo().setDescription(errorMessage);
		}
		return status;
	}
	
	/**
	 * converts from the XNJS action status to Unicore job status
	 * 
	 * states from unigridsTypes.xsd
	 * 
	 * <xsd:simpleType name="StatusType">
	 <xsd:restriction base="xsd:string">
	 <xsd:enumeration value="UNDEFINED"/>
	 <xsd:enumeration value="READY"/>
	 <xsd:enumeration value="QUEUED"/>
	 <xsd:enumeration value="RUNNING"/>
	 <xsd:enumeration value="SUCCESSFUL"/>
	 <xsd:enumeration value="FAILED"/>
	 <xsd:enumeration value="STAGINGIN"/>
	 <xsd:enumeration value="STAGINGOUT"/>
	 </xsd:restriction>
	 </xsd:simpleType>
	 *
	 * @param emsStatus
	 * @return UNICORE status
	 */
	public static Enum convertStatus(Integer emsStatus, boolean successful){
		int i=emsStatus.intValue(); 
		switch (i){
			case ActionStatus.PREPROCESSING: 
				return StatusType.STAGINGIN;
			case ActionStatus.POSTPROCESSING: 
				return StatusType.STAGINGOUT;
			case ActionStatus.RUNNING: 
				return StatusType.RUNNING;
			case ActionStatus.PENDING:
				return StatusType.QUEUED;
			case ActionStatus.QUEUED:
				return StatusType.QUEUED;
			case ActionStatus.READY: 
				return StatusType.READY;
			case ActionStatus.DONE:
				if(successful){
					return StatusType.SUCCESSFUL;
				}
				else{
					return StatusType.FAILED;
				}
			default:
				return StatusType.UNDEFINED;
		}
	}
	
}
