/*********************************************************************************
 * Copyright (c) 2006-2012 Forschungszentrum Juelich GmbH 
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.FileSystemDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.FileSystemType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.RangeValueType;
import org.unigrids.services.atomic.types.GridFileType;
import org.unigrids.services.atomic.types.PermissionsType;
import org.unigrids.services.atomic.types.PropertyType;
import org.unigrids.services.atomic.types.ProtocolDocument;
import org.unigrids.services.atomic.types.ProtocolType;
import org.unigrids.x2006.x04.services.sms.ChangeACLEntryType;
import org.unigrids.x2006.x04.services.sms.ChangeACLType;
import org.unigrids.x2006.x04.services.sms.ChangePermissionsDocument;
import org.unigrids.x2006.x04.services.sms.ChangePermissionsDocument.ChangePermissions;
import org.unigrids.x2006.x04.services.sms.ChangePermissionsEntryType;
import org.unigrids.x2006.x04.services.sms.CopyDocument;
import org.unigrids.x2006.x04.services.sms.CreateDirectoryDocument;
import org.unigrids.x2006.x04.services.sms.DeleteDocument;
import org.unigrids.x2006.x04.services.sms.ExportFileDocument;
import org.unigrids.x2006.x04.services.sms.ExportFileDocument.ExportFile;
import org.unigrids.x2006.x04.services.sms.ExportFileResponseDocument;
import org.unigrids.x2006.x04.services.sms.ExtendedChangePermissionsType;
import org.unigrids.x2006.x04.services.sms.ExtraParametersDocument.ExtraParameters;
import org.unigrids.x2006.x04.services.sms.FilterType;
import org.unigrids.x2006.x04.services.sms.FindDocument;
import org.unigrids.x2006.x04.services.sms.ImportFileDocument;
import org.unigrids.x2006.x04.services.sms.ImportFileDocument.ImportFile;
import org.unigrids.x2006.x04.services.sms.ImportFileResponseDocument;
import org.unigrids.x2006.x04.services.sms.ListDirectoryDocument;
import org.unigrids.x2006.x04.services.sms.ListPropertiesDocument;
import org.unigrids.x2006.x04.services.sms.PermissionsChangeModeType;
import org.unigrids.x2006.x04.services.sms.PermissionsClassType;
import org.unigrids.x2006.x04.services.sms.ReceiveFileDocument;
import org.unigrids.x2006.x04.services.sms.ReceiveFileResponseDocument;
import org.unigrids.x2006.x04.services.sms.RenameDocument;
import org.unigrids.x2006.x04.services.sms.SendFileDocument;
import org.unigrids.x2006.x04.services.sms.SendFileResponseDocument;
import org.unigrids.x2006.x04.services.sms.StoragePropertiesDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.CoreClientCapabilities.FTClientCapability;
import de.fzj.unicore.uas.FiletransferParameterProvider;
import de.fzj.unicore.uas.StorageManagement;
import eu.unicore.services.ClientCapabilities;
import eu.unicore.services.ClientCapability;
import eu.unicore.services.ws.BaseFault;
import eu.unicore.services.ws.ClientException;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * A client to access a storage management service (SMS) instance <br/>
 * 
 * Apart from providing convenient access to the functionality given by the
 * {@link StorageManagement} interface, this client provides a number of helper
 * methods. For example, the getImport() and getExport() methods will create a
 * {@link FileTransferClient} for writing/reading data to/from a given remote file.<br/>
 * 
 * @author schuller
 */
public class StorageClient extends BaseClientWithStatus {

	private final StorageManagement sms;

	/**
	 * protocol(s) used to access the storage
	 */
	public static final QName RPProtocol = ProtocolDocument.type.getDocumentElementName();

	/**
	 * Filesystem descriptor
	 */
	public static QName RPFileSystem = FileSystemDocument.type.getDocumentElementName();

	protected final static Map<ProtocolType.Enum, Class<? extends FileTransferClient>> registeredClients 
	= new HashMap<ProtocolType.Enum, Class<? extends FileTransferClient>>();

	/**
	 * register a client class supporting the given protocol. Note that clients are usually registered 
	 * using the service loader mechanism 
	 * @see FTClientCapability
	 * @param proto - the protocol
	 * @param clazz - the {@link FileTransferClient} class
	 */
	public static synchronized void registerClient(ProtocolType.Enum proto,	Class<? extends FileTransferClient> clazz) {
		registerClient(proto.toString(),clazz);
	}

