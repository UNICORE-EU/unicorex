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


package de.fzj.unicore.uas.impl.sms;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.unigrids.services.atomic.types.ACLEntryType;
import org.unigrids.services.atomic.types.ACLEntryTypeType;
import org.unigrids.services.atomic.types.ACLType;
import org.unigrids.services.atomic.types.GridFileType;
import org.unigrids.services.atomic.types.MetadataType;
import org.unigrids.services.atomic.types.PropertyType;
import org.unigrids.services.atomic.types.ProtocolType;
import org.unigrids.services.atomic.types.TextInfoType;
import org.unigrids.x2006.x04.services.sms.ChangeACLType;
import org.unigrids.x2006.x04.services.sms.ChangePermissionsDocument;
import org.unigrids.x2006.x04.services.sms.ChangePermissionsResponseDocument;
import org.unigrids.x2006.x04.services.sms.CopyDocument;
import org.unigrids.x2006.x04.services.sms.CopyResponseDocument;
import org.unigrids.x2006.x04.services.sms.CreateDirectoryDocument;
import org.unigrids.x2006.x04.services.sms.CreateDirectoryResponseDocument;
import org.unigrids.x2006.x04.services.sms.DeleteDocument;
import org.unigrids.x2006.x04.services.sms.DeleteResponseDocument;
import org.unigrids.x2006.x04.services.sms.ExportFileDocument;
import org.unigrids.x2006.x04.services.sms.ExportFileResponseDocument;
import org.unigrids.x2006.x04.services.sms.ExtendedChangePermissionsType;
import org.unigrids.x2006.x04.services.sms.ExtraParametersDocument.ExtraParameters;
import org.unigrids.x2006.x04.services.sms.FindDocument;
import org.unigrids.x2006.x04.services.sms.FindDocument.Find;
import org.unigrids.x2006.x04.services.sms.FindResponseDocument;
import org.unigrids.x2006.x04.services.sms.FindResponseDocument.FindResponse;
import org.unigrids.x2006.x04.services.sms.ImportFileDocument;
import org.unigrids.x2006.x04.services.sms.ImportFileDocument.ImportFile;
import org.unigrids.x2006.x04.services.sms.ImportFileResponseDocument;
import org.unigrids.x2006.x04.services.sms.ListDirectoryDocument;
import org.unigrids.x2006.x04.services.sms.ListDirectoryResponseDocument;
import org.unigrids.x2006.x04.services.sms.ListDirectoryResponseDocument.ListDirectoryResponse;
import org.unigrids.x2006.x04.services.sms.ListPropertiesDocument;
import org.unigrids.x2006.x04.services.sms.ListPropertiesResponseDocument;
import org.unigrids.x2006.x04.services.sms.ReceiveFileDocument;
import org.unigrids.x2006.x04.services.sms.ReceiveFileResponseDocument;
import org.unigrids.x2006.x04.services.sms.RenameDocument;
import org.unigrids.x2006.x04.services.sms.RenameResponseDocument;
import org.unigrids.x2006.x04.services.sms.SendFileDocument;
import org.unigrids.x2006.x04.services.sms.SendFileResponseDocument;
import org.unigrids.x2006.x04.services.sms.StoragePropertiesDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.MetadataManagement;
import de.fzj.unicore.uas.StorageManagement;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.UASProperties;
import de.fzj.unicore.uas.client.BaseUASClient;
import de.fzj.unicore.uas.fts.FileTransferCapability;
import de.fzj.unicore.uas.fts.FiletransferInitParameters;
import de.fzj.unicore.uas.fts.ProtocolRenderer;
import de.fzj.unicore.uas.impl.BaseInitParameters;
import de.fzj.unicore.uas.impl.PersistingPreferencesResource;
import de.fzj.unicore.uas.impl.UmaskRenderer;
import de.fzj.unicore.uas.impl.UmaskSupport;
import de.fzj.unicore.uas.impl.bp.BPSupportImpl;
import de.fzj.unicore.uas.impl.enumeration.EnumerationInitParameters;
import de.fzj.unicore.uas.metadata.MetadataManagementImpl;
import de.fzj.unicore.uas.metadata.MetadataManager;
import de.fzj.unicore.uas.metadata.MetadataSupport;
import de.fzj.unicore.uas.trigger.impl.SetupDirectoryScan;
import de.fzj.unicore.uas.trigger.xnjs.ScanSettings;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.uas.xnjs.StorageAdapterFactory;
import de.fzj.unicore.uas.xnjs.TSIStorageAdapterFactory;
import de.fzj.unicore.wsrflite.ContainerProperties;
import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.InitParameters;
import de.fzj.unicore.wsrflite.InitParameters.TerminationMode;
import de.fzj.unicore.wsrflite.messaging.Message;
import de.fzj.unicore.wsrflite.messaging.PullPoint;
import de.fzj.unicore.wsrflite.messaging.ResourceAddedMessage;
import de.fzj.unicore.wsrflite.messaging.ResourceDeletedMessage;
import de.fzj.unicore.wsrflite.utils.Utilities;
import de.fzj.unicore.wsrflite.xmlbeans.BaseFault;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.AddressRenderer;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.io.ACLEntry;
import de.fzj.unicore.xnjs.io.ACLEntry.Type;
import de.fzj.unicore.xnjs.io.ChangeACL;
import de.fzj.unicore.xnjs.io.ChangeACL.ACLChangeMode;
import de.fzj.unicore.xnjs.io.CompositeFindOptions;
import de.fzj.unicore.xnjs.io.FileSet;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import de.fzj.unicore.xnjs.io.Permissions;
import de.fzj.unicore.xnjs.io.XnjsFile;
import de.fzj.unicore.xnjs.io.XnjsFileWithACL;
import de.fzj.unicore.xnjs.tsi.BatchMode;
import de.fzj.unicore.xnjs.tsi.TSI;
import eu.unicore.security.Client;
import eu.unicore.security.Xlogin;
import eu.unicore.services.ws.utils.WSServerUtilities;
import eu.unicore.uftp.server.workers.UFTPWorker;
import eu.unicore.util.ConcurrentAccess;
import eu.unicore.util.Log;

