package de.fzj.unicore.uas.xnjs;

import java.io.IOException;
import java.util.Map;

import javax.security.auth.x500.X500Principal;

import org.apache.logging.log4j.Logger;
import org.unigrids.services.atomic.types.PropertyType;
import org.unigrids.x2006.x04.services.sms.ExtraParametersDocument.ExtraParameters;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.client.FileTransferClient;
import de.fzj.unicore.uas.fts.ProgressListener;
import de.fzj.unicore.uas.fts.StatusTracker;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.io.IFileTransfer;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import de.fzj.unicore.xnjs.io.TransferInfo;
import de.fzj.unicore.xnjs.io.TransferInfo.Status;
import de.fzj.unicore.xnjs.io.XnjsFile;
import de.fzj.unicore.xnjs.tsi.TSI;
import eu.unicore.security.Client;
import eu.unicore.services.Kernel;
import eu.unicore.services.security.ETDAssertionForwarding;
import eu.unicore.services.utils.Utilities;
import eu.unicore.services.ws.WSUtilities;
import eu.unicore.util.httpclient.IClientConfiguration;


/**
 * Base class for UNICORE file staging <br/>
 * There are subclasses {@link U6FileImportBase} for import and {@link U6FileExportBase} for export.
 * 
 * @author schuller
 * @author demuth
 * @since 1.1.3
 */
public abstract class U6FileTransferBase implements IFileTransfer, ProgressListener<Long> {

	protected static final Logger logger = LogUtil.getLogger(LogUtil.DATA,U6FileTransferBase.class);

	//metrics/usage logger
	protected static final Logger usageLogger = LogUtil.getLogger(LogUtil.SERVICES+".datatransfer.USAGE", U6FileTransferBase.class);

	protected OverwritePolicy overwrite=OverwritePolicy.OVERWRITE;

	protected ImportPolicy importPolicy = ImportPolicy.PREFER_COPY;

	protected String workdir;

	protected Client client;

	protected EndpointReferenceType smsEPR;

	protected EndpointReferenceType fileTransferInstanceEpr;

	protected IClientConfiguration sec;
	
	protected boolean export;

	protected FileTransferClient ftc;

	private TSI tsi=null;

	private volatile boolean isCancelled=false;
	
	protected IStorageAdapter storageAdapter;

	protected final XNJS configuration;
	
	protected final Kernel kernel;
	
	protected long startTime=-1;
	
	protected long finishTime=-1;
	
	protected final TransferInfo info;
	
	protected StatusTracker statusTracker;
	
	public U6FileTransferBase(XNJS configuration){
		this.configuration=configuration;
		this.kernel=configuration.get(Kernel.class);
		this.info = new TransferInfo(Utilities.newUniqueID(), null, null);
	}

	public TransferInfo getInfo(){
		return info;
	}
	
	/**
	 * create a {@link FileTransferClient} correctly configured 
	 * for this transfer<br/>
	 * The file transfer EPR is already available 
	 * as {@link #fileTransferInstanceEpr}<br/>
	 * 
	 * @return {@link FileTransferClient}
	 * @throws Exception
	 */
	protected abstract FileTransferClient getFTClient()throws Exception;
	
	/**
	 * cleanup references to reduce memory footprint: this class might
	 * reside in memory for quite some time, so it should reduce memory
	 * consumption as soon as the transfer has been completed
	 */
	protected void onFinishCleanup(){
		ftc=null;
		client=null;
		storageAdapter=null;
		smsEPR=null;
		fileTransferInstanceEpr=null;
		sec=null;
		tsi=null;
	}
	
	public void setClient(Client client){
		this.client=client;
	}

	@Override
	public void abort() {
		isCancelled=true;
	}

	public void setOverwritePolicy(OverwritePolicy overwrite) {
		this.overwrite=overwrite;
	}

	public boolean isOverwrite() {
		return OverwritePolicy.OVERWRITE.equals(overwrite);
	}

	public void setImportPolicy(ImportPolicy policy){
		this.importPolicy = policy;
	}
	