	/**
	 * register a client class supporting the given protocol. Note that clients are usually registered 
	 * using the service loader mechanism 
	 * @see FTClientCapability
	 * @param proto - the protocol
	 * @param clazz - the {@link FileTransferClient} class
	 */
	public static synchronized void registerClient(String proto,Class<? extends FileTransferClient> clazz) {
		initRegisteredClients();
		doRegister(proto, clazz);
	}

	private static void doRegister(String proto, Class<? extends FileTransferClient> clazz){
		registeredClients.put(ProtocolType.Enum.forString(proto), clazz);
	}

	/**
	 * create a client for the storage service instance at EPR 'address' using the given URL
	 * 
	 * @param url - the URL
	 * @param address - the EPR
	 * @param sec - the security settings to use
	 * @throws Exception
	 */
	public StorageClient(String url, EndpointReferenceType address,IClientConfiguration sec) throws Exception {
		super(url, address, sec);
		sms = makeProxy(StorageManagement.class);
		initRegisteredClients();
	}

	/**
	 * create a client for the storage service instance at EPR 'address'
	 * 
	 * @param address - the EPR
	 * @param sec - the security settings to use
	 * @throws Exception
	 */
	public StorageClient(EndpointReferenceType address, IClientConfiguration sec) throws Exception {
		this(address.getAddress().getStringValue(), address, sec);
	}

	/**
	 * returns the service's StoragePropertiesDocument
	 */
	public StoragePropertiesDocument getResourcePropertiesDocument()
	throws Exception {
		return StoragePropertiesDocument.Factory
		.parse(GetResourcePropertyDocument()
				.getGetResourcePropertyDocumentResponse()
				.newInputStream());
	}


	/**
	 * returns the name of this storage
	 */
	public String getStorageName()throws Exception {
		return getFileSystem().getName();
	}

	/**
	 * returns the available space on the storage, or <code>-1</code> if not known
	 */
	public long getAvailableSpace()throws Exception {
		RangeValueType diskSpace=getFileSystem().getDiskSpace();
		return diskSpace!=null?(long)diskSpace.getExactArray()[0].getDoubleValue():-1;
	}

	/**
	 * returns the description of the file system accessed through this storage
	 */
	public FileSystemType getFileSystem()
	throws Exception {
		return getResourcePropertiesDocument().getStorageProperties().getFileSystem();
	}

	/**
	 * list the named directory
	 * 
	 * @param path - directory name
	 * @return GridFileType array containing info about the directory content
	 * @throws BaseFault
	 */
	public GridFileType[] listDirectory(String path) throws BaseFault {
		ListDirectoryDocument req = ListDirectoryDocument.Factory.newInstance();
		req.addNewListDirectory().setPath(path);
		return sms.ListDirectory(req).getListDirectoryResponse()
		.getGridFileArray();
	}

	/**
	 * get the proporties of a given path
	 * 
	 * @param path - the file path (relative to storage root)
	 * @return GridFileType describing the file/directory at "path"
	 * @throws BaseFault
	 * @throws FileNotFoundException if path does not exist remotely
	 */
	public GridFileType listProperties(String path) throws BaseFault, FileNotFoundException {
		ListPropertiesDocument req = ListPropertiesDocument.Factory.newInstance();
		req.addNewListProperties().setPath(path);
		GridFileType res=sms.ListProperties(req).getListPropertiesResponse().getGridFile();
		if(res==null){
			throw new FileNotFoundException("Path <"+path+"> not found on storage.");
		}
		else return res;
	}

	/**
	 * check if the Storage supports metadata management
	 * 
	 * return <code>true</code> if metadata management is supported, <code>false</code> otherwise
	 * @throws Exception - if communication errors occur
	 */
	public boolean supportsMetadata() throws Exception{
		return getResourcePropertiesDocument().getStorageProperties().getMetadataServiceReference()!=null;
	}

	/**
	 * check if the Storage supports file ACLs
	 * 
	 * return <code>true</code> if ACLs are supported, <code>false</code> otherwise
	 * @throws Exception - if communication errors occur
	 */
	public boolean supportsACL() throws Exception{
		return getResourcePropertiesDocument().getStorageProperties().isSetACLSupported()
			&& getResourcePropertiesDocument().getStorageProperties().getACLSupported() ;
	}

	/**
	 * check if the Storage supports data-oriented processing (rule-based actions based on 
	 * new data files)
	 * 
	 * return <code>true</code> if triggering management is supported, <code>false</code> otherwise
	 * @throws Exception - if communication errors occur
	 */
	public boolean supportsTriggering() throws Exception{
		return getResourcePropertiesDocument().getStorageProperties().getTriggeringSupported();
	}
	
