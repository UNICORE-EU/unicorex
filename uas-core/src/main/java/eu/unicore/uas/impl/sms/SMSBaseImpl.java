package eu.unicore.uas.impl.sms;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import eu.unicore.client.data.UFTPConstants;
import eu.unicore.security.Client;
import eu.unicore.security.Xlogin;
import eu.unicore.services.Home;
import eu.unicore.services.InitParameters;
import eu.unicore.services.messaging.ResourceDeletedMessage;
import eu.unicore.uas.UAS;
import eu.unicore.uas.fts.FileTransferCapability;
import eu.unicore.uas.fts.FiletransferInitParameters;
import eu.unicore.uas.impl.PersistingPreferencesResource;
import eu.unicore.uas.impl.UmaskSupport;
import eu.unicore.uas.metadata.MetadataManager;
import eu.unicore.uas.metadata.MetadataSupport;
import eu.unicore.uas.trigger.impl.SetupDirectoryScan;
import eu.unicore.uas.trigger.xnjs.ScanSettings;
import eu.unicore.uas.util.LogUtil;
import eu.unicore.uas.xnjs.StorageAdapterFactory;
import eu.unicore.uas.xnjs.TSIStorageAdapterFactory;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.io.ACLEntry.Type;
import eu.unicore.xnjs.io.ChangeACL;
import eu.unicore.xnjs.io.ChangeACL.ACLChangeMode;
import eu.unicore.xnjs.io.FileSet;
import eu.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.xnjs.io.XnjsFile;
import eu.unicore.xnjs.io.XnjsFileWithACL;
import eu.unicore.xnjs.tsi.BatchMode;
import eu.unicore.xnjs.tsi.TSI;

/**
 * Basic storage resource implementation
 * 
 * @author schuller
 */
public abstract class SMSBaseImpl extends PersistingPreferencesResource implements UmaskSupport {

	private static final Logger logger = LogUtil.getLogger(LogUtil.DATA, SMSBaseImpl.class);

	/**
	 * the maximum number of results to return in a single getListing() operation
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
		// we get a factory ID if this instance was created
		// via a StorageFactory
		String storageFactoryID=init.factoryID;
		if(storageFactoryID != null){
			model.setParentUID(storageFactoryID);
			model.setParentServiceName(UAS.SMF);
		}
		model.inheritSharing = init.inheritSharing;
	}
	
	public void copy(String source, String target) throws Exception {
		source = makeSMSLocal(source);
		target = makeSMSLocal(target);
		getStorageAdapter().cp(source, target);
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
		getStorageAdapter().mkdir(path);
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
	 * creates server-server transfer instance
	 *
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
		return kernel.getHome(UAS.SERVER_FTS).createResource(init);
	}
	
	public void rename(String source, String target) throws Exception {
		source = makeSMSLocal(source);
		target = makeSMSLocal(target);
		MetadataManager mm = getMetadataManager();
		if(mm != null){
			mm.renameResource(source, target);
		}
		getStorageAdapter().rename(source, target);
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
		return getStorageAdapter().ls(path,offset,limit,filter);
	}

	/**
	 * gets the properties for 'path'
	 * @param path - a path relative to storage root
	 * 
	 * @return {@link XnjsFile} or <code>null</code> if path does not exist
	 * @throws Exception in case of problems performing the request
	 */
	public XnjsFileWithACL getProperties(String path)throws Exception{
		return getStorageAdapter().getProperties(path);
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
	 * create a new file import resource
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
		String target = makeSMSLocal(file);
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
	 * create a new file export resource
	 *
	 * @param file
	 * @param protocol
	 * @param extraParameters
	 * 
	 * @return UID of the new filetransfer resource
	 * @throws Exception
	 */
	public String createFileExport(String file, String protocol, Map<String,String>extraParameters)
			throws Exception {
		FiletransferInitParameters init = new FiletransferInitParameters();
		String source = makeSMSLocal(file);
		if(!source.contains(UFTPConstants.SESSION_TAG) && getProperties(source)==null){
			throw new FileNotFoundException("File <"+source+"> not found on storage");
		}
		init.source = source;
		init.isExport = true;
		init.extraParameters.putAll(extraParameters);
		if(!source.contains(UFTPConstants.SESSION_TAG) && getMetadataManager()!=null){
			String ct = getMetadataManager().getMetadataByName(source).get("Content-Type");
			if(ct!=null){
				init.mimetype = ct;
			}
		}
		return createFileTransfer(init, protocol);
	}


	/**
	 * create new client-server FileTransfer resource and return its UUID
	 * 
	 * @param initParam - the initialisation parameters
	 * @param protocol - the protocol to use
	 * @return UUID
	 */
	private String createFileTransfer(FiletransferInitParameters initParam, String protocol)
			throws Exception{
		initParam.smsUUID = getUniqueID();
		initParam.workdir = getStorageRoot();
		if(initParam.workdir==null) {
			throw new Exception("Storage is not ready.");
		}
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
		TSI ret = getXNJSFacade().getTSI(client,null);
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
					logger.warn("No user ID configured for data triggering on shared storage <{}>", getUniqueID());
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
			logger.info("Have directory scan <{}> for <{}>", uid, getClient().getDistinguishedName());
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
		ChangeACL change = new ChangeACL(Type.GROUP, gid, aclSpec, false, ACLChangeMode.MODIFY);
		ChangeACL change2 = new ChangeACL(Type.GROUP, gid, aclSpec, true, ACLChangeMode.MODIFY);
		getStorageAdapter().setfacl(getStorageRoot(), false, new ChangeACL[] {change, change2}, true);
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