/**
 * Basic Storage Management service implementation
 * 
 * @author schuller
 */
public abstract class SMSBaseImpl extends PersistingPreferencesResource implements StorageManagement, UmaskSupport{

	private static final Logger logger = LogUtil.getLogger(LogUtil.DATA, SMSBaseImpl.class);

	/**
	 * the maximum number of results to return in a single ListDirectory() operation
	 */
	public static final int MAX_LS_RESULTS = 10000;

	public static final QName RPInternalFiletransferReference=EndpointReferenceDocument.type.getDocumentElementName();

	@Override
	protected void addWSResourceInterfaces(BPSupportImpl baseProfile) {
		super.addWSResourceInterfaces(baseProfile);
		baseProfile.addWSResourceInterface(SMS_PORT);
	}

	public SMSBaseImpl() {
		super();

		addRenderer(new ACLSupportedRP(this));
		addRenderer(new FiletransferReferenceRP(this));
		addRenderer(new ProtocolRenderer(this){
			public List<ProtocolType.Enum> getProtocols(){
				return getAvailableProtocols();
			}
		});
		addRenderer(new FileSystemRP(this));
		addRenderer(new UmaskRenderer(this));
		addRenderer(new TriggerSupportedRenderer(this));

		AddressRenderer ftListAddress=new AddressRenderer(this, RPFiletransferEnumerationReference,true){
			@Override
			protected String getServiceSpec() {
				return UAS.ENUMERATION+"?res="+getModel().fileTransferEnumerationID;
			}
		};
		addRenderer(ftListAddress);

		AddressRenderer mdAddress=new AddressRenderer(this, RPMetadataServiceReference,true){
			@Override
			protected String getServiceSpec() {
				String metadataServiceID = getModel().metadataServiceID;
				if(metadataServiceID==null)return null;
				return UAS.META+"?res="+metadataServiceID;
			}
		};
		addRenderer(mdAddress);
	}

	@Override
	public SMSModel getModel(){
		return (SMSModel)model;
	}

	@Override
	public void initialise(InitParameters initobjs)throws Exception{
		if(model == null){
			model = new SMSModel();
		}
		SMSModel model = getModel();

		super.initialise(initobjs);
		StorageInitParameters init = (StorageInitParameters)initobjs;
		
		if (init.storageDescription == null)
			throw new IllegalStateException("No storage configuration found");

		model.storageDescription = init.storageDescription;

		model.fsname = init.storageDescription.getName();
		
		model.umask = init.storageDescription.getDefaultUmask();
		
		//factory ID
		String storageFactoryID=init.factoryID;
		if(storageFactoryID != null){
			model.setParentUID(storageFactoryID);
			model.setParentServiceName(UAS.SMF);
		}

		model.inheritSharing = init.inheritSharing;

		//enumeration for server-server transfers
		model.fileTransferEnumerationID=createFTListEnumeration();

		setResourceStatus(ResourceStatus.READY);
	}


	@Override
	public QName getPortType() {
		return SMS_PORT;
	}

	@Override
	public QName getResourcePropertyDocumentQName() {
		return StoragePropertiesDocument.type.getDocumentElementName();
	}

	@ConcurrentAccess(allow=true)
	public ChangePermissionsResponseDocument ChangePermissions(ChangePermissionsDocument inDoc) 
			throws BaseFault {
		try{
			org.unigrids.x2006.x04.services.sms.ChangePermissionsDocument.ChangePermissions in = 
					inDoc.getChangePermissions();
			String file=makeSMSLocal(in.getPath());
			IStorageAdapter tsi=getStorageAdapter();

			boolean recursive = in.isSetRecursive() ? in.getRecursive() : false;

			//chmod
			ExtendedChangePermissionsType extendedCh = in.getExtendedPermissions();
			if (extendedCh == null || extendedCh.isNil()) {
				if (in.getPermissions() != null)
					SMSUtils.legacyChangePermissions(file, tsi, in);
			} else
				SMSUtils.extendedChangePermissions(file, tsi, extendedCh, recursive);

			//chgrp
			String newGroup = in.getChangeOwningGroup();
			if (in.isSetChangeOwningGroup() && newGroup != null)
				tsi.chgrp(file, newGroup, recursive);

			//setfacl
			ChangeACLType aclChange = in.getACL();
			if (in.isSetACL() && aclChange != null)
				SMSUtils.setACL(file, tsi, aclChange, recursive);

			ChangePermissionsResponseDocument res=ChangePermissionsResponseDocument.Factory.newInstance();
			res.addNewChangePermissionsResponse();
			return res;
		}
		catch(Exception e){
			throw BaseFault.createFault("Could not change permissions.",e);
		}
	}

