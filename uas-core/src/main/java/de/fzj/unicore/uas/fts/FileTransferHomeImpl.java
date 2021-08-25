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


package de.fzj.unicore.uas.fts;

import de.fzj.unicore.uas.fts.http.FileServlet;
import eu.unicore.services.InitParameters;
import eu.unicore.services.Resource;
import eu.unicore.services.exceptions.ResourceNotCreatedException;
import eu.unicore.services.impl.DefaultHome;

/**
 * File transfer home<br/>.
 * 
 * The actual class created by {@link #doCreateInstance()} is looked up dynamically
 * in the UAS properties.
 *
 * @author schuller
 */
public class FileTransferHomeImpl extends DefaultHome {

	//the actual protocol to be used is passed via this thread-local
	private final ThreadLocal<String>protocolT=new ThreadLocal<String>();

	/**
	 * Creates a FileTransferImpl for the given protocol<br/>
	 * If the protocol is null, a {@link ServerToServerFileTransferImpl} is created.
     */
	@Override
	protected Resource doCreateInstance()throws Exception{
		try{
			String protocol=protocolT.get();
			if(protocol==null){
				return new ServerToServerFileTransferImpl();
			}
			else return FileTransferCapabilities.getFileTransferImpl(protocol, getKernel());
		}
		finally{
			protocolT.remove();
		}
	}

	@Override
	public String createResource(InitParameters initobjs) throws ResourceNotCreatedException{
		FiletransferInitParameters ftInit = (FiletransferInitParameters)initobjs;
		if(ftInit.protocol!=null)protocolT.set(ftInit.protocol.toString());
		return super.createResource(initobjs);
	}

	/**
	 * Called after server start
	 */
	public void run(){
		initBFT();
	}

	protected void initBFT(){
		FileServlet.initialise(getKernel());
	}

}
