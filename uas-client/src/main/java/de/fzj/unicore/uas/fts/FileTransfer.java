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

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.ParameterStyle;
import javax.jws.soap.SOAPBinding.Style;
import javax.jws.soap.SOAPBinding.Use;
import javax.xml.namespace.QName;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.SourceDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.TargetDocument;
import org.unigrids.services.atomic.types.ProtocolDocument;
import org.unigrids.services.atomic.types.StorageEndpointReferenceDocument;
import org.unigrids.x2006.x04.services.fts.SizeDocument;
import org.unigrids.x2006.x04.services.fts.StatusDetailsDocument;
import org.unigrids.x2006.x04.services.fts.StatusDocument;
import org.unigrids.x2006.x04.services.fts.TransferredBytesDocument;

import de.fzj.unicore.wsrflite.xmlbeans.WSResource;

/**
 * FileTransfer
 * 
 * @author schuller
 */
@WebService
@SOAPBinding(parameterStyle=ParameterStyle.BARE, use=Use.LITERAL, style=Style.DOCUMENT)
public interface FileTransfer extends WSResource {

	/**
	 * Resource property representing the Source of the transfer
	 */
	public static QName RPSource=SourceDocument.type.getDocumentElementName();
	
	/**
	 * Resource property representing the Target of the transfer
	 */
	public static QName RPTarget=TargetDocument.type.getDocumentElementName();
	
	/**
	 * protocol used
	 */
	public static QName RPProtocol=ProtocolDocument.type.getDocumentElementName();
	
	/**
	 * bytes transferred
	 */
    public static QName RPTransferred=TransferredBytesDocument.type.getDocumentElementName();	

	/**
	 * SMS epr
	 */
    public static QName RPParentSMS=StorageEndpointReferenceDocument.type.getDocumentElementName();	

	/**
	 * Status summary (RUNNING, DONE, etc)
	 */
    public static QName RPStatus=StatusDocument.type.getDocumentElementName();	
    

    /**
	 * Status details (protocol/implementation dependent)
	 */
    public static QName RPStatusDetails=StatusDetailsDocument.type.getDocumentElementName();	
    
    /**
	 * Resource property representing the size of the remote file
	 */
	public static QName RPSize=SizeDocument.type.getDocumentElementName();
	
}
