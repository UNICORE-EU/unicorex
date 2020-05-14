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
 

package de.fzj.unicore.uas.impl.tss;

import java.util.Calendar;
import java.util.Map;

import org.apache.log4j.Logger;

import de.fzj.unicore.uas.TargetSystemFactory;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.UASProperties;
import de.fzj.unicore.uas.impl.BaseInitParameters;
import de.fzj.unicore.uas.impl.BaseResourceImpl;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.wsrflite.InitParameters;
import de.fzj.unicore.wsrflite.InitParameters.TerminationMode;
import de.fzj.unicore.wsrflite.messaging.PullPoint;

/**
 * Implements the {@link TargetSystemFactory} interface.<br/>
 * 
 * <p>This is a fairly simplistic implementation that does not
 * use all the capabilities of the interface. For all the
 * TargetSystems created by this factory, the same XNJS instance
 * is used. The TargetSystemDescription supplied to CreateTSR() 
 * is ignored.</p>
 * 
 * @author schuller
 */
public class TargetSystemFactoryImpl extends BaseResourceImpl {

	private static final Logger logger = LogUtil.getLogger(LogUtil.JOBS, TargetSystemFactoryImpl.class);

	public TargetSystemFactoryImpl(){
		super();
	}

	@Override
	public TSFModel getModel(){
		return (TSFModel)model;
	}
	
	@Override
	public void initialise(InitParameters initArgs)throws Exception{
		if(model == null){
			setModel(new TSFModel());
		}
		TSFModel model=getModel();
		super.initialise(initArgs);
		model.supportsReservation=getXNJSFacade().supportsReservation();
	}
	
	/**
	 * create a new TSS with default settings, add the ID to the model and return the ID
	 */
	public String createTargetSystem() throws Exception {
		return createTargetSystem(null,null);
	}
	
	/**
	 * create a new TSS, add the ID to the model and return the ID
	 * @param tt - initial termination time - null for default TT
	 * @param parameters - user-specified parameters
	 */
	public String createTargetSystem(Calendar tt, Map<String,String> parameters)throws Exception{
		BaseInitParameters initObjs= tt!=null?
				new BaseInitParameters(null, tt): new BaseInitParameters(null, TerminationMode.NEVER);
		initObjs.parentUUID = getUniqueID();
		initObjs.xnjsReference = getXNJSReference();
		UASProperties props = kernel.getAttribute(UASProperties.class);
		Class<?>tssClass = props.getClassValue(UASProperties.TSS_CLASS, TargetSystemImpl.class);
		initObjs.resourceClassName = tssClass.getName();
		if(parameters!=null)initObjs.extraParameters.putAll(parameters);
		String id = kernel.getHome(UAS.TSS).createResource(initObjs);
		getModel().addChild(UAS.TSS, id);
		return id;
	}
	
	@Override
	public void processMessages(PullPoint p){
		//check for deleted TSSs and remove them
		while(p.hasNext()){
			String m=(String)p.next().getBody();
			if(m.startsWith("deleted:")){
				String id=m.substring(m.indexOf(":")+1);
				logger.debug("Removing TSS with ID "+id+"...");
				getModel().removeChild(id);
			}
		}
	}

}