	@ConcurrentAccess(allow=true)
	public CopyResponseDocument Copy(CopyDocument in) throws BaseFault {
		try{
			copy(in.getCopy().getSource(),in.getCopy().getDestination());
			CopyResponseDocument res=CopyResponseDocument.Factory.newInstance();
			res.addNewCopyResponse();
			return res;
		}
		catch(Exception e){
			throw BaseFault.createFault("Could not copy.",e);
		}
	}
	
	public void copy(String source, String target) throws Exception {
		source = makeSMSLocal(source);
		target=makeSMSLocal(target);
		IStorageAdapter tsi=getStorageAdapter();
		tsi.cp(source, target);
		MetadataManager mm = getMetadataManager();
		if(mm != null){
			mm.copyResourceMetadata(source, target);
		}
	}
	
	/**
	 * Retrieve the base dir of this storage.</br>
	 * If necessary, the base dir is resolved.
	 * It is assumed to end with the file separator.
	 */
	protected abstract String getStorageRoot() throws ExecutionException;

	/**
	 * create a directory: assumes path relative to storage root
	 */
	@ConcurrentAccess(allow=true)
	public CreateDirectoryResponseDocument CreateDirectory(CreateDirectoryDocument in) throws BaseFault {
		try{
			mkdir(in.getCreateDirectory().getPath());
			CreateDirectoryResponseDocument res=CreateDirectoryResponseDocument.Factory.newInstance();
			res.addNewCreateDirectoryResponse();
			return res;
		}catch(Exception e){
			LogUtil.logException("Could not create directory.",e,logger);
			throw BaseFault.createFault("Could not create directory.",e);
		}
	}
	
	public void mkdir(String path) throws Exception {
		path = makeSMSLocal(path);
		IStorageAdapter tsi=getStorageAdapter();
		tsi.mkdir(path);
	}

	@ConcurrentAccess(allow=true)
	public DeleteResponseDocument Delete(DeleteDocument in) throws BaseFault {
		try{
			doDelete(in.getDelete().getPathArray());
			DeleteResponseDocument res=DeleteResponseDocument.Factory.newInstance();
			res.addNewDeleteResponse();
			return res;
		}
		catch(Exception e){
			throw BaseFault.createFault("Could not perform delete.",e);
		}

	}
	
	public void doDelete(String... paths) throws Exception {
		IStorageAdapter tsi=getStorageAdapter();
		boolean supportsBatch=tsi instanceof BatchMode;
		MetadataManager mm = getMetadataManager();
		if(supportsBatch){
			((BatchMode)tsi).startBatch();
		}
		for(String inPath: paths){
			String path=makeSMSLocal(inPath);
			tsi.rmdir(path);
			if(mm != null)mm.removeMetadata(path);
		}
		if(supportsBatch){
			((BatchMode)tsi).commitBatch();
		}
	}
	
	/**
	 * export a file<br/>
	 * 
	 * the path will always be interpreted as relative to storage root 
	 */
	@ConcurrentAccess(allow=true)
	public ExportFileResponseDocument ExportFile(ExportFileDocument in) throws BaseFault {
		try {
			String source = in.getExportFile().getSource();
			ProtocolType.Enum protocol=in.getExportFile().getProtocol();
			ExtraParameters param=in.getExportFile().getExtraParameters();
			String id = createFileExport(source, protocol, parseExtraParameters(param));
			EndpointReferenceType epr=toEPR(id, protocol);
			ExportFileResponseDocument res=ExportFileResponseDocument.Factory.newInstance();
			res.addNewExportFileResponse().setExportEPR(epr);
			return res;
		} catch (Exception e) {
			throw BaseFault.createFault("Could not create file export.",e);
		}
	}


	@ConcurrentAccess(allow=true)
	public ImportFileResponseDocument ImportFile(ImportFileDocument in) throws BaseFault {
		try {
			ImportFile request = in.getImportFile();
			String destination=request.getDestination();
			ProtocolType.Enum protocol=request.getProtocol();
			Boolean overwrite=Boolean.TRUE;
			if(request.isSetOverwrite()){
				overwrite=Boolean.valueOf(request.getOverwrite());
			}
			Map<String,String> param=parseExtraParameters(request.getExtraParameters());
			long numBytes = -1;
			if(request.getNumBytes()!=null){
				numBytes = request.getNumBytes().longValue();
			}
			String id = createFileImport(destination,protocol,overwrite,numBytes,param);

			ImportFileResponseDocument res=ImportFileResponseDocument.Factory.newInstance();
			res.addNewImportFileResponse().setImportEPR(toEPR(id, protocol));
			return res;
		} catch (Exception e) {
			logger.error("",e);
			throw BaseFault.createFault("Could not create file import.",e);
		}
	}

	/**
	 * create a filetransfer that imports data from a remote SMS
	 * 
	 * @param in
	 * @throws BaseFault
	 */
	@ConcurrentAccess(allow=true)
	public ReceiveFileResponseDocument ReceiveFile(ReceiveFileDocument in)
			throws BaseFault{
		try{
			String source=in.getReceiveFile().getSource();
			String target=in.getReceiveFile().getDestination();
			ExtraParameters param=in.getReceiveFile().getExtraParameters();
			String id = transferFile(source, target, false, parseExtraParameters(param));
			EndpointReferenceType epr = WSServerUtilities.makeEPR(UAS.SERVER_FTS,id,kernel);
			WSServerUtilities.addUGSRefparamToEpr(epr,id);
			ReceiveFileResponseDocument res=ReceiveFileResponseDocument.Factory.newInstance();
			res.addNewReceiveFileResponse().setReceiveFileEPR(epr);
			return res;
		}catch(Exception e){
			throw BaseFault.createFault("Could not initiate receive file.",e);
		}
	}