	/**
	 * get a new MetadataClient for managing metadata on this storage.
	 * 
	 * @throws ClientException - if the storage does not support metadata
	 * @throws Exception - on communication errors
	 */
	public MetadataClient getMetadataClient() throws ClientException, Exception{
		EndpointReferenceType epr=getResourcePropertiesDocument().getStorageProperties().getMetadataServiceReference();
		if(epr==null){
			throw new ClientException("This service does not support metadata management --- " +
			"please check using the supportsMetadata() method.");
		}
		return new MetadataClient(epr,this.getSecurityConfiguration());
	}


	/**
	 * change the owner's permissions for a resource
	 * 
	 * @param path - the resource (relative to storage root)
	 * @param read - the readable flag
	 * @param write - the writeable flag
	 * @param execute - the executable flag
	 * @throws BaseFault
	 */
	public void changePermissions(String path, boolean read, boolean write, boolean execute) throws BaseFault {
		ChangePermissionsDocument req = ChangePermissionsDocument.Factory.newInstance();
		req.addNewChangePermissions().setPath(path);
		PermissionsType permissions=PermissionsType.Factory.newInstance();
		permissions.setReadable(read);
		permissions.setWritable(write);
		permissions.setExecutable(execute);
		req.getChangePermissions().setPermissions(permissions);
		sms.ChangePermissions(req);
	}


	/**
	 * Performs a remote chmod operation. This is an extended version of 
	 * {@link #changePermissions(String, boolean, boolean, boolean)} which allows only to change 
	 * owning user's permissions. 
	 * <p>
	 *    Note that this operation may not be supported by server side (in particular by non-UNIX
	 *    servers and servers running the embedded Java TSI). 
	 * @param path file to perform chmod on (relative to storage root)
	 * @param mode whether specified permissions should be added, removed or set 
	 * @param permClass whether modifications should be applied to owner, group or others
	 * @param rwxPermissions permissions being the subject of the operation
	 * @param recursive whether the operation should be recursive on directories
	 * @throws BaseFault
	 */
	public void changePermissions2(String path, PermissionsChangeModeType.Enum mode, 
			PermissionsClassType.Enum permClass, String rwxPermissions, boolean recursive) throws BaseFault {
		ChangePermissionsDocument reqDoc = ChangePermissionsDocument.Factory.newInstance();
		ChangePermissions req = reqDoc.addNewChangePermissions();
		req.setPath(path);
		ExtendedChangePermissionsType chmod = req.addNewExtendedPermissions();
		ChangePermissionsEntryType chmodE = chmod.addNewChangePermissionsEntry();
		chmodE.setKind(permClass);
		chmodE.setMode(mode);
		chmodE.setPermissions(rwxPermissions);
		req.setRecursive(recursive);
		sms.ChangePermissions(reqDoc);
	}

	/**
	 * Changes owning group of the file.
	 * <p>
	 *    Note that this operation may not be supported by server side (in particular by non-UNIX
	 *    servers and servers running the embedded Java TSI). 
	 * @param path file to perform chmod on (relative to storage root)
	 * @param group the new group
	 * @param recursive if the operation should be recursive on directory
	 * @throws BaseFault
	 */
	public void chgrp(String path, String group, boolean recursive) throws BaseFault {
		ChangePermissionsDocument reqDoc = ChangePermissionsDocument.Factory.newInstance();
		ChangePermissions req = reqDoc.addNewChangePermissions();
		req.setPath(path);
		req.setChangeOwningGroup(group);
		req.setRecursive(recursive);
		sms.ChangePermissions(reqDoc);
	}

	/**
	 * Modifies ACL for the given file. Allows for clearing all ACLs, setting and removing particular
	 * ACL entries. 
	 * <p>
	 *    Note that this operation may not be supported by server side (in particular by non-UNIX
	 *    servers and servers running the embedded Java TSI).
	 * @param path file to perform operation on
	 * @param clearAll whether (prior to any other changes) all ACL entries for the file should be deleted
	 * @param aces possibly empty array of ACL entries 
	 * @param recursive whether the operation should be recursive on directories
	 * @throws BaseFault
	 */
	public void setACL(String path, boolean clearAll, ChangeACLEntryType[] aces, boolean recursive) throws BaseFault {
		ChangePermissionsDocument reqDoc = ChangePermissionsDocument.Factory.newInstance();
		ChangePermissions req = reqDoc.addNewChangePermissions();
		req.setPath(path);
		ChangeACLType acl = ChangeACLType.Factory.newInstance();
		if (aces != null){
			acl.setChangeACLEntryArray(aces);
		}
		acl.setClearACL(clearAll);
		req.setACL(acl);
		req.setRecursive(recursive);
		sms.ChangePermissions(reqDoc);		
	}

