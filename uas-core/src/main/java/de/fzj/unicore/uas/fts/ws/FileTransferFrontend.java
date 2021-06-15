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


package de.fzj.unicore.uas.fts.ws;

import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import org.unigrids.services.atomic.types.ProtocolType;
import org.unigrids.x2006.x04.services.fts.FileTransferPropertiesDocument;
import org.unigrids.x2006.x04.services.fts.SummaryType;

import de.fzj.unicore.uas.ft.http.AccessURLDocument;
import de.fzj.unicore.uas.fts.FileTransfer;
import de.fzj.unicore.uas.fts.FileTransferImpl;
import de.fzj.unicore.uas.impl.UASBaseFrontEnd;
import eu.unicore.services.ws.renderers.AddressRenderer;
import eu.unicore.services.ws.renderers.ValueRenderer;

/**
 * WSRF frontend for FileTransfer
 *  
 * @author schuller
 */
public class FileTransferFrontend extends UASBaseFrontEnd implements FileTransfer {

	/**
	 * Configuration key: maps protocol to implementation class.
	 * For example, the config entry 
	 * "uas.filetransfer.protocol.HTTP=my.class.for.http" 
	 * will map the given class to the HTTP protocol.
	 * The class must extend {@link FileTransferFrontend}
	 */
	public static final String CONFIG_PROTOCOL_KEY="uas.filetransfer.protocol.";

	protected static final int STATUS_RUNNING=SummaryType.INT_RUNNING;
	protected static final int STATUS_DONE=SummaryType.INT_DONE;
	protected static final int STATUS_FAILED=SummaryType.INT_FAILED;
	protected static final int STATUS_UNDEFINED=SummaryType.INT_UNDEFINED;
	protected static final int STATUS_READY=SummaryType.INT_READY;

	protected final FileTransferImpl resource;
	
	public FileTransferFrontend(FileTransferImpl r){
		super(r);
		this.resource = r;
		addRenderer(new ProtocolRenderer(resource){
			public List<ProtocolType.Enum> getProtocols(){
				return Collections.singletonList(resource.getModel().getProtocol());
			}
		});
		addRenderer(new TransferredBytesResourceProperty(resource));
		addRenderer(new StatusResourceProperty(resource));
		addRenderer(new SourceResourceProperty(resource));
		addRenderer(new TargetResourceProperty(resource));
		addRenderer(new UASSizeResourceProperty(resource));
		addRenderer(new AddressRenderer(resource, RPParentSMS,true){
			@Override
			protected String getServiceSpec(){
				return resource.getModel().getServiceSpec();
			}
		});
		//publish protocol-specific params
		addRenderer(new ParameterRenderer(resource));
		// supporting potential old clients using BFT
		addRenderer(new ValueRenderer(resource, AccessURLDocument.type.getDocumentElementName()){
			protected AccessURLDocument getValue(){
				String accessURL = resource.getProtocolDependentParameters().getOrDefault("accessURL", "");
				AccessURLDocument urlDoc=AccessURLDocument.Factory.newInstance();
				urlDoc.setAccessURL(accessURL);
				return urlDoc;
			}
		});
	}
	
	@Override
	public QName getResourcePropertyDocumentQName() {
		return FileTransferPropertiesDocument.type.getDocumentElementName();
	}

	private static final QName portType=new QName("http://unigrids.org/2006/04/services/fts","FileTransfer");

	public QName getPortType()
	{
		return portType;
	}

	public static String statusAsString(int statusCode){
		return SummaryType.Enum.forInt(statusCode).toString();
	}
}