	/**
	 * create a filetransfer that pushes data to a remote SMS
	 * 
	 * @param in
	 * @throws BaseFault
	 */
	@ConcurrentAccess(allow=true)
	public SendFileResponseDocument SendFile(SendFileDocument in)
			throws BaseFault {
		try {
			String source = in.getSendFile().getSource();
			String target = in.getSendFile().getDestination();
			ExtraParameters param=in.getSendFile().getExtraParameters();
			String id = transferFile(source, target, true, parseExtraParameters(param));
			EndpointReferenceType epr = WSServerUtilities.makeEPR(UAS.SERVER_FTS,id,kernel);
			WSServerUtilities.addUGSRefparamToEpr(epr,id);
			SendFileResponseDocument res = SendFileResponseDocument.Factory.newInstance();
			res.addNewSendFileResponse().setSendFileEPR(epr);
			return res;
		} catch (Exception e) {
			throw BaseFault.createFault("Could not initiate send file.",e);
		}
	}

	/**
	 * TODO controller method
	 * 
	 * create a server-to-server transfer
	 * @param source
	 * @param target
	 * @param isExport - if this is <code>true</code>, source is a file on this storage
	 * @param extraParameters
	 * @return UUID of the new transfer resource
	 * @throws Exception
	 */
	public String transferFile(String source, String target, boolean isExport,
			Map<String,String>extraParameters)
	throws Exception {
		if(isExport){
			boolean hasWildcards=FileSet.hasWildcards(source); 
			XnjsFileWithACL xnjsFile = hasWildcards?null:getProperties(source);
			if(!hasWildcards && xnjsFile == null)
			{
				throw new Exception("File "+source+" not found on this storage.");
			}
			if (!source.startsWith(getSeparator())){
				source = getSeparator() + source;
			}
		}
		else{
			if(!target.startsWith(getSeparator())){
				target = getSeparator() + target;
			}
		}
		FiletransferInitParameters init = new FiletransferInitParameters();
		init.target = target;
		init.source = source;
		init.isExport = isExport;
		
		if(extraParameters!=null){
			init.extraParameters.putAll(extraParameters);
		}
		return createTransferResource(init);
	}
	
	/**
	 * creates server-server transfer. Common things (like security info) will be added to 
	 * the initParameter map.<br/>
	 * Side effect: the EPR is added to the relevant resource property
	 * @param init - basic parameters 
	 * @return UUID of the new transfer resource
	 * @throws Exception
	 */
	protected String createTransferResource(FiletransferInitParameters init) throws Exception{
		init.umask = getUmask();
		init.smsUUID = getUniqueID();
		init.workdir = getStorageRoot();
		init.storageAdapterFactory = getStorageAdapterFactory();
		init.xnjsReference = getModel().getXnjsReference();
		String id=kernel.getHome(UAS.SERVER_FTS).createResource(init);
		ResourceAddedMessage m=new ResourceAddedMessage(UAS.SERVER_FTS,id);
		getKernel().getMessaging().getChannel(getUniqueID()).publish(m);
		return id;
	}

	@ConcurrentAccess(allow=true)
	public ListDirectoryResponseDocument ListDirectory(ListDirectoryDocument in) throws BaseFault {
		ListDirectoryResponseDocument resd=ListDirectoryResponseDocument.Factory.newInstance();
		ListDirectoryResponse res=resd.addNewListDirectoryResponse();
		BigInteger offsetP=in.getListDirectory().getOffset();
		int offset=( offsetP!=null?offsetP.intValue():0) ;
		BigInteger limitP=in.getListDirectory().getLimit();
		int max=uasProperties.getIntValue(UASProperties.SMS_LS_LIMIT);
		int limit=limitP!=null?limitP.intValue():max;
		if(limit>max){
			String msg="Could not list directory: the requested number of results " +
					"exceeds the internal limit of <"+max+">. " +
					"Please use the limit and offset parameters!";
			logger.warn(msg);
			throw BaseFault.createFault(msg);
		}
		//get list from tsi
		try{
			String p=makeSMSLocal(in.getListDirectory().getPath());
			XnjsFile[] tsifiles=getListing(p, offset, limit, getModel().storageDescription.isFilterListing());
			for(XnjsFile f: tsifiles){
				GridFileType gf=res.addNewGridFile();
				convert(f,gf);
			}
		}catch(Exception e){
			LogUtil.logException("Could not list directory.",e,logger);
			throw BaseFault.createFault("Could not list directory.",e);
		}
		return resd;
	}


	@ConcurrentAccess(allow=true)
	public ListPropertiesResponseDocument ListProperties(ListPropertiesDocument in) throws BaseFault {
		String request=in.getListProperties().getPath();
		if(request==null)throw BaseFault.createFault("Could not list properties: target path is null.");
		ListPropertiesResponseDocument res=ListPropertiesResponseDocument.Factory.newInstance();
		res.addNewListPropertiesResponse();
		try{
			String path=makeSMSLocal(request);
			XnjsFileWithACL file=getProperties(path);
			if(file!=null){
				GridFileType gf=res.getListPropertiesResponse().addNewGridFile();
				convert(file, gf, true);
			}
		}catch(Exception e){
			String msg="Could not list properties of <"+request+">.";
			throw BaseFault.createFault(msg,e);
		}
		return res;
	}

