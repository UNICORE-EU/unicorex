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
 
package de.fzj.unicore.uas.client;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.input.BoundedInputStream;
import org.apache.logging.log4j.Logger;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.SourceTargetType;
import org.unigrids.services.atomic.types.PropertyType;
import org.unigrids.services.atomic.types.ProtocolDocument;
import org.unigrids.services.atomic.types.ProtocolType;
import org.unigrids.services.atomic.types.StorageEndpointReferenceDocument;
import org.unigrids.x2006.x04.services.fts.FileTransferPropertiesDocument;
import org.unigrids.x2006.x04.services.fts.SizeDocument;
import org.unigrids.x2006.x04.services.fts.StatusDocument;
import org.unigrids.x2006.x04.services.fts.SummaryType;
import org.unigrids.x2006.x04.services.fts.TransferredBytesDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.fts.FileTransfer;
import de.fzj.unicore.uas.fts.FiletransferOptions;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * Base client class for managing file transfers
 * 
 * Concrete subclasses may add methods specific to the actual file transfer protocol
 * 
 * @author schuller
 */
public abstract class FileTransferClient extends BaseUASClient 
implements FiletransferOptions.Read, FiletransferOptions.Write, AutoCloseable {
	
	protected static final Logger logger=Log.getLogger(Log.CLIENT, FileTransferClient.class);
	
	/**
	 * Some protocol implementations require knowledge about how to 
	 * deal with existing remote files. 
	 * When <em>importing</em> data to a remote location, this 
	 * flag controls whether data is appended to an existing 
	 * file, or whether the remote file is overwritten.
	 * If <code>true</code> data is appended.
	 * The default value is <code>false</code>. 
	 */
	protected boolean append=false;
	
	/**
	 * the storage this file transfer operates on
	 */
	protected StorageClient sms;
	
	/**
	 * Main constructor. NOTE: concrete subclasses MUST implement at least this constructor,
	 * as it is called from the {@link StorageClient} using reflection
	 * 
	 * @param url - the url of the target file transfer service
	 * @param epr - the EPR of the target file transfer service
	 * @param sec - security settings 
	 */
	public FileTransferClient(String url, EndpointReferenceType epr, IClientConfiguration sec)throws Exception {
		super(url, epr, sec);
	}

	public FileTransferClient(EndpointReferenceType epr, IClientConfiguration sec) throws Exception {
		this(epr.getAddress().getStringValue(),epr,sec);
	}

	/**
	 * uploads the given data
	 * 
	 * @param data
	 * @throws Exception
	 */
	public void write(byte[] data) throws Exception {
		writeAllData(new ByteArrayInputStream(data));
	}
	
	/**
	 * Writes <code>numBytes</code> bytes of data from <code>source</code> to the 
	 * remote location. If <code>numBytes</code> is negative, all bytes from the input
	 * stream (until EOF) are read and written to the remote location.<br/>
	 * In case the remote file exists, it is overwritten.
	 *
	 * @param source
	 * @param numBytes - how many bytes to read from the source, or -1 if all data should be read
	 * @throws Exception
	 * @since 1.4.0
	 */
	public void writeAllData(InputStream source, long numBytes)throws Exception{
		if(numBytes<0){
			writeAllData(source);
		}
		else{
			writeAllData(new BoundedInputStream(source, numBytes));
		}
	}
	
	
	/**
	 * get the number of bytes that have been already transferred
	 * 
	 * @return the number of transferred bytes or <code>-1</code> if not available
	 */
	public long getTransferredBytes()throws Exception{
		TransferredBytesDocument tbd=TransferredBytesDocument.Factory.parse(getResourceProperty(FileTransfer.RPTransferred));
		return tbd.getTransferredBytes();
	}
	
	/**
	 * get the address of the Storage resource on which the current file resides
	 * 
	 * @return an {@link EndpointReferenceType} to the parent storage 
	 */
	public EndpointReferenceType getParentStorage()throws Exception{
		StorageEndpointReferenceDocument ser=StorageEndpointReferenceDocument.Factory.parse(getResourceProperty(FileTransfer.RPParentSMS));
		return ser.getStorageEndpointReference();
	}

	/**
	 * get the protocol used by this transfer
	 */
	public ProtocolType.Enum getProtocol()throws Exception{
		ProtocolDocument pd=ProtocolDocument.Factory.parse(getResourceProperty(FileTransfer.RPProtocol));
		return pd.getProtocol();
	}

	/**
	 * get the source URI of this transfer
	 */
	public String getSource()throws Exception{
		SourceTargetType stt = getResourcePropertiesDocument().getFileTransferProperties().getSource();
		return stt!=null?stt.getURI() : null;
	}	


	/**
	 * get the target URI of this transfer
	 */
	public String getTarget() throws Exception{
		SourceTargetType stt = getResourcePropertiesDocument().getFileTransferProperties().getTarget();
		return stt!=null?stt.getURI() : null;
	}

	/**
	 * get the (human-readable) status of this transfer
	 */
	public String getStatus()throws Exception{
		StatusDocument status=StatusDocument.Factory.parse(getResourceProperty(FileTransfer.RPStatus));
		return status.getStatus().getSummary().toString()+" ["+status.getStatus().getDescription()+"]";
	}
	
	/**
	 * get the file transfer status
	 */
	public SummaryType.Enum getStatusSummary()throws Exception{
		StatusDocument status=StatusDocument.Factory.parse(getResourceProperty(FileTransfer.RPStatus));
		return status.getStatus().getSummary();
	}
	
	/**
	 * get the size of the source file, which may be remote<br/>
	 * This default implementation checks the filetransfer "Size" 
	 * resource property {@link FileTransfer#RPSize}
	 * 
	 * @return the size of the source file or <code>-1</code> 
	 *         if not available or an error occurs)
	 */
	public long getSourceFileSize(){
		try{
			SizeDocument sd=SizeDocument.Factory.parse(getResourceProperty(FileTransfer.RPSize));
			return sd.getSize();
		}catch(Exception ex){
			Log.logException("Error getting size of remote file.", ex, logger);
			return -1;
		}
	}
	
	/**
	 * get the resource properties document of this file transfer
	 * @throws Exception
	 */
	public FileTransferPropertiesDocument getResourcePropertiesDocument()throws Exception{
		return FileTransferPropertiesDocument.Factory.parse(GetResourcePropertyDocument().getGetResourcePropertyDocumentResponse().newInputStream());
	}
	
	/**
	 * set the "append" flag that is used when importing data to a remote location. 
	 * If <code>true</code>, data is appended to the remote file, if it exists.
	 * By default, remote files are overwritten.
	 * @param append - set to to <code>true</code> to append data
	 */
	public void setAppend(boolean append){
		this.append=append;
	}
	
	public StorageClient getParentStorageClient(){
		if(sms==null){
			try{
				sms=new StorageClient(getParentStorage(), getSecurityConfiguration());
			}catch(Exception ex){
				throw new RuntimeException(ex);
			}
		}
		return sms;
	}
	
	/**
	 * get any protocol dependent parameters from the resource property document
	 */
	public Map<String,String> getProtocolDependentRPs()throws Exception{
		Map<String,String>result=new HashMap<String,String>();
		PropertyType[] props=getResourcePropertiesDocument().getFileTransferProperties().getPropertyArray();
		for(PropertyType p: props){
			if(p.getName()!=null && p.getValue()!=null){
				result.put(p.getName(), p.getValue());
			}
		}
		return result;
	}
	
	/**
	 * destroy the underlying resource
	 */
	@Override
	public void close() throws Exception {
		destroy();
	}
	
}
