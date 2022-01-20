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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.fts.FileTransferCapability;
import de.fzj.unicore.uas.fts.FiletransferInitParameters;
import de.fzj.unicore.uas.impl.PersistingPreferencesResource;
import de.fzj.unicore.uas.impl.UmaskSupport;
import de.fzj.unicore.uas.metadata.MetadataManager;
import de.fzj.unicore.uas.metadata.MetadataSupport;
import de.fzj.unicore.uas.trigger.impl.SetupDirectoryScan;
import de.fzj.unicore.uas.trigger.xnjs.ScanSettings;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.uas.xnjs.StorageAdapterFactory;
import de.fzj.unicore.uas.xnjs.TSIStorageAdapterFactory;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.io.ACLEntry.Type;
import de.fzj.unicore.xnjs.io.ChangeACL;
import de.fzj.unicore.xnjs.io.ChangeACL.ACLChangeMode;
import de.fzj.unicore.xnjs.io.FileSet;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import de.fzj.unicore.xnjs.io.XnjsFile;
import de.fzj.unicore.xnjs.io.XnjsFileWithACL;
import de.fzj.unicore.xnjs.tsi.BatchMode;
import de.fzj.unicore.xnjs.tsi.TSI;
import eu.unicore.security.Client;
import eu.unicore.security.Xlogin;
import eu.unicore.services.Home;
import eu.unicore.services.InitParameters;
import eu.unicore.services.messaging.Message;
import eu.unicore.services.messaging.PullPoint;
import eu.unicore.services.messaging.ResourceAddedMessage;
import eu.unicore.services.messaging.ResourceDeletedMessage;
import eu.unicore.uftp.server.workers.UFTPWorker;

/**
 * Basic storage resource implementation
 * 
 * @author schuller
 */
public abstract class SMSBaseImpl extends PersistingPreferencesResource implements UmaskSupport {

	private static final Logger logger = LogUtil.getLogger(LogUtil.DATA, SMSBaseImpl.class);

	/**
	 * the maximum number of results to return in a single ListDirectory() opeSMSBaseImplration
	 */
	public static final int MAX_LS_RESULTS = 10000;

	public SMSBaseImpl() {
		super();
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

		setResourceStatus(ResourceStatus.READY);
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
	public abstract String getStorageRoot() throws ExecutionException;
	
	public void mkdir(String path) throws Exception {
		path = makeSMSLocal(path);
		IStorageAdapter tsi=getStorageAdapter();
		tsi.mkdir(path);
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
			catch(Exception e){}
		}
		
		String scanUID = model.getDirectoryScanUID();
		if(scanUID != null){
			try{
				getXNJSFacade().destroyAction(scanUID, getClient());
			}catch(Exception ex){}
		}
		
		if (model.storageDescription.isCleanupOnDestroy()) {
			try{
				getStorageAdapter().rmdir("/");
			}catch(Exception ex){}
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
	public String createFileImport(String file,String protocol, boolean overwrite, 
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
	public String createFileExport(String file, String protocol,Map<String,String>extraParameters)
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
	private String createFileTransfer(FiletransferInitParameters initParam, String protocol)
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
		if(sep==null)sep=getTSI().getFileSeparator();
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

	/**
	 * get the enabled and available file transfer protocols
	 */
	public List<String>getAvailableProtocols(){
		List<String> res = new ArrayList<>();
		for(FileTransferCapability c: kernel.getCapabilities(FileTransferCapability.class)){
			String p = c.getProtocol();
			if(p!=null && c.isAvailable() && !"U6".equals(p) && !(p.contains("-"))){
				if(isProtocolAllowed(p))res.add(p);
			}
		}
		return res;
	}

	public boolean isProtocolAllowed(String protocol) {
		return true;
	}

}
