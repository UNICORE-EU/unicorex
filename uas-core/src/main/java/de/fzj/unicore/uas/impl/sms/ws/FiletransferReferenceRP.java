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
 ********************************************************************************/
 

package de.fzj.unicore.uas.impl.sms.ws;

import java.util.List;

import javax.xml.namespace.QName;

import org.w3.x2005.x08.addressing.EndpointReferenceDocument;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.impl.sms.SMSBaseImpl;
import de.fzj.unicore.wsrflite.Resource;
import de.fzj.unicore.wsrflite.xmlbeans.XmlRenderer.Internal;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.AddressListRenderer;

/**
 * internally generates the list of references to server-to-server 
 * filetransfer resources on an SMS. These are not published via the
 * RP document. <br/>
 * 
 * @author schuller
 */
public class FiletransferReferenceRP extends AddressListRenderer implements Internal {

	private static final QName qname=new QName("http://unigrids.org/2006/04/services/fts","ServerToServerFileTransfer");
	
	public FiletransferReferenceRP(Resource parent){
		super(parent,
				UAS.SERVER_FTS, 
				EndpointReferenceDocument.type.getDocumentElementName(),
				qname,
				false);
	}

	@Override
	protected List<String> getUIDs() {
		return ((SMSBaseImpl)parent).getModel().getFileTransferUIDs();
	}
	
}