	@ConcurrentAccess(allow=true)
	public RenameResponseDocument Rename(RenameDocument in) throws BaseFault {
		try{
			rename(in.getRename().getSource(), in.getRename().getDestination());
			RenameResponseDocument res=RenameResponseDocument.Factory.newInstance();
			res.addNewRenameResponse();
			return res;
		}
		catch(Exception e){
			throw BaseFault.createFault("Could not rename",e);
		}
	}
	
	public void rename(String source, String target) throws Exception {
		source = makeSMSLocal(source);
		target=makeSMSLocal(target);
		MetadataManager mm = getMetadataManager();
		if(mm != null){
			mm.renameResource(source, target);
		}
		IStorageAdapter tsi=getStorageAdapter();
		tsi.rename(source, target);
	}

	@ConcurrentAccess(allow=true)	
	public FindResponseDocument Find(FindDocument in) throws BaseFault {
		FindResponseDocument resd=FindResponseDocument.Factory.newInstance();
		FindResponse res=resd.addNewFindResponse();
		try{
			Find find=in.getFind();
			String base=find.getBase();
			CompositeFindOptions opts=getXNJSFacade().getFindOptions(find.getFilter());
			if(find.isSetRecurse()){
				opts.setRecurse(find.getRecurse());
			}
			//perform find on tsi
			String p=makeSMSLocal(base);
			if(logger.isTraceEnabled()){
				logger.trace("Listing <"+p+"> workdir="+getStorageRoot());
			}
			IStorageAdapter tsi=getStorageAdapter();
			XnjsFile[] tsifiles=tsi.find(base, opts, -1, -1);
			for(XnjsFile f: tsifiles){
				if(getModel().storageDescription.isFilterListing() && !f.isOwnedByCaller()){
					if(logger.isTraceEnabled())logger.trace("Skipping "+f.getPath()+", not owned by caller.");
					continue;
				}
				GridFileType gf=res.addNewGridFile();
				convert(f,gf);
				if(logger.isTraceEnabled())logger.trace("XNJS filepath: "+f.getPath()+" -> "+gf.getPath());
			}

			return resd;
		}
		catch(Exception e){
			throw BaseFault.createFault("Could not perform find operation.",e);
		}
	}


	/**
	 * gets the contents for directory 'path'
	 * @param path - directory relative to storage root
	 * @param offset - offset into the directory listing
	 * @param limit - max number of results
	 * @param filter - whether to include files not owned by the current client
	 * @return an array of {@link XnjsFile}
	 * @throws ExecutionException
	 */
	public XnjsFile[] getListing(String path, int offset, int limit, boolean filter)throws Exception{
		IStorageAdapter tsi=getStorageAdapter();
		return tsi.ls(path,offset,limit,filter);
	}

	/**
	 * gets the properties for 'path'
	 * @param path - a path relative to storage root
	 * 
	 * @return {@link XnjsFile} or <code>null</code> if path does not exist
	 * @throws Exception in case of problems performing the request
	 */
	public XnjsFileWithACL getProperties(String path)throws Exception{
		IStorageAdapter tsi=getStorageAdapter();
		return tsi.getProperties(path);
	}

	/**
	 * convert a path to normal form.<br/> 
	 * <ul>
	 * <li>Replace "\" with "/" </li>
	 * <li>make sure the path starts with "/" </li>
	 * <li>decode special characters (spaces, ...) using urlDecode()</li>
	 * <li>replace duplicate slashes "//" with a single "/" </li>
	 * </ul>
	 */
	public String makeSMSLocal(String p){
		String res=SMSUtils.urlDecode(p).replace('\\','/');
		if(res.length()==0 || '/'!=res.charAt(0))res="/"+res;
		res=res.replace("//", "/");
		return res;
	}

	/**
	 * get the enabled and available file transfer protocols
	 */
	public List<ProtocolType.Enum>getAvailableProtocols(){
		List<ProtocolType.Enum> res = new ArrayList<>();
		for(FileTransferCapability c: kernel.getCapabilities(FileTransferCapability.class)){
			if(c.isAvailable() && !"U6".equals(c.getProtocol())){
				ProtocolType.Enum e = ProtocolType.Enum.forString(c.getProtocol());
				if(e!=null)res.add(e);
			}
		}
		return res;
	}

	/**
	 * converts from XNJSfile to GridFile
	 * 
	 * @param f - the XNJS file info
	 * @param gf - the Grid file info
	 * @param addMetadata - whether to attach metadata
	 */
	protected void convert(XnjsFile f, GridFileType gf, boolean addMetadata)throws Exception{
		gf.setPath(f.getPath());
		gf.setSize(f.getSize());
		gf.setIsDirectory(f.isDirectory());
		gf.setLastModified(f.getLastModified());
		Permissions p=f.getPermissions();
		gf.addNewPermissions().setReadable(p.isReadable());
		gf.getPermissions().setWritable(p.isWritable());
		gf.getPermissions().setExecutable(p.isExecutable());
		if(addMetadata){
			MetadataManager mm=getMetadataManager();
			try{
				if (mm!= null)
					attachMetadata(f,gf,mm);
			}catch(Exception ex){
				LogUtil.logException("Error attaching metadata for file "+f.getPath(), ex);
			}
		}
		if (f.getUNIXPermissions() != null)
			gf.setFilePermissions(f.getUNIXPermissions());
		if (f.getGroup() != null)
			gf.setGroup(f.getGroup());
		if (f.getOwner() != null)
			gf.setOwner(f.getOwner());

		if (f instanceof XnjsFileWithACL) {
			XnjsFileWithACL fWithACL = (XnjsFileWithACL) f;
			ACLEntry[] acl = fWithACL.getACL();
			if (acl != null) {
				ACLType xmlACL = gf.addNewACL();
				for (ACLEntry aclEntry: acl) {
					ACLEntryType xmlACLEntry = xmlACL.addNewEntry();
					xmlACLEntry.setPermissions(aclEntry.getPermissions());
					xmlACLEntry.setSubject(aclEntry.getSubject());
					if (aclEntry.getType() == ACLEntry.Type.GROUP)
						xmlACLEntry.setType(ACLEntryTypeType.GROUP);
					else
						xmlACLEntry.setType(ACLEntryTypeType.USER);
					xmlACLEntry.setDefaultACL(aclEntry.isDefaultACL());
				}
			}
		}
	}

