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

import eu.unicore.security.OperationType;
import eu.unicore.security.SEIOperationType;
import eu.unicore.security.wsutil.RequiresSignature;
import eu.unicore.services.ws.BaseFault;
import eu.unicore.services.ws.ResourceLifetime;
import eu.unicore.services.ws.ResourceProperties;
import eu.unicore.services.ws.exceptions.ResourceUnavailableFault;
import eu.unicore.services.ws.exceptions.ResourceUnknownFault;

/**
 * Metadata service interface
 * 
 * @author Waqas Noor
 * @author schuller
 * @author Konstantine Muradov
 */
@WebService(targetNamespace = "http://unigrids.org/2006/04/services/metadata",
		portName="MetadataManagement")
@SOAPBinding(parameterStyle=ParameterStyle.BARE, use=Use.LITERAL, style=Style.DOCUMENT)
public interface MetadataManagement extends ResourceProperties,ResourceLifetime {
	//Namespace
	public static final String META_NS="http://unigrids.org/2006/04/services/metadata";
	
	//Porttype
	public static final QName META_PORT=new QName(META_NS,"MetadataManagement");

	@RequiresSignature
	@WebMethod(action = "http://unigrids.org/2006/04/services/metadata/MetadataManagement/CreateMetadataRequest")
	@WebResult(targetNamespace=META_NS, name="CreateMetadataResponse")
	public org.unigrids.x2006.x04.services.metadata.CreateMetadataResponseDocument CreateMetadata(
			@WebParam(targetNamespace=META_NS, name="CreateMetadata")
			org.unigrids.x2006.x04.services.metadata.CreateMetadataDocument in)
			throws ResourceUnavailableFault,ResourceUnknownFault,BaseFault;

	@RequiresSignature
	@WebMethod(action = "http://unigrids.org/2006/04/services/metadata/MetadataManagement/UpdateMetadataRequest")
	@WebResult(targetNamespace=META_NS, name="UpdateMetadataResponse")
	public org.unigrids.x2006.x04.services.metadata.UpdateMetadataResponseDocument UpdateMetadata(
			@WebParam(targetNamespace=META_NS, name="UpdateMetadata")
			org.unigrids.x2006.x04.services.metadata.UpdateMetadataDocument in)
			throws ResourceUnavailableFault,ResourceUnknownFault,BaseFault;

	@RequiresSignature
	@WebMethod(action = "http://unigrids.org/2006/04/services/metadata/MetadataManagement/DeleteMetadataRequest")
	@WebResult(targetNamespace=META_NS, name="DeleteMetadataResponse")
	public org.unigrids.x2006.x04.services.metadata.DeleteMetadataResponseDocument DeleteMetadata(
			@WebParam(targetNamespace=META_NS, name="DeleteMetadata")
			org.unigrids.x2006.x04.services.metadata.DeleteMetadataDocument in)
			throws ResourceUnavailableFault,ResourceUnknownFault,BaseFault;
	
	/**
	 * search metadata via the provided search string
	 * 
	 * @param in - search parameters
	 * 
	 * @throws BaseFault
	 */
	@SEIOperationType(OperationType.read)
	@WebMethod(action = "http://unigrids.org/2006/04/services/metadata/MetadataManagement/SearchMetadataRequest")
	@WebResult(targetNamespace=META_NS, name="SearchMetadataResponse")
	public org.unigrids.x2006.x04.services.metadata.SearchMetadataResponseDocument SearchMetadata(
			@WebParam(targetNamespace=META_NS, name="SearchMetadata")
			org.unigrids.x2006.x04.services.metadata.SearchMetadataDocument in)
			throws ResourceUnavailableFault,ResourceUnknownFault,BaseFault;
	
	/**
	 * Federated metadata search
	 * 
	 * @param in - search parameters
	 * 
	 * @throws BaseFault
	 */
	
	@SEIOperationType(OperationType.read)
	@WebMethod(action = "http://unigrids.org/2006/04/services/metadata/MetadataManagement/FederatedMetadataSearchRequest")
	@WebResult(targetNamespace=META_NS, name="FederatedMetadataSearchResponse")
	public org.unigrids.x2006.x04.services.metadata.FederatedMetadataSearchResponseDocument FederatedMetadataSearch(
			@WebParam(targetNamespace=META_NS, name="FederatedMetadataSearch")
			org.unigrids.x2006.x04.services.metadata.FederatedMetadataSearchDocument in)
			throws BaseFault;
	
	/**
	 * get metadata for a specified resource
	 * 
	 * @throws BaseFault
	 */
	@SEIOperationType(OperationType.read)
	@WebMethod(action = "http://unigrids.org/2006/04/services/metadata/MetadataManagement/GetMetadataRequest")
	@WebResult(targetNamespace=META_NS, name="GetMetadataResponse")
	public org.unigrids.x2006.x04.services.metadata.GetMetadataResponseDocument GetMetadata(
			@WebParam(targetNamespace=META_NS, name="GetMetadata")
			org.unigrids.x2006.x04.services.metadata.GetMetadataDocument in)
			throws ResourceUnavailableFault,ResourceUnknownFault,BaseFault;
	
	/**
	 * initiate aysnchronous metadata extraction
	 * @param in - base path and requested extraction depth
	 * @return EPR of a Task
	 * @throws BaseFault
	 */
	@RequiresSignature
	@WebMethod(action = "http://unigrids.org/2006/04/services/metadata/MetadataManagement/StartMetadataExtractionRequest")
	@WebResult(targetNamespace=META_NS, name="StartMetadataExtractionResponse")
	public org.unigrids.x2006.x04.services.metadata.StartMetadataExtractionResponseDocument StartMetadataExtraction(
			@WebParam(targetNamespace=META_NS, name="StartMetadataExtraction")
			org.unigrids.x2006.x04.services.metadata.StartMetadataExtractionDocument in)
			throws ResourceUnavailableFault,ResourceUnknownFault,BaseFault;

}