	public ImportPolicy getImportPolicy(){
		return importPolicy;
	}
	
	/**
	 * increase the total number of transferred bytes by the given amount 
	 * 
	 * @param amount - the number of transferred bytes
	 */
	public void notifyProgress(Long amount) {
		info.setTransferredBytes(info.getTransferredBytes()+amount);
		//update finish time, so we have an up-to-date rate estimate
		finishTime=System.currentTimeMillis();
	}

	/**
	 * if the transfer has been cancelled, this method will 
	 * throw a CancelledException
	 * @see #abort()
	 * @throws ProgressListener.CancelledException
	 */
	public void checkCancelled()throws ProgressListener.CancelledException{
		if(isCancelled)throw new ProgressListener.CancelledException();
	}
	
	public boolean isCancelled(){
		return isCancelled;
	}
	
	public void setStorageAdapter(IStorageAdapter adapter) {
		this.storageAdapter=adapter;
	}

	public String getWorkdir() {
		return workdir;
	}
	
	public void setWorkdir(String workdir) {
		this.workdir = workdir;
	}
	
	public Client getClient() {
		return client;
	}
	
	public EndpointReferenceType getSmsEPR() {
		return smsEPR;
	}

	public void setSmsEPR(EndpointReferenceType smsEPR) {
		this.smsEPR = smsEPR;
	}

	protected void initSecurityProperties()
	{
		try{
			Kernel kernel=configuration.get(Kernel.class);
			sec=kernel.getClientConfiguration().clone();
			boolean etd;
			X500Principal receiver = WSUtilities.extractServerX500Principal(smsEPR);
			if (receiver == null) {
				logger.debug("SMS DN is null. It won't be possible to work on user's behalf");
				etd=ETDAssertionForwarding.configureETD(client, sec);
			} else {
				logger.debug("Extracted SMS DN. It will be possible to work on user's behalf");
				etd=ETDAssertionForwarding.configureETDChainExtension(client, sec, receiver);
			}
			logger.debug("Creating filetransfer for client "+client+"\nUsing ETD: "+etd);
		}catch(NullPointerException npe){
			logger.warn("No security info available, running in non-secure mode?");
		}
	}
	
	/**
	 * do a WSRF destroy on the file transfer
	 */
	protected void destroyFileTransferResource()
	{
		try{
			if(ftc!=null){
				ftc.destroy();
			}
		}
		catch(Exception e){
			logger.error("Could not destroy file transfer resource.",e);
		}
	}

	protected synchronized IStorageAdapter getStorageAdapter()throws ExecutionException{
		if(storageAdapter!=null)return storageAdapter;
		if(tsi==null){
			tsi=configuration.getTargetSystemInterface(client);
			tsi.setStorageRoot(workdir);
		}
		return tsi;
	}

	/**
	 * checks if we may overwrite the given file (in case it exists)
	 * 
	 * @param fileName -  the file to check
	 * @throws IOException - if the file exists and DONT_OVERWRITE is requested
	 */
	protected void checkOverwriteAllowed(IStorageAdapter adapter, String fileName)throws Exception{
		if(OverwritePolicy.DONT_OVERWRITE.equals(overwrite)){
			XnjsFile f=adapter.getProperties(fileName);
			if(f!=null){
				throw new IOException("File <"+f.getPath()+"> exists, and we have been asked to not overwrite.");
			}
		}
	}

	/**
	 * normalise path by making sure only the local file separator 
	 * (i.e. IStorageAdapter#getFileSeparator()) is used, and by 
	 * replacing multiple slashes by single ones
	 * 
	 * @throws ExecutionException
	 */
	protected String cleanLocalFilePath(String file) throws ExecutionException
	{
		return file.replaceAll("/+","/").replace("\\/","/").replace("/", getStorageAdapter().getFileSeparator());
	}
	
	/**
	 * creates any missing directories
	 */
	protected void createParentDirectories(String target)throws ExecutionException{
		String s = getParentOfLocalFilePath(target);
		XnjsFile parent=getStorageAdapter().getProperties(s);
		if(parent==null){
			//create
			getStorageAdapter().mkdir(s);
		}
		else if(!parent.isDirectory()){
			throw new ExecutionException("Parent <"+s+"> is not a directory");
		}
	}
	