	protected void attachMetadata(XnjsFile f, GridFileType gridFile, MetadataManager metaManager)throws Exception{
		if(gridFile.getIsDirectory())return;
		String resourceName=gridFile.getPath();
		Map<String,String> metadata=metaManager.getMetadataByName(resourceName);
		MetadataType md=gridFile.addNewMetadata();
		for(Map.Entry<String, String> item: metadata.entrySet()){
			String key=item.getKey();
			String value=item.getValue();
			if("Content-MD5".equalsIgnoreCase(key)){
				md.setContentMD5(value);
			}
			else if("Content-Type".equalsIgnoreCase(key)){
				md.setContentType(value);
			}
			else{
				TextInfoType t=md.addNewProperty();
				t.setName(key);
				t.setValue(value);
			}
		}
	}

	/**
	 * converts from XNJSfile to GridFileType
	 */
	protected void convert(XnjsFile f, GridFileType gf)throws Exception{
		convert(f,gf,false);
	}

	/**
	 * create an instance of the Enumeration service for publishing
	 * the list of filetransfers
	 * @return UUID of the new Enumeration instance
	 * @throws Exception
	 */
	protected String createFTListEnumeration()throws Exception{
		String uid = getUniqueID()+"_filetransfers";
		EnumerationInitParameters init = new EnumerationInitParameters(uid, TerminationMode.NEVER);
		init.parentUUID = getUniqueID();
		init.parentServiceName = getServiceName();
		init.targetServiceRP = RPInternalFiletransferReference;
		init.acl.addAll(getModel().getAcl());
		init.ownerDN=getModel().getOwnerDN();
		Home h=kernel.getHome(UAS.ENUMERATION);
		if(h==null)throw new Exception("Enumeration service is not deployed!");
		return h.createResource(init);
	}

	@Override
	public void processMessages(PullPoint p){
		List<String>fileTransferUIDs = getModel().getFileTransferUIDs();
		while(p.hasNext()){
			Message message=p.next();
			if(message instanceof ResourceDeletedMessage){
				ResourceDeletedMessage rdm=(ResourceDeletedMessage)message;
				String id=rdm.getDeletedResource();
				String service=rdm.getServiceName();
				if(UAS.SERVER_FTS.equals(service)){
					fileTransferUIDs.remove(id);
				}
			}
			else if(message instanceof ResourceAddedMessage){
				ResourceAddedMessage ram=(ResourceAddedMessage)message;
				String id=ram.getAddedResource();
				String service=ram.getServiceName();
				if(UAS.SERVER_FTS.equals(service)){
					fileTransferUIDs.add(id);
				}	
			}
		}
	}

	/**
	 * resource-specific destruction: cleanup instances of metadata and enumeration instances  
	 */
	@Override
	public void destroy() {
		SMSModel model = getModel();
		
		String storageFactoryID = model.getParentUID();
		if(storageFactoryID!=null){
			try{
				ResourceDeletedMessage m=new ResourceDeletedMessage("deleted:"+getUniqueID());
				m.setDeletedResource(getUniqueID());
				m.setServiceName(getServiceName());
				kernel.getMessaging().getChannel(storageFactoryID).publish(m);
			}
			catch(Exception e){
				LogUtil.logException("Could not send internal message.",e,logger);
			}
		}
		
		String metadataServiceID = model.metadataServiceID;
		if(metadataServiceID!=null){
			try{
				EndpointReferenceType md=WSServerUtilities.makeEPR(UAS.META, metadataServiceID, kernel);
				BaseUASClient c=new BaseUASClient(md, kernel.getClientConfiguration());
				c.destroy();
			}catch(Exception e){
				LogUtil.logException("Could not destroy metadata service instance.",e,logger);
			}
		}
		
		String enumID = model.fileTransferEnumerationID;
		if(enumID!=null){
			try{
				EndpointReferenceType md=WSServerUtilities.makeEPR(UAS.ENUMERATION, enumID, kernel);
				BaseUASClient c=new BaseUASClient(md, kernel.getClientConfiguration());
				c.destroy();
			}catch(Exception e){
				LogUtil.logException("Could not destroy enumeration service instance.",e,logger);
			}
		}

		String scanUID = model.getDirectoryScanUID();
		if(scanUID != null){
			try{
				getXNJSFacade().destroyAction(scanUID, getClient());
			}catch(Exception ex){
				LogUtil.logException("Could not abort directory scan with UID "+scanUID,ex,logger);
			}
		}
		
		if (model.storageDescription.isCleanupOnDestroy()) {
			try{
				//remove the storage root directory itself
				getStorageAdapter().rmdir("/");
			}catch(Exception ex){

			}
		}

		super.destroy();
	}