	/**
	 * Low level access to the ChangePermissions operation. This is useful e.g. for performing
	 * many changePermissions2() operations in one WS call.
	 * @param path file to perform operation on
	 * @param reqDoc parameter
	 * @throws BaseFault
	 */
	public void ChangePermissions(String path, ChangePermissionsDocument reqDoc) throws BaseFault {
		sms.ChangePermissions(reqDoc);
	}


	/**
	 * perform server-side find
	 * 
	 * @param base - the base path (relative to storage root)
	 * @param recurse - whether to recurse into subdirectories
	 * @param nameMatch - the string to match file names against
	 * @param regexp - should the nameMatch be interpreted as a regular expression. See {@link Pattern} for regexp syntax
	 * @param before - only list files before this date
	 * @param after - only list files after this date
	 * @return list of {@link GridFileType}
	 * @throws BaseFault
	 */
	public GridFileType[] find(String base, boolean recurse, String nameMatch, boolean regexp, Calendar before, Calendar after) throws BaseFault {
		FindDocument req = FindDocument.Factory.newInstance();
		req.addNewFind().setBase(base);
		req.getFind().setRecurse(recurse);
		FilterType filter=FilterType.Factory.newInstance();
		if(regexp){
			filter.setNameMatchRegExp(nameMatch);
		}
		else{
			filter.setNameMatch(nameMatch);
		}
		filter.setBefore(before);
		filter.setAfter(after);
		req.getFind().setFilter(filter);
		return sms.Find(req).getFindResponse().getGridFileArray();
	}

	/**
	 * copy a file on the storage
	 * 
	 * @param source - the source file (relative to storage root)
	 * @param destination - target (relative to storage root)
	 */
	public void copy(String source, String destination) throws BaseFault {
		CopyDocument in = CopyDocument.Factory.newInstance();
		in.addNewCopy().setSource(source);
		in.getCopy().setDestination(destination);
		sms.Copy(in);
	}

	/**
	 * Delete a file or directory
	 * 
	 * @param path - the file to delete (relative to storage root)
	 */
	public void delete(String path) throws Exception {
		DeleteDocument in = DeleteDocument.Factory.newInstance();
		in.addNewDelete().addPath(path);
		sms.Delete(in);
	}

	/**
	 * Delete multiple files or directories<br/>
	 * 
	 * Note: For pre-1.7.x servers this will iterate over the paths and invoke
	 * the single-path delete() method.
	 * 
	 * @param paths - the files to delete (relative to storage root)
	 */
	public void delete(Collection<String> paths) throws Exception {
		DeleteDocument in = DeleteDocument.Factory.newInstance();
		in.addNewDelete();
		boolean oldServer=!checkVersion("1.6.2");
		Iterator<String>iter=paths.iterator();
		while(iter.hasNext()){
			String path=iter.next();
			if(oldServer){
				delete(path);
			}
			else{
				in.getDelete().addPath(path);
			}
		}
		if(oldServer)return;
		
		sms.Delete(in);
	}
	
	/**
	 * rename a file or directory
	 * 
	 * @param source - the source file (relative to storage root)
	 * @param destination - the target
	 * @throws Exception
	 */
	public void rename(String source, String destination) throws BaseFault{
		RenameDocument in = RenameDocument.Factory.newInstance();
		in.addNewRename().setSource(source);
		in.getRename().setDestination(destination);
		sms.Rename(in);
	}

	/**
	 * Low-level access to the "Import" function for uploading a file
	 * 
	 * @param in
	 *            an ImportFileDocument describing the import
	 * @return ImportFileResponseDocument containing the EPR of the newly
	 *         created FileTransfer instance
	 * @throws BaseFault
	 */
	public ImportFileResponseDocument ImportFile(ImportFileDocument in)throws Exception {
		return sms.ImportFile(in);
	}

	/**
	 * Low-level access to the "Export" function for downloading a file
	 * 
	 * @param in
	 *            an ExportFileDocument describing the export
	 * @return ExportFileResponseDocument containing the EPR of the newly
	 *         created FileTransfer instance
	 * @throws BaseFault
	 */
	public ExportFileResponseDocument ExportFile(ExportFileDocument in)throws BaseFault {
		return sms.ExportFile(in);
	}

