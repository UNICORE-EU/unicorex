/*********************************************************************************
 * Copyright (c) 2013 Forschungszentrum Juelich GmbH 
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

import java.util.Calendar;

import org.unigrids.x2006.x04.services.jms.EstimatedEndTimeDocument;

import de.fzj.unicore.uas.impl.job.JobManagementImpl;
import eu.unicore.services.ws.renderers.ValueRenderer;

/**
 * renders the job's estimated end time (which is retrieved from the XNJS Action)
 */
public class EstimatedEndtimeRenderer extends ValueRenderer {
	
	public EstimatedEndtimeRenderer(JobManagementImpl parent){
		super(parent, EstimatedEndTimeDocument.type.getDocumentElementName());
	}

	protected EstimatedEndTimeDocument getValue() {
		EstimatedEndTimeDocument eet = null;
		long time=((JobManagementImpl)parent).getXNJSAction().getExecutionContext().getEstimatedEndtime();
		if(time>0){
			eet=EstimatedEndTimeDocument.Factory.newInstance();
			Calendar estimatedEndtime=Calendar.getInstance();
			estimatedEndtime.setTimeInMillis(time);
			eet.setEstimatedEndTime(estimatedEndtime);
		}
		return eet;
	}
	
}