	/**
	 * TODO controller method
	 * 
	 * @param file
	 * @param protocol
	 * @param overwrite
	 * @param extraParameters
	 * 
	 * @return UID of the new filetransfer resource
	 * @throws Exception
	 */
	public String createFileImport(String file, ProtocolType.Enum protocol, boolean overwrite, 
			long numBytes, Map<String,String>extraParameters)
			throws Exception {
		FiletransferInitParameters init = new FiletransferInitParameters();
		String target=makeSMSLocal(file);
		checkImportTarget(target);
		init.target = target;
		init.overwrite = overwrite;
		init.isExport = false;
		init.numbytes = numBytes;
		init.extraParameters.putAll(extraParameters);
		createParentDirectories(target);
		return createFileTransfer(init, protocol);
	}
	

	/**
	 * TODO controller method
	 * 
	 * @param file
	 * @param protocol
	 * @param extraParameters
	 * 
	 * @return UID of the new filetransfer resource
	 * @throws Exception
	 */
	public String createFileExport(String file, ProtocolType.Enum protocol,Map<String,String>extraParameters)
			throws Exception {
		FiletransferInitParameters init = new FiletransferInitParameters();
		String source=makeSMSLocal(file);
		if(!source.contains(UFTPWorker.sessionModeTag) && getProperties(source)==null){
			throw new FileNotFoundException("File <"+source+"> not found on storage");
		}
		init.source = source;
		init.isExport = true;
		init.extraParameters.putAll(extraParameters);
		
		if(!source.contains(UFTPWorker.sessionModeTag) && getMetadataManager()!=null){
			String ct = getMetadataManager().getMetadataByName(source).get("Content-Type");
			if(ct!=null){
				init.mimetype = ct;
			}
		}
		return createFileTransfer(init, protocol);
	}


	/**
	 * create new client-server FileTransfer resource and return its EPR
	 * 
	 * @param initParam - the initialisation parameters
	 * @param protocol - the protocol to use
	 * @return EPR
	 */
	private String createFileTransfer(FiletransferInitParameters initParam, ProtocolType.Enum protocol)
			throws Exception{
		initParam.smsUUID = getUniqueID();
		initParam.workdir = getStorageRoot();
		initParam.umask = getUmask();
		initParam.storageAdapterFactory = getStorageAdapterFactory();
		initParam.xnjsReference = getModel().getXnjsReference();
		initParam.protocol = protocol;
		Home home=kernel.getHome(UAS.CLIENT_FTS);
		if(home==null)throw new Exception("Requested service <"+UAS.CLIENT_FTS+"> is not available.");
		return home.createResource(initParam);
	}

	private EndpointReferenceType toEPR(String ftID, ProtocolType.Enum protocol){
		EndpointReferenceType epr = WSServerUtilities.newEPR(kernel.getContainerSecurityConfiguration());
		if(!getModel().enableDirectFiletransfer){
			epr.addNewAddress().setStringValue(
					WSServerUtilities.makeAddress(UAS.CLIENT_FTS, ftID, 
							kernel.getContainerProperties()));
		}
		else{
			String serv=kernel.getContainerProperties().getValue(ContainerProperties.WSRF_SERVLETPATH);
			String baseAddr = Utilities.getPhysicalServerAddress(kernel.getContainerProperties(),
					kernel.getContainerSecurityConfiguration().isSslEnabled()); 
			String add=baseAddr+serv+"/"+UAS.CLIENT_FTS+"?res="+ftID;
			epr.addNewAddress().setStringValue(add);
			logger.debug("Direct filetransfer enabled, address= "+add);
		}
		WSServerUtilities.addUGSRefparamToEpr(epr, ftID);
		return epr;
	}

	/**
	 * perform sanity checks on the target of an import<br/>
	 * <ul>
	 *   <li>it must not be a directory
	 * </ul>
	 * 
	 * @param target - the target name for an import
	 * @throws ExecutionException if problems with XNJS/TSI occur
	 * @throws IllegalArgumentException - if target is invalid
	 */
	protected void checkImportTarget(String target)throws Exception{
		XnjsFile f=getProperties(target);
		if(f==null)return;
		if(f.isDirectory())throw new IllegalArgumentException("Invalid target filename "+target+": is a directory");
	}

	private String sep=null;
	
	/**
	 * gets the file separator string. Override this if providing a non-TSI storage.
	 * @return the separator char returned by the {@link TSI#getFileSeparator()}
	 */
	protected String getSeparator(){
		if(sep==null){
			try{
				sep=getTSI().getFileSeparator();
			}catch(ExecutionException ex){
				LogUtil.logException("Could not get file separator", ex, logger);
				sep="/";
			}
		}
		return sep;
	}

	/**
	 * creates any missing directories
	 */
	public void createParentDirectories(String target)throws Exception{
		while(target.startsWith("/"))target=target.substring(1);
		String[] dirs=target.split("/");
		String dir="";
		if(dirs.length>1 && dirs[0].length()!=0){
			//build directory
			int i=0;
			while(i<dirs.length-1){
				if(i>0)dir+="/";
				dir+=dirs[i];
				i++;
			}
		}
		if(dir.length()>0){
			String path=dir;
			IStorageAdapter tsi=getStorageAdapter();
			XnjsFile xDir=tsi.getProperties(path);
			if(xDir==null){
				logger.debug("Creating directory "+path);
				tsi.mkdir(path);	
			}
			else if(!xDir.isDirectory()){
				throw new IOException("</"+dir+"> already exists on this storage and is not a directory");
			}
		}
	}