	/**
	 * low-level access to the "ReceiveFile" function which instructs the 
	 * SMS to read a file from another SMS
	 * @param in - the XMLBeans ReceiveFileDocument
	 * @return ReceiveFileResponseDocument containing the EPR of the newly created
	 *         Filetransfer instance
	 * @throws BaseFault
	 */
	public ReceiveFileResponseDocument ReceiveFile(ReceiveFileDocument in)throws BaseFault {
		return sms.ReceiveFile(in);
	}

	/**
	 * low-level access to the "SendFile" function which instructs the 
	 * SMS to push a file to another SMS
	 * 
	 * @param in - the XMLBeans SendFileDocument
	 * @throws BaseFault
	 */
	public SendFileResponseDocument SendFile(SendFileDocument in)throws BaseFault {
		return sms.SendFile(in);
	}

	/**
	 * instructs the SMS to get a remote file from <code>source</code> and
	 * write it to <code>destination</code>
	 * 
	 * @param source - a URL describing the source
	 * @param destination - the target file (relative to storage root)
	 * @param extraParameters - protocol specific extra parameters
	 * @return a {@link TransferControllerClient} for monitoring the transfer
	 * @throws Exception
	 */
	public TransferControllerClient fetchFile(String source, String destination, Map<String,String>extraParameters)
			throws Exception{
				ReceiveFileDocument in = ReceiveFileDocument.Factory.newInstance();
				in.addNewReceiveFile().setDestination(destination);
				in.getReceiveFile().setSource(source);
				in.getReceiveFile().setExtraParameters(makeExtraParameters(extraParameters, null));
				EndpointReferenceType epr = ReceiveFile(in).getReceiveFileResponse().getReceiveFileEPR();
				TransferControllerClient c = new TransferControllerClient(
						epr.getAddress().getStringValue(), epr,	getSecurityConfiguration());
				return c;
			}

	/**
	 * instructs the SMS to get a remote file from <code>source</code> and
	 * write it to <code>destination</code>
	 * 
	 * @param source - a URL describing the source
	 * @param destination - the target file (relative to storage root)
	 * @return a {@link TransferControllerClient} for monitoring the transfer
	 * @throws Exception
	 */
	public TransferControllerClient fetchFile(String source, String destination)
	throws Exception {
		return fetchFile(source,destination,null);
	}

	/**
	 * instructs the SMS to send a file from <code>source</code> and write it
	 * to the remote <code>destination</code>
	 * 
	 * @param source - the source file (relative to storage root)
	 * @param destination - a URL describing the destination
	 * @param extraParameters
	 * @return a {@link TransferControllerClient} for monitoring the transfer
	 * @throws Exception
	 */
	public TransferControllerClient sendFile(String source, String destination, Map<String,String> extraParameters)
	throws Exception {
		SendFileDocument in = SendFileDocument.Factory.newInstance();
		in.addNewSendFile().setDestination(destination);
		in.getSendFile().setSource(source);
		in.getSendFile().setExtraParameters(makeExtraParameters(extraParameters, null));
		EndpointReferenceType epr = SendFile(in).getSendFileResponse()
		.getSendFileEPR();
		TransferControllerClient c = new TransferControllerClient(epr
				.getAddress().getStringValue(), epr, getSecurityConfiguration());
		return c;
	}
	/**
	 * instructs the SMS to send a file from <code>source</code> and write it
	 * to the remote <code>destination</code>
	 * 
	 * @param source - the source file (relative to storage root)
	 * @param destination - a URL describing the destination
	 * @return a {@link TransferControllerClient} for monitoring the transfer
	 * @throws Exception
	 */
	public TransferControllerClient sendFile(String source, String destination)
	throws Exception {
		return sendFile(source,destination,null);
	}

	/**
	 * create a directory
	 * 
	 * @param path - the path name (relative to storage root)
	 * @throws BaseFault
	 */
	public void createDirectory(String path) throws BaseFault {
		CreateDirectoryDocument in = CreateDirectoryDocument.Factory.newInstance();
		if(!path.startsWith("/")) path = "/"+path;
		in.addNewCreateDirectory().setPath(path);
		sms.CreateDirectory(in);
	}

	/**
	 * Create a client for doing a file import, while trying to use the first
	 * protocol matching the given list.<br/>
	 * 
	 * @param path - the file name (relative to storage root)
	 * @param preferredProtocols -
	 *            a list of protocols. The first supported one is used
	 * @return {@link FileTransferClient}
	 * @throws IOException if protocols are not supported, or no matching client class is found
	 */
	public FileTransferClient getExport(String path,
			ProtocolType.Enum... preferredProtocols) throws IOException {
		return getExport(path, null, preferredProtocols);
	}

