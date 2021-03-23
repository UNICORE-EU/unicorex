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


package de.fzj.unicore.uas.impl.sms.ws;

import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.logging.log4j.Logger;
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

import de.fzj.unicore.uas.StorageManagement;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.UASProperties;
import de.fzj.unicore.uas.fts.ws.ProtocolRenderer;
import de.fzj.unicore.uas.impl.UASBaseFrontEnd;
import de.fzj.unicore.uas.impl.UmaskRenderer;
import de.fzj.unicore.uas.impl.bp.BPSupportImpl;
import de.fzj.unicore.uas.impl.sms.SMSBaseImpl;
import de.fzj.unicore.uas.impl.sms.SMSUtils;
import de.fzj.unicore.uas.metadata.MetadataManager;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.wsrflite.ContainerProperties;
import de.fzj.unicore.wsrflite.utils.Utilities;
import de.fzj.unicore.wsrflite.xmlbeans.BaseFault;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.AddressRenderer;
import de.fzj.unicore.xnjs.io.ACLEntry;
import de.fzj.unicore.xnjs.io.CompositeFindOptions;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import de.fzj.unicore.xnjs.io.Permissions;
import de.fzj.unicore.xnjs.io.XnjsFile;
import de.fzj.unicore.xnjs.io.XnjsFileWithACL;
import eu.unicore.services.ws.utils.WSServerUtilities;
import eu.unicore.util.ConcurrentAccess;

/**
 * Basic Storage Management service implementation
 * 
 * @author schuller
 */
public class SMSFrontend extends UASBaseFrontEnd implements StorageManagement {

	private static final Logger logger = LogUtil.getLogger(LogUtil.DATA, SMSFrontend.class);

	public static final QName RPInternalFiletransferReference=EndpointReferenceDocument.type.getDocumentElementName();

	private final SMSBaseImpl resource;
	
	@Override
	protected void addWSResourceInterfaces(BPSupportImpl baseProfile) {
		super.addWSResourceInterfaces(baseProfile);
		baseProfile.addWSResourceInterface(SMS_PORT);
	}

