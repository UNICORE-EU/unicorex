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

import org.ggf.schemas.byteio.x2005.x10.streamableAccess.EndOfStreamDocument;
import org.ggf.schemas.byteio.x2005.x10.streamableAccess.PositionDocument;
import org.ggf.schemas.byteio.x2005.x10.streamableAccess.ReadableDocument;
import org.ggf.schemas.byteio.x2005.x10.streamableAccess.SeekReadDocument;
import org.ggf.schemas.byteio.x2005.x10.streamableAccess.SeekReadResponseDocument;
import org.ggf.schemas.byteio.x2005.x10.streamableAccess.SeekWriteDocument;
import org.ggf.schemas.byteio.x2005.x10.streamableAccess.SeekWriteResponseDocument;
import org.ggf.schemas.byteio.x2005.x10.streamableAccess.SeekableDocument;
import org.ggf.schemas.byteio.x2005.x10.streamableAccess.SizeDocument;
import org.ggf.schemas.byteio.x2005.x10.streamableAccess.TransferMechanismDocument;
import org.ggf.schemas.byteio.x2005.x10.streamableAccess.WriteableDocument;

import de.fzj.unicore.uas.fts.FileTransfer;
import eu.unicore.services.ws.BaseFault;

/**
 * streamable byte-io interface
 * 
 * @author schuller
 */
@WebService
@SOAPBinding(parameterStyle=ParameterStyle.BARE, use=Use.LITERAL, style=Style.DOCUMENT)
public interface StreamableByteIO extends FileTransfer{

	/**
	 * basic namespace for sbyteio
	 */
	public static final String SBYTIO_NS="http://schemas.ggf.org/byteio/2005/10/streamable-access";	
	
	/**
	 * origin indicator: current position
	 */
	public static final String SBYTIO_ORIGIN_CURRENT=SBYTIO_NS+"/seek-origins/current";	
	
	/**
	 * origin indicator: start of stream
	 */
	public static final String SBYTIO_ORIGIN_BEGINNING=SBYTIO_NS+"/seek-origins/beginning";	
	
	/**
	 * origin indicator: end of stream
	 */
	public static final String SBYTIO_ORIGIN_END=SBYTIO_NS+"/seek-origins/end";	
	
	/**
	 * transfer type: simple
	 */
	public static final String TRANSFER_SIMPLE="http://schemas.ggf.org/byteio/2005/10/transfer-mechanisms/simple";	

	/**
	 * Size resource property
	 */
	public static final QName RPSize = SizeDocument.type.getDocumentElementName();
	

	/**
	 * Is the resource readable?
	 */
	public static final QName RPReadable = ReadableDocument.type.getDocumentElementName();

	/**
	 * Is the resource readable?
	 */
	public static final QName RPWriteable = WriteableDocument.type.getDocumentElementName();
	
	/**
	 * Transfer mechanisms supported by this implementation
	 */
	public static final QName RPTransferMechanisms = TransferMechanismDocument.type.getDocumentElementName();
	
	/**
	 * is the resource at end of stream?
	 */
	public static final QName RPEndOfStream = EndOfStreamDocument.type.getDocumentElementName();
	
	/**
	 * is the resource seekable?
	 */
	public static final QName RPSeekable = SeekableDocument.type.getDocumentElementName();
	
	
	/**
	 * position in the stream
	 */
	public static final QName RPPosition= PositionDocument.type.getDocumentElementName();
	
	/**
	 * seek read
	 */
	@WebMethod(action=SBYTIO_NS+"/seekRead")
	@WebResult(targetNamespace=SBYTIO_NS,name="seekReadResponse")
	SeekReadResponseDocument seekRead(
			@WebParam(targetNamespace=SBYTIO_NS,name="seekRead")
			SeekReadDocument req)throws BaseFault;
	

	/** 
	 * seek write
	 */
	@WebMethod(action=SBYTIO_NS+"/seekWrite")
	@WebResult(targetNamespace=SBYTIO_NS,name="seekWriteResponse")
	SeekWriteResponseDocument seekWrite(
			@WebParam(targetNamespace=SBYTIO_NS,name="seekWrite")
			SeekWriteDocument req)throws BaseFault;
	
}