	/**
	 * Create a client for doing a file import, while trying to use the first
	 * protocol matching the given list.<br/>
	 * 
	 * @param path - the file name (relative to storage root) 
	 * @param extraParameters - protocol specific extra parameters
	 * @param preferredProtocols - a list of protocols. The first supported one is used
	 * @return {@link FileTransferClient}
	 * @throws IOException if protocols are not supported, or no matching client class is found
	 */
	public FileTransferClient getExport(String path,
			Map<String,String>extraParameters,
			ProtocolType.Enum... preferredProtocols) throws IOException {
		ProtocolType.Enum protocol = findSupportedProtocol(preferredProtocols);
		Class<? extends FileTransferClient> clazz = getFiletransferClientClass(protocol);
		EndpointReferenceType ftEpr=null;
		try {
			ExportFileDocument efd = ExportFileDocument.Factory.newInstance();
			ExportFile export = efd.addNewExportFile();
			export.setProtocol(protocol);
			export.setSource(path);
			export.setExtraParameters(makeExtraParameters(extraParameters, String.valueOf(protocol)));
			ftEpr = ExportFile(efd).getExportFileResponse().getExportEPR();
		} catch (Exception e) {
			String msg=Log.createFaultMessage("Can't create export.",e);
			throw new IOException(msg,e);
		}
		try{
			String url = ftEpr.getAddress().getStringValue();
			FileTransferClient fts= clazz.getConstructor(
					new Class[] { String.class, EndpointReferenceType.class,
							IClientConfiguration.class }).newInstance(
									new Object[] { url, ftEpr, getSecurityConfiguration() });
			if(fts instanceof Configurable){
				((Configurable)fts).configure(extraParameters);
			}
			return fts;
		} catch (Exception e) {
			String msg=Log.createFaultMessage("Can't instantiate export client for protocol <"+protocol+">", e);
			throw new IOException(msg,e);
		}
	}

	/**
	 * Create a client for downloading data from the given path
	 * 
	 * @param path
	 */
	public HttpFileTransferClient download(String path) throws IOException {
		return (HttpFileTransferClient) getExport(path, ProtocolType.BFT);
	}
	
	/**
	 * returns the protocols supported by this storage
	 * 
	 * @return an array of {@link ProtocolType} enumerated types
	 * @throws Exception
	 */
	public ProtocolType.Enum[] getSupportedProtocols() throws Exception {
		return getResourcePropertiesDocument()
			.getStorageProperties().getProtocolArray();
	}

	/**
	 * Convenience method for accessing a remote file directly via its URI
	 * 
	 * @param uri -
	 *            URI of the remote file (including the protocol to use)
	 * @param sec -
	 *            security settings
	 * @param writeTo -
	 *            OutputStream to write the data to
	 * 
	 * @throws Exception
	 */
	public static void download(URI uri, IClientConfiguration sec,
			OutputStream writeTo) throws Exception{
		ProtocolType.Enum protocol = ProtocolType.Enum.forString(uri
				.getScheme());
		String storageUrl = uri.getSchemeSpecificPart();
		String relativePath = uri.getFragment();
		EndpointReferenceType storageEpr = EndpointReferenceType.Factory.newInstance();
		storageEpr.addNewAddress().setStringValue(storageUrl);
		StorageClient storageClient = new StorageClient(storageUrl,	storageEpr, sec);
		FileTransferClient ftc=storageClient.getExport(relativePath, protocol);
		ftc.readAllData(writeTo);
		writeTo.flush();
	}