	public SMSFrontend(SMSBaseImpl r) {
		super(r);
		this.resource = r;
		
		addRenderer(new ACLSupportedRP(r));
		addRenderer(new FiletransferReferenceRP(r));
		addRenderer(new ProtocolRenderer(r){
			public List<ProtocolType.Enum> getProtocols(){
				return getAvailableProtocols();
			}
		});
		addRenderer(new FileSystemRP(r));
		addRenderer(new UmaskRenderer(r));
		addRenderer(new TriggerSupportedRenderer(r));

		AddressRenderer ftListAddress = new AddressRenderer(r, RPFiletransferEnumerationReference,true){
			@Override
			protected String getServiceSpec() {
				return UAS.ENUMERATION+"?res="+r.getModel().getFileTransferEnumerationID();
			}
		};
		addRenderer(ftListAddress);

		AddressRenderer mdAddress = new AddressRenderer(r, RPMetadataServiceReference,true){
			@Override
			protected String getServiceSpec() {
				String metadataServiceID = r.getModel().getMetadataServiceID();
				if(metadataServiceID==null)return null;
				return UAS.META+"?res="+metadataServiceID;
			}
		};
		addRenderer(mdAddress);
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
			String file = resource.makeSMSLocal(in.getPath());
			IStorageAdapter tsi = resource.getStorageAdapter();

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
			resource.copy(in.getCopy().getSource(),in.getCopy().getDestination());
			CopyResponseDocument res=CopyResponseDocument.Factory.newInstance();
			res.addNewCopyResponse();
			return res;
		}
		catch(Exception e){
			throw BaseFault.createFault("Could not copy.",e);
		}
	}

	/**
	 * create a directory: assumes path relative to storage root
	 */
	@ConcurrentAccess(allow=true)
	public CreateDirectoryResponseDocument CreateDirectory(CreateDirectoryDocument in) throws BaseFault {
		try{
			resource.mkdir(in.getCreateDirectory().getPath());
			CreateDirectoryResponseDocument res=CreateDirectoryResponseDocument.Factory.newInstance();
			res.addNewCreateDirectoryResponse();
			return res;
		}catch(Exception e){
			LogUtil.logException("Could not create directory.",e,logger);
			throw BaseFault.createFault("Could not create directory.",e);
		}
	}
	
	@ConcurrentAccess(allow=true)
	public DeleteResponseDocument Delete(DeleteDocument in) throws BaseFault {
		try{
			resource.doDelete(in.getDelete().getPathArray());
			DeleteResponseDocument res=DeleteResponseDocument.Factory.newInstance();
			res.addNewDeleteResponse();
			return res;
		}
		catch(Exception e){
			throw BaseFault.createFault("Could not perform delete.",e);
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
			String id = resource.createFileExport(source, protocol, parseExtraParameters(param));
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
			String destination = request.getDestination();
			ProtocolType.Enum protocol = request.getProtocol();
			Boolean overwrite = Boolean.TRUE;
			if(request.isSetOverwrite()){
				overwrite=Boolean.valueOf(request.getOverwrite());
			}
			Map<String,String> param = parseExtraParameters(request.getExtraParameters());
			long numBytes = -1;
			if(request.getNumBytes()!=null){
				numBytes = request.getNumBytes().longValue();
			}
			String id = resource.createFileImport(destination,protocol,overwrite,numBytes,param);

			ImportFileResponseDocument res = ImportFileResponseDocument.Factory.newInstance();
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
			String source = in.getReceiveFile().getSource();
			String target = in.getReceiveFile().getDestination();
			ExtraParameters param = in.getReceiveFile().getExtraParameters();
			String id = resource.transferFile(source, target, false, parseExtraParameters(param));
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
			String id = resource.transferFile(source, target, true, parseExtraParameters(param));
			EndpointReferenceType epr = WSServerUtilities.makeEPR(UAS.SERVER_FTS,id,kernel);
			WSServerUtilities.addUGSRefparamToEpr(epr,id);
			SendFileResponseDocument res = SendFileResponseDocument.Factory.newInstance();
			res.addNewSendFileResponse().setSendFileEPR(epr);
			return res;
		} catch (Exception e) {
			throw BaseFault.createFault("Could not initiate send file.",e);
		}
	}
	
	@ConcurrentAccess(allow=true)
	public ListDirectoryResponseDocument ListDirectory(ListDirectoryDocument in) throws BaseFault {
		ListDirectoryResponseDocument resd = ListDirectoryResponseDocument.Factory.newInstance();
		ListDirectoryResponse res = resd.addNewListDirectoryResponse();
		BigInteger offsetP = in.getListDirectory().getOffset();
		int offset = ( offsetP!=null?offsetP.intValue():0) ;
		BigInteger limitP = in.getListDirectory().getLimit();
		int max = resource.getProperties().getIntValue(UASProperties.SMS_LS_LIMIT);
		int limit = limitP!=null ? limitP.intValue() : max;
		if(limit>max){
			String msg="Could not list directory: the requested number of results " +
					"exceeds the internal limit of <"+max+">. " +
					"Please use the limit and offset parameters!";
			logger.warn(msg);
			throw BaseFault.createFault(msg);
		}
		//get list from tsi
		try{
			String p = resource.makeSMSLocal(in.getListDirectory().getPath());
			XnjsFile[] tsifiles = resource.getListing(p, offset, limit, resource.getModel().getStorageDescription().isFilterListing());
			for(XnjsFile f: tsifiles){
				GridFileType gf = res.addNewGridFile();
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
		String request = in.getListProperties().getPath();
		if(request==null)throw BaseFault.createFault("Could not list properties: target path is null.");
		ListPropertiesResponseDocument res = ListPropertiesResponseDocument.Factory.newInstance();
		res.addNewListPropertiesResponse();
		try{
			String path = resource.makeSMSLocal(request);
			XnjsFileWithACL file = resource.getProperties(path);
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
			resource.rename(in.getRename().getSource(), in.getRename().getDestination());
			RenameResponseDocument res=RenameResponseDocument.Factory.newInstance();
			res.addNewRenameResponse();
			return res;
		}
		catch(Exception e){
			throw BaseFault.createFault("Could not rename",e);
		}
	}
	
	@ConcurrentAccess(allow=true)	
	public FindResponseDocument Find(FindDocument in) throws BaseFault {
		FindResponseDocument resd=FindResponseDocument.Factory.newInstance();
		FindResponse res=resd.addNewFindResponse();
		try{
			Find find=in.getFind();
			String base=find.getBase();
			CompositeFindOptions opts = resource.getXNJSFacade().getFindOptions(find.getFilter());
			if(find.isSetRecurse()){
				opts.setRecurse(find.getRecurse());
			}
			//perform find on tsi
			String p = resource.makeSMSLocal(base);
			if(logger.isTraceEnabled()){
				logger.trace("Listing <"+p+"> workdir=" + resource.getStorageRoot());
			}
			IStorageAdapter tsi = resource.getStorageAdapter();
			XnjsFile[] tsifiles=tsi.find(base, opts, -1, -1);
			for(XnjsFile f: tsifiles){
				if(resource.getModel().getStorageDescription().isFilterListing() && !f.isOwnedByCaller()){
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
	 * get the enabled and available file transfer protocols
	 */
	public List<ProtocolType.Enum>getAvailableProtocols(){
		List<ProtocolType.Enum> res = new ArrayList<>();
		for(String p: resource.getAvailableProtocols()) {
			try{
				ProtocolType.Enum e = ProtocolType.Enum.forString(p);
				if(e!=null)res.add(e);
			}catch(Exception e) {}
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
			MetadataManager mm = resource.getMetadataManager();
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

	private EndpointReferenceType toEPR(String ftID, ProtocolType.Enum protocol){
		EndpointReferenceType epr = WSServerUtilities.newEPR(kernel.getContainerSecurityConfiguration());
		if(!resource.getModel().getEnableDirectFiletransfer()){
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



}
