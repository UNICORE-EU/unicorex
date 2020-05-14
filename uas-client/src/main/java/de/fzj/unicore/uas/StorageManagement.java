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
 

package de.fzj.unicore.uas;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.ParameterStyle;
import javax.jws.soap.SOAPBinding.Style;
import javax.jws.soap.SOAPBinding.Use;
import javax.xml.namespace.QName;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.FileSystemDocument;
import org.unigrids.services.atomic.types.ProtocolDocument;
import org.unigrids.services.atomic.types.UmaskDocument;
import org.unigrids.x2006.x04.services.sms.ACLSupportedDocument;
import org.unigrids.x2006.x04.services.sms.FiletransferEnumerationReferenceDocument;
import org.unigrids.x2006.x04.services.sms.MetadataServiceReferenceDocument;

import de.fzj.unicore.wsrflite.xmlbeans.BaseFault;
import de.fzj.unicore.wsrflite.xmlbeans.ResourceLifetime;
import de.fzj.unicore.wsrflite.xmlbeans.ResourceProperties;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnavailableFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnknownFault;
import eu.unicore.security.OperationType;
import eu.unicore.security.SEIOperationType;
import eu.unicore.security.wsutil.RequiresSignature;

@WebService(targetNamespace = "http://unigrids.org/2006/04/services/sms",
		portName="StorageManagement")
@SOAPBinding(parameterStyle=ParameterStyle.BARE, use=Use.LITERAL, style=Style.DOCUMENT)
public interface StorageManagement extends ResourceProperties,ResourceLifetime {
	//Namespace
	public static final String SMS_NS="http://unigrids.org/2006/04/services/sms";
	
	//Porttype
	public static final QName SMS_PORT=new QName(SMS_NS,"StorageManagement");
	
	//actions
	public static final String ACTION_EXPORT="http://unigrids.org/2006/04/services/sms/StorageManagement/ExportFileRequest";
	public static final String ACTION_IMPORT="http://unigrids.org/2006/04/services/sms/StorageManagement/ImportFileRequest";
	public static final String ACTION_SEND="http://unigrids.org/2006/04/services/sms/StorageManagement/SendFileRequest";
	public static final String ACTION_RECEIVE="http://unigrids.org/2006/04/services/sms/StorageManagement/ReceiveFileRequest";
	public static final String ACTION_DELETE="http://unigrids.org/2006/04/services/sms/StorageManagement/DeleteRequest";
	public static final String ACTION_RENAME="http://unigrids.org/2006/04/services/sms/StorageManagement/RenameRequest";
	public static final String ACTION_FIND="http://unigrids.org/2006/04/services/sms/StorageManagement/FindRequest";
	public static final String ACTION_TRIGGER="http://unigrids.org/2006/04/services/sms/StorageManagement/TriggerRequest";
	
	/**
	 * protocol(s) used to access the storage
	 */
	public static QName RPProtocol=ProtocolDocument.type.getDocumentElementName();
	
	/**
	 * Filesystem descriptor
	 */
	public static QName RPFileSystem=FileSystemDocument.type.getDocumentElementName();

	/**
	 * ACL support
	 */
	public static final QName ACLSupported=ACLSupportedDocument.type.getDocumentElementName();

	/**
	 * Umask
	 */
	public static final QName RPUmask = UmaskDocument.type.getDocumentElementName();
	
	/**
	 * address of the associated metadata management service
	 */
	public static QName RPMetadataServiceReference=MetadataServiceReferenceDocument.type.getDocumentElementName();
	
	/**
	 * references to server-to-server file transfers on this storage
	 */
	public static QName RPFiletransferEnumerationReference=FiletransferEnumerationReferenceDocument.type.getDocumentElementName();
	
	
	@WebMethod(action = "http://unigrids.org/2006/04/services/sms/StorageManagement/ListDirectoryRequest")
	@SEIOperationType(OperationType.read)
	@WebResult(targetNamespace=SMS_NS,name="ListDirectoryResponse")
	public org.unigrids.x2006.x04.services.sms.ListDirectoryResponseDocument ListDirectory(
			@WebParam(targetNamespace=SMS_NS,name="ListDirectory")
			org.unigrids.x2006.x04.services.sms.ListDirectoryDocument in)
			throws ResourceUnavailableFault,ResourceUnknownFault,BaseFault;

	@WebMethod(action = "http://unigrids.org/2006/04/services/sms/StorageManagement/ListPropertiesRequest")
	@SEIOperationType(OperationType.read)
	@WebResult(targetNamespace=SMS_NS,name="ListPropertiesResponse")
	public org.unigrids.x2006.x04.services.sms.ListPropertiesResponseDocument ListProperties(
			@WebParam(targetNamespace=SMS_NS,name="ListProperties")
			org.unigrids.x2006.x04.services.sms.ListPropertiesDocument in)
			throws ResourceUnavailableFault,ResourceUnknownFault,BaseFault;

