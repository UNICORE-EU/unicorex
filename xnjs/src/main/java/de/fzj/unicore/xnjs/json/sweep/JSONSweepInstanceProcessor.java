/*********************************************************************************
 * Copyright (c) 2012 Forschungszentrum Juelich GmbH 
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


package de.fzj.unicore.xnjs.json.sweep;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.json.JSONJobProcessor;

/**
 * handles sweep instances forked by SweepProcessor
 * 
 * TODO
 *  - perform input file modifications (file sweep)
 * 
 * @author schuller
 */
public class JSONSweepInstanceProcessor extends JSONJobProcessor {

	//for storing the sweep params
	static final String SWEEP_PARAMS_KEY = JSONSweepInstanceProcessor.class.getName()+"_SweepParams";
	
	//for storing the ID of the master job
	static final String SWEEP_PARENT_JOB_ID_KEY = JSONSweepInstanceProcessor.class.getName()+"_ParentJob_ID";
	
	//for storing the Uspace location of the master job
	static final String SWEEP_PARENT_JOB_USPACE_KEY = JSONSweepInstanceProcessor.class.getName()+"_ParentJob_Uspace";
	
	public JSONSweepInstanceProcessor(XNJS xnjs){
		super(xnjs);
	}
	
}