	/**
	 * get the parent file path
	 * @param file
	 * @return the parent, computed by first calling {@link #cleanLocalFilePath(String)} 
	 * and then cutting off the file name
	 * @throws ExecutionException
	 */
	protected String getParentOfLocalFilePath(String file) throws ExecutionException
	{
		String result = cleanLocalFilePath(file);
		int i=result.lastIndexOf(getStorageAdapter().getFileSeparator());
		return i>0 ? result.substring(0,i) : "/" ;
	}

	/**
	 * get the file name
	 * @param filePath
	 * @return the filename obtained by cutting off the path
	 * @throws ExecutionException
	 */
	protected String getFileName(String filePath) throws ExecutionException
	{
		int index=filePath.lastIndexOf("/");
		if(index>-1){
			return filePath.substring(filePath.lastIndexOf("/")+1);
		}
		return filePath;
	}
	
	public void setExtraParameters(Map<String,String>params){
		// can be overriden in sub-classes for configuring any 
		// protocol-dependent stuff
	}
	
	/**
	 * setup a map containing protocol dependent extra parameters
	 * @return {@link Map} or <code>null</code>
	 */
	protected Map<String,String>getExtraParameters(){
		return null;
	}

	protected final ExtraParameters convert(Map<String,String>params){
		ExtraParameters r=ExtraParameters.Factory.newInstance();
		for(Map.Entry<String, String>e: params.entrySet()){
			PropertyType t=r.addNewParameter();
			t.setName(e.getKey());
			t.setValue(e.getValue());
		}
		return r;
	}
	
	public boolean isExport() {
		return export;
	}

	public void setExport(boolean export) {
		this.export = export;
	}
	
	/**
	 * get the time that this transfer is/was running
	 * 
	 * @return elapsed time or -1 if not known yet
	 */
	public long getElapsedTime(){
		return finishTime>0 && startTime>0 ? (finishTime-startTime) : -1 ;
	}

	/**
	 * helper to set the status and message in case of cancel
	 */
	public void cancelled(){
		info.setStatus(Status.ABORTED, "File transfer cancelled.");
	}
	
	/**
	 * helper to set the status and message in case of failure
     * @param msg - error message to set		
	 */
	public void failed(String msg){
		info.setStatus(Status.FAILED,msg);
	}
	
	public void setStatusTracker(StatusTracker tracker) {
		this.statusTracker = tracker;
	}
	
	/**
	 * computes transfer rates, bytes transferred and
	 * logs it to the USAGE logger at INFO level<br/>
	 * (If needed, Log4j can be configured to send these log messages to a 
	 * specific file instead of the standard logfile)
	 * <p>
	 * The format is:
	 * [clientDN] [Sent|Received] [bytes] [kb/sec] [SMS URL] [source] [target] [protocol] [parentJobID]   
	 * <p>
	 */
	protected void computeMetrics(){
		finishTime=System.currentTimeMillis();
		long dataSize = info.getDataSize();
		long consumedMillis=getElapsedTime();
		float r=(float)dataSize/(float)consumedMillis;
		String what=isExport()?"sent":"received";
		String dn=client!=null?client.getDistinguishedName():"anonymous";
		String url=getSmsEPR().getAddress().getStringValue();
		
		if(logger.isDebugEnabled()){
			logger.debug(what+" "+dataSize+" bytes in "+consumedMillis+" milliseconds, data rate= "+r + " kB/s");
		}
		//usage logging
		if(usageLogger.isInfoEnabled()){
			usageLogger.info("["+dn+"]" + " ["+what+"]" + " ["+dataSize+"]" + " ["+r+" kB/s]" +" ["+url+"]"
		                  +" ["+info.getSource()+"]" + " ["+info.getTarget()+"]" 
					      +" ["+info.getProtocol()+"]" + " ["+info.getParentActionID()+"]");
		}
	}
	
}