	/**
	 * get the {@link IStorageAdapter} to use to access the backend storage for import
	 * and export of data.<br/>
	 */
	public IStorageAdapter getStorageAdapter()throws Exception{
		TSI tsi=getTSI();
		tsi.setStorageRoot(getStorageRoot());
		return tsi;
	}

	private TSI getTSI(){
		Client client=getClient();
		TSI ret = getXNJSFacade().getTargetSystemInterface(client);
		ret.setUmask(getUmask());
		return ret;
	}

	/**
	 * get the {@link StorageAdapterFactory} to use to create 
	 * an {@link IStorageAdapter} for the the backend storage 
	 * This default implementation returns a {@link TSIStorageAdapterFactory} instance, meaning the 
	 * filesystem on the target system will be used as storage
	 */
	protected StorageAdapterFactory getStorageAdapterFactory(){
		return new TSIStorageAdapterFactory();
	}

	/**
	 * get the {@link MetadataManager}<br/>
	 * This default implementation will return a storage metadata manager
	 * @return {@link MetadataManager} or <code>null</code> if disabled
	 */
	public MetadataManager getMetadataManager()throws Exception{
		if(getModel().storageDescription.isDisableMetadata())return null;
		return MetadataSupport.getManager(kernel, getStorageAdapter(), getUniqueID());
	}


	/**
	 * which class to use as {@link MetadataManagement} implementation
	 * @return class name of the MM implementation
	 */
	protected String getMetadataManagementImplClassName(){
		return MetadataManagementImpl.class.getName();
	}

	protected String createMetadataServiceInstance(){
		Home mdHome=kernel.getHome(UAS.META);
		String mdID = null;
		if(mdHome!=null){
			try{
				BaseInitParameters init = new BaseInitParameters(getUniqueID()+"_metadata", TerminationMode.NEVER);
				init.parentUUID = getUniqueID();
				init.acl.addAll(getModel().getAcl());
				init.ownerDN=getModel().getOwnerDN();
				mdID = mdHome.createResource(init);
			}catch(Exception ex){
				Log.logException("Could not create metadata service instance", ex, logger);
			}
		}
		return mdID;
	}

	/**
	 * creates the metadata service instance and adds a resource property 
	 * holding its address 
	 */
	protected void setupMetadataService()throws Exception{
		SMSModel m = getModel();
		if(!m.storageDescription.isDisableMetadata()){
			m.metadataServiceID=createMetadataServiceInstance();
		}
	}

	protected void setupDirectoryScan()throws Exception{
		if(isTriggerEnabled()){
			Client scanClient = getClient();
			ScanSettings scanSettings=new ScanSettings();
			scanSettings.storageUID=getUniqueID();
			if(!"user".equals(scanClient.getRole().getName())){
				String userID = getModel().storageDescription.getSharedTriggerUser();
				if(userID == null){
					logger.warn("No user ID configured for data triggering on shared storage <"+getUniqueID()+">");
					return;
				}
				scanClient = new Client();
				Xlogin xlogin = new Xlogin(new String[]{userID});
				scanClient.setXlogin(xlogin);
				scanSettings.sharedStorageMode=true;
			}
			SetupDirectoryScan sds=new SetupDirectoryScan(scanSettings, 
					scanClient, getXNJSFacade().getXNJS());
			
			String uid = sds.call();
			logger.info("Have directory scan "+uid+" for <"+getClient().getDistinguishedName()+">");
			getModel().setDirectoryScanUID(uid);
		}
	}

	public boolean isTriggerEnabled(){
		SMSModel m = getModel();
		return m.storageDescription!=null && m.storageDescription.isEnableTrigger();
	}


	protected EndpointReferenceType createRemoteStorageEPR(URI uri)
	{
		EndpointReferenceType epr=EndpointReferenceType.Factory.newInstance();
		String withoutScheme=uri.getSchemeSpecificPart();
		String upToFragment = withoutScheme.split("#")[0];
		epr.addNewAddress().setStringValue(upToFragment);
		return epr;
	}

	private Map<String,String> parseExtraParameters(ExtraParameters param){
		Map<String,String>result=new HashMap<String, String>();
		if(param!=null){
			PropertyType[]params=param.getParameterArray();
			for(PropertyType p: params){
				result.put(p.getName(),p.getValue());
			}
		}
		return result;
	}

	public String getUmask() {
		return getModel().umask;
	}

	public void setUmask(String umask) {
		getModel().umask=umask;
		getTSI().setUmask(umask);
	}

	public StorageDescription getStorageDescription() {
		return getModel().storageDescription;
	}


	protected void setNormalAndDefACL(String gid, String aclSpec) throws Exception {
		IStorageAdapter storage = getStorageAdapter();

		ChangeACL change = new ChangeACL(Type.GROUP, gid, aclSpec, false, ACLChangeMode.MODIFY);
		ChangeACL change2 = new ChangeACL(Type.GROUP, gid, aclSpec, true, ACLChangeMode.MODIFY);

		storage.setfacl(getStorageRoot(), false, new ChangeACL[] {change, change2}, true);
	}


}