	@RequiresSignature
	@WebMethod(action = "http://unigrids.org/2006/04/services/sms/StorageManagement/CopyRequest")
	@WebResult(targetNamespace=SMS_NS,name="CopyResponse")
	public org.unigrids.x2006.x04.services.sms.CopyResponseDocument Copy(
			@WebParam(targetNamespace=SMS_NS,name="Copy")
			org.unigrids.x2006.x04.services.sms.CopyDocument in)
			throws ResourceUnavailableFault,ResourceUnknownFault,BaseFault;

	@RequiresSignature
	@WebMethod(action = "http://unigrids.org/2006/04/services/sms/StorageManagement/CreateDirectoryRequest")
	@WebResult(targetNamespace=SMS_NS,name="CreateDirectoryResponse")
	public org.unigrids.x2006.x04.services.sms.CreateDirectoryResponseDocument CreateDirectory(
			@WebParam(targetNamespace=SMS_NS,name="CreateDirectory")
			org.unigrids.x2006.x04.services.sms.CreateDirectoryDocument in)
			throws ResourceUnavailableFault,ResourceUnknownFault,BaseFault;

	@RequiresSignature
	@WebMethod(action = ACTION_DELETE)
	@WebResult(targetNamespace=SMS_NS,name="DeleteResponse")
	public org.unigrids.x2006.x04.services.sms.DeleteResponseDocument Delete(
			@WebParam(targetNamespace=SMS_NS,name="Delete")
			org.unigrids.x2006.x04.services.sms.DeleteDocument in)
			throws ResourceUnavailableFault,ResourceUnknownFault,BaseFault;

	@RequiresSignature
	@WebMethod(action = ACTION_RENAME)
	@WebResult(targetNamespace=SMS_NS,name="RenameResponse")
	public org.unigrids.x2006.x04.services.sms.RenameResponseDocument Rename(
			@WebParam(targetNamespace=SMS_NS,name="Rename")
			org.unigrids.x2006.x04.services.sms.RenameDocument in)
			throws ResourceUnavailableFault,ResourceUnknownFault,BaseFault;

	@RequiresSignature
	@WebMethod(action = "http://unigrids.org/2006/04/services/sms/StorageManagement/ChangePermissionsRequest")
	@WebResult(targetNamespace=SMS_NS,name="ChangePermissionsResponse")
	public org.unigrids.x2006.x04.services.sms.ChangePermissionsResponseDocument ChangePermissions(
			@WebParam(targetNamespace=SMS_NS,name="ChangePermissions")
			org.unigrids.x2006.x04.services.sms.ChangePermissionsDocument in)
			throws ResourceUnavailableFault,ResourceUnknownFault,BaseFault;

	@RequiresSignature
	@WebMethod(action = ACTION_IMPORT)
	@WebResult(targetNamespace=SMS_NS,name="ImportFileResponse")
	public org.unigrids.x2006.x04.services.sms.ImportFileResponseDocument ImportFile(
			@WebParam(targetNamespace=SMS_NS,name="ImportFile")
			org.unigrids.x2006.x04.services.sms.ImportFileDocument in)
			throws ResourceUnavailableFault,ResourceUnknownFault,BaseFault;

	@RequiresSignature
	@WebMethod(action = ACTION_EXPORT)
	@SEIOperationType(OperationType.read)
	@WebResult(targetNamespace=SMS_NS,name="ExportFileResponse")
	public org.unigrids.x2006.x04.services.sms.ExportFileResponseDocument ExportFile(
			@WebParam(targetNamespace=SMS_NS,name="ExportFile")
			org.unigrids.x2006.x04.services.sms.ExportFileDocument in)
			throws ResourceUnavailableFault,ResourceUnknownFault,BaseFault;

	@RequiresSignature
	@WebMethod(action = ACTION_RECEIVE)
	@WebResult(targetNamespace=SMS_NS,name="ReceiveFileResponse")
	public org.unigrids.x2006.x04.services.sms.ReceiveFileResponseDocument ReceiveFile(
			@WebParam(targetNamespace=SMS_NS,name="ReceiveFile")
			org.unigrids.x2006.x04.services.sms.ReceiveFileDocument in)
			throws ResourceUnavailableFault,ResourceUnknownFault,BaseFault;
	
	@RequiresSignature
	@WebMethod(action = ACTION_SEND)
	@SEIOperationType(OperationType.read)
	@WebResult(targetNamespace=SMS_NS,name="SendFileResponse")
	public org.unigrids.x2006.x04.services.sms.SendFileResponseDocument SendFile(
			@WebParam(targetNamespace=SMS_NS,name="SendFile")
			org.unigrids.x2006.x04.services.sms.SendFileDocument in)
			throws ResourceUnavailableFault,ResourceUnknownFault,BaseFault;
	
	@WebMethod(action = ACTION_FIND)
	@SEIOperationType(OperationType.read)
	@WebResult(targetNamespace=SMS_NS,name="FindResponse")
	public org.unigrids.x2006.x04.services.sms.FindResponseDocument Find(
			@WebParam(targetNamespace=SMS_NS,name="Find")
			org.unigrids.x2006.x04.services.sms.FindDocument in)
			throws ResourceUnavailableFault,ResourceUnknownFault,BaseFault;

}