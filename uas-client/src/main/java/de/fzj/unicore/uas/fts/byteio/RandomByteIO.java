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
 
package de.fzj.unicore.uas.fts.byteio;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.ParameterStyle;
import javax.jws.soap.SOAPBinding.Style;
import javax.jws.soap.SOAPBinding.Use;
import javax.xml.namespace.QName;
import javax.xml.ws.soap.MTOM;

import org.ggf.schemas.byteio.x2005.x10.randomAccess.AppendDocument;
import org.ggf.schemas.byteio.x2005.x10.randomAccess.AppendResponseDocument;
import org.ggf.schemas.byteio.x2005.x10.randomAccess.ReadDocument;
import org.ggf.schemas.byteio.x2005.x10.randomAccess.ReadResponseDocument;
import org.ggf.schemas.byteio.x2005.x10.randomAccess.ReadableDocument;
import org.ggf.schemas.byteio.x2005.x10.randomAccess.SizeDocument;
import org.ggf.schemas.byteio.x2005.x10.randomAccess.TransferMechanismDocument;
import org.ggf.schemas.byteio.x2005.x10.randomAccess.TruncAppendDocument;
import org.ggf.schemas.byteio.x2005.x10.randomAccess.TruncAppendResponseDocument;
import org.ggf.schemas.byteio.x2005.x10.randomAccess.WriteDocument;
import org.ggf.schemas.byteio.x2005.x10.randomAccess.WriteResponseDocument;
import org.ggf.schemas.byteio.x2005.x10.randomAccess.WriteableDocument;

import de.fzj.unicore.uas.fts.FileTransfer;
import de.fzj.unicore.wsrflite.xmlbeans.BaseFault;

/**
 * the random-access byteio interface
 * 
 * @author schuller
 */
@WebService
@SOAPBinding(parameterStyle=ParameterStyle.BARE, use=Use.LITERAL, style=Style.DOCUMENT)
@MTOM
public interface RandomByteIO extends FileTransfer {

	public static final String RBYTIO_NS="http://schemas.ggf.org/byteio/2005/10/random-access";	
	
	/**
	 * transfer type: simple
	 */
	public static final String TRANSFER_SIMPLE=
		"http://schemas.ggf.org/byteio/2005/10/transfer-mechanisms/simple";	
	
	/**
	 * Size resource property
	 */
	public static final QName RPSize = SizeDocument.type.getDocumentElementName();
	
	/**
	 * Transfer mechanisms supported by this implementation
	 */
	public static final QName RPTransferMechanisms = TransferMechanismDocument.type.getDocumentElementName();
	
	
	/**
	 * Is the resource readable?
	 */
	public static final QName RPReadable = ReadableDocument.type.getDocumentElementName();

	/**
	 * Is the resource readable?
	 */
	public static final QName RPWriteable = WriteableDocument.type.getDocumentElementName();
	
	@WebMethod(action=RBYTIO_NS+"/read")
	@WebResult(targetNamespace=RBYTIO_NS,name="readResponse")
	public ReadResponseDocument read(
			@WebParam(targetNamespace=RBYTIO_NS,name="read")
			ReadDocument req) throws BaseFault;
	
	@WebMethod(action=RBYTIO_NS+"/write")
	@WebResult(targetNamespace=RBYTIO_NS,name="writeResponse")
	public WriteResponseDocument write(
			@WebParam(targetNamespace=RBYTIO_NS,name="write")
			WriteDocument req)throws BaseFault;
	
	@WebMethod(action=RBYTIO_NS+"/append")
	@WebResult(targetNamespace=RBYTIO_NS,name="appendResponse")
	public AppendResponseDocument append(
			@WebParam(targetNamespace=RBYTIO_NS,name="append")
			AppendDocument req) throws BaseFault;
	
	@WebMethod(action=RBYTIO_NS+"/truncAppend")
	@WebResult(targetNamespace=RBYTIO_NS,name="truncAppendResponse")
	public TruncAppendResponseDocument truncAppend(
			@WebParam(targetNamespace=RBYTIO_NS,name="truncAppend")
			TruncAppendDocument req) throws BaseFault;
	
}