	/**
	 * Create a client for doing a file import, while trying to use the first
	 * protocol matching the given list<br/>.
	 * 
	 * @param path - the path of the file (relative to storage root)
	 * @param append - whether to append in case the file exists
	 * @param extraParameters - additional protocol dependent parameters
	 * @param numBytes - the number of bytes to upload
	 * @param protocols - the protocols to choose from
	 *
	 * @return {@link FileTransferClient}
	 * @throws IOException
	 *             if no suitable protocol can be found, or no matching client
	 *             class is found
	 */
	public FileTransferClient createFileImport(String path, boolean append, Map<String,String>extraParameters, long numBytes, ProtocolType.Enum... protocols) 
			throws IOException {
		ProtocolType.Enum protocol = findSupportedProtocol(protocols);
		Class<? extends FileTransferClient> clazz = getFiletransferClientClass(protocol);
		EndpointReferenceType ftEpr=null;
		try {
			ImportFileDocument ifd = ImportFileDocument.Factory.newInstance();
			ImportFile in = ifd.addNewImportFile();
			in.setProtocol(protocol);
			in.setOverwrite(!append);
			in.setDestination(path);
			in.setExtraParameters(makeExtraParameters(extraParameters, String.valueOf(protocol)));
			if(numBytes>-1)in.setNumBytes(BigInteger.valueOf(numBytes));
			
			ftEpr = ImportFile(ifd).getImportFileResponse().getImportEPR();
			String url = ftEpr.getAddress().getStringValue();
			FileTransferClient fts=clazz.getConstructor(
					new Class[] { String.class, EndpointReferenceType.class,
							IClientConfiguration.class}).newInstance(
									new Object[] { url, ftEpr, getSecurityConfiguration()});
			fts.setAppend(append);
			if(fts instanceof Configurable){
				((Configurable)fts).configure(extraParameters);
			}
			return fts;
		} catch (Exception e) {
			String msg=Log.createFaultMessage("Can't create import.", e);
			throw new IOException(msg,e);
		}

	}
	
	/**
	 * Create a client for doing a file import, while trying to use the first
	 * protocol matching the given list<br/>.
	 * 
	 * @param path - the path of the file (relative to storage root)
	 * @param append - whether to append in case the file exists
	 * @param extraParameters - additional protocol dependent parameters
	 * @param protocols - the protocols to choose from
	 *
	 * @return {@link FileTransferClient}
	 * @throws IOException
	 *             if no suitable protocol can be found, or no matching client
	 *             class is found
	 */
	public FileTransferClient getImport(String path, boolean append, Map<String,String>extraParameters,  ProtocolType.Enum... protocols) 
	throws IOException {
		return createFileImport(path, append, extraParameters, -1, protocols);
	}
	
	/**
	 * Create a client for doing a file import, while trying to use the first
	 * protocol matching the given list<br/>. An existing file will be overwritten.
	 * 
	 * @param path - path of the file relative to storage root
	 * @param preferredProtocols -
	 *            a list of protocols. The first supported one is used
	 * @return {@link FileTransferClient}
	 * @throws IOException
	 *             if no suitable protocol can be found, or no matching client
	 *             class is found
	 */
	public FileTransferClient getImport(String path,
			ProtocolType.Enum... preferredProtocols) throws IOException {
		return getImport(path,false,null,preferredProtocols);
	}

	/**
	 * create a client for uploading data to the given path
	 * 
	 * @param path
	 */
	public HttpFileTransferClient upload(String path) throws IOException {
		return (HttpFileTransferClient) getImport(path, ProtocolType.BFT);
	}
	
	/**
	 * get an {@link EnumerationClient} listing the server-to-server file transfers
	 * or <code>null</code> if the server does not support this (i.e. is version &lt; 1.5.0) 
	 * 
	 * @see #getServerVersion()
	 * @throws Exception
	 */
	public EnumerationClient<EndpointReferenceDocument> getFiletransferEnumeration() throws Exception{
		EndpointReferenceType epr=getResourcePropertiesDocument().getStorageProperties().getFiletransferEnumerationReference();
		if(epr!=null){
			EnumerationClient<EndpointReferenceDocument>c=new EnumerationClient<EndpointReferenceDocument>(epr,getSecurityConfiguration(),EndpointReferenceDocument.type.getDocumentElementName());
			c.setUpdateInterval(-1);
			return c;
		}
		return null;
	}

	private static ServiceLoader<FiletransferParameterProvider>parameterProviders=null;

	/**
	 * convert the given parameters map into a ExtraParameters document 
	 * 
	 * @param params - map containing parameters. If <code>null</code> or empty,
	 * an attempt is made to find default parameters using {@link FiletransferParameterProvider}
	 * instances 
	 * @param protocol - filetransfer protocol, or <code>null</code> if not known. In the latter case,
	 * all parameter providers will be queried
	 */
	static ExtraParameters makeExtraParameters(Map<String,String>params, String protocol){
		synchronized (StorageClient.class) {
			if(parameterProviders==null){
				parameterProviders=ServiceLoader.load(FiletransferParameterProvider.class);
			}
		}
		if(params==null){
			params=new HashMap<String, String>();
		}
		Iterator<FiletransferParameterProvider>iter=parameterProviders.iterator();
		while(iter.hasNext()){
			FiletransferParameterProvider f=iter.next();
			f.provideParameters(params, protocol);
		}
		ExtraParameters r=ExtraParameters.Factory.newInstance();
		if(params!=null){
			for(Map.Entry<String, String>e: params.entrySet()){
				PropertyType t=r.addNewParameter();
				t.setName(e.getKey());
				t.setValue(e.getValue());
			}
		}
		return r;
	}

