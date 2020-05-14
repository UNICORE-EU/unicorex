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

import java.io.IOException;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.ggf.schemas.byteio.x2005.x10.byteIo.DataDocument;

import de.fzj.unicore.wsrflite.xmlbeans.BaseFault;

/**
 * Constants and some helpers
 * 
 * @author schuller
 */
public class ByteIO {

	private ByteIO(){}
	
	/**
	 * encode binary data
	 * 
	 * @param mechanism - the transfermechanism
	 * @param data - the data
	 * @return a ByteIO {@link DataDocument}
	 */
	public static XmlObject encode(String mechanism, byte[] data)throws BaseFault{
		if(RandomByteIO.TRANSFER_SIMPLE.equals(mechanism)){
			return encodeBase64(data);
		}
		else throw BaseFault.createFault("Unsupported transfer mechanism: "+mechanism);
	}
	
	/**
	 * decode binary data
	 * 
	 * @param mechanism - the transfer mechanism
	 * @param data - the data
	 */
	public static byte[] decode(String mechanism, XmlObject data)throws IOException,XmlException{
		if(RandomByteIO.TRANSFER_SIMPLE.equals(mechanism)){
			return decodeBase64(data);
		}
		return null;
	}
	
	public static XmlObject encodeBase64(byte[] data){
		DataDocument d=DataDocument.Factory.newInstance();
		d.setData(data);
		return d;
	}
	
	public static byte[] decodeBase64(XmlObject o)throws IOException,XmlException{
		DataDocument base64=DataDocument.Factory.parse(o.newInputStream());
		return base64.getData();
	}
}