	/**
	 * Gets the first supported protocol, or null if no match found
	 * 
	 * @param preferredProtocols - the list of protocols to check
	 * @return the first protocol supported by this storage
	 * @throws IOException - if the supported protocols can not be determined
	 */
	public ProtocolType.Enum findSupportedProtocol(ProtocolType.Enum... preferredProtocols)
	throws IOException {
		try {
			for (ProtocolType.Enum p : preferredProtocols) {
				for (ProtocolType.Enum test : getSupportedProtocols()) {
					if (test.equals(p))
						return p;
				}
			}
		} catch (Exception e) {
			throw new IOException("Can't get protocols.",e);
		}
		throw new IOException("None of the file transfer protocols "+
				Arrays.asList(preferredProtocols)+" is supported by the storage!");

	}

	/**
	 * Client side find... consider using the "serverside" find method if possible
	 * 
	 * @param topFolder is root for the search
	 * @param filter representing searchCriteria relative to topFolder
	 * @return search result
	 */
	public GridFileType[] find (String topFolder, IGridFileFilter filter) throws BaseFault{
		GridFileType[] retval = null;
		List<GridFileType> list;

		if (topFolder ==null || "".equals(topFolder) || filter == null){
			return retval;
		}
		if ("/".equals(topFolder)){
			list = recursiveFind(topFolder, filter);
		}
		else{
			filter.setCriteria(topFolder + filter.getInitCriteria());
			list = recursiveFind(topFolder, filter);
		}
		retval = new GridFileType[list.size()];
		for (int i = 0; i < retval.length; i++) {
			retval[i] = list.get(i);
		}
		return retval;
	}
	
	@SuppressWarnings("unchecked")
	protected Class<? extends FileTransferClient> getFiletransferClientClass(ProtocolType.Enum protocol) throws IOException {
		Class<? extends FileTransferClient> clazz = null; 
		String className = System.getProperty(String.valueOf(protocol)+".clientClass");
		if(className != null){
			try{
				clazz = (Class<? extends FileTransferClient>)(Class.forName(className));
			}catch(Exception ex){
				throw new IOException("Custom client class <"+className+"> for protocol <"+protocol+"> cannot be instantiated!", ex);
			}
		}
		else{
			clazz = registeredClients.get(protocol);
		}
		if (clazz == null){
			throw new IOException("No matching client class supporting the <"
					+ protocol + "> protocol found.");
		}
		return clazz;
	}

	/**
	 * 
	 * @param topFolder path to search root element
	 * @param filter representing the search criteria (with absolute path)
	 * @return search result
	 * @throws BaseFault
	 */
	private List<GridFileType> recursiveFind (String topFolder, IGridFileFilter filter) throws BaseFault{
		GridFileType[] childs=null;
		List<GridFileType> collect = new ArrayList<GridFileType>();
		List<GridFileType> subfolders = new ArrayList<GridFileType>();

		try{
			childs = listDirectory(topFolder);
		}catch(BaseFault ignored){ }

		// proceed child elements of topFolder
		if (childs == null){
			return collect;
		}
		for (int i = 0; i < childs.length; i++) {
			if (childs[i].getIsDirectory()){
				subfolders.add(childs[i]);
			}
			if (filter.match(childs[i])){
				collect.add(childs[i]);
			}
		}
		// proceed subfolders if necessary
		for (int i = 0; i < subfolders.size(); i++) {
			if (filter.browseSubfolder(subfolders.get(i))) {
				String subFolder = subfolders.get(i).getPath();
				collect.addAll(recursiveFind(subFolder, filter));
			}
		}
		return collect;
	}


	//register client classes via the META-INF/services mechanism
	private static synchronized void initRegisteredClients(){
		if(registeredClients.size()==0){
			ServiceLoader<ClientCapabilities>sl=ServiceLoader.load(ClientCapabilities.class);
			Iterator<ClientCapabilities>iter=sl.iterator();
			while(iter.hasNext()){
				ClientCapability[]cs=iter.next().getClientCapabilities();
				for(int j=0; j<cs.length;j++){
					ClientCapability c=cs[j];
					if(c instanceof FTClientCapability){
						FTClientCapability ftc=(FTClientCapability)c;
						doRegister(ftc.getProtocol(), ftc.getImplementation());
					}
				}
			}
		}
	}

}
