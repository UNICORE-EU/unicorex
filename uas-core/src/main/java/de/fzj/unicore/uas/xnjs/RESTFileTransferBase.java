package de.fzj.unicore.uas.xnjs;

import java.io.IOException;
import java.util.Map;

import org.apache.logging.log4j.Logger;

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
import eu.unicore.client.Endpoint;
import eu.unicore.client.data.FiletransferClient;
import eu.unicore.security.Client;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.services.rest.jwt.JWTDelegation;
import eu.unicore.services.rest.jwt.JWTServerProperties;
import eu.unicore.services.utils.Utilities;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;


/**
 * Base class for UNICORE RESTful file staging <br/>
 * There are subclasses {@link RESTFileImportBase} for import and {@link RESTFileExportBase} for export.
 * 
 * @author schuller
 * @author demuth
 */
public abstract class RESTFileTransferBase implements IFileTransfer, ProgressListener<Long> {

	protected static final Logger logger = LogUtil.getLogger(LogUtil.DATA, RESTFileTransferBase.class);

	//metrics/usage logger
	protected static final Logger usageLogger = Log.getLogger(Log.SERVICES+".datatransfer.USAGE", RESTFileTransferBase.class);

	protected OverwritePolicy overwrite = OverwritePolicy.OVERWRITE;

	protected ImportPolicy importPolicy = ImportPolicy.PREFER_COPY;

	protected String workdir;

	protected Client client;

	protected Endpoint storageEndpoint;

	protected Endpoint fileTransferEndpoint;

	protected IClientConfiguration sec;
	
	protected IAuthCallback auth;
	
	protected boolean export;

	protected FiletransferClient ftc;

	private TSI tsi;

	private volatile boolean isCancelled = false;
	
	protected IStorageAdapter storageAdapter;

	protected final XNJS configuration;
	
	protected final Kernel kernel;
	
	protected long startTime = -1;
	
	protected long finishTime = -1;
	
	protected final TransferInfo info;
	
	protected StatusTracker statusTracker;
	
	public RESTFileTransferBase(XNJS configuration){
		this.configuration=configuration;
		this.kernel=configuration.get(Kernel.class);
		this.info = new TransferInfo(Utilities.newUniqueID(), null, null);
		this.info.setProtocol("BFT");
	}

	public TransferInfo getInfo(){
		return info;
	}
	
	/**
	 * cleanup references to reduce memory footprint: this class might
	 * reside in memory for quite some time, so it should reduce memory
	 * consumption as soon as the transfer has been completed
	 */
	protected void onFinishCleanup(){
		ftc=null;
		client=null;
		storageAdapter=null;
		storageEndpoint=null;
		storageEndpoint=null;
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
	
	public Endpoint getStorageEndpoint() {
		return storageEndpoint;
	}

	public void setStorageEndpoint(Endpoint storageEndpoint) {
		this.storageEndpoint = storageEndpoint;
	}

	protected void initSecurityProperties()
	{
		try{
			Kernel kernel = configuration.get(Kernel.class);
			sec = kernel.getClientConfiguration().clone();
			String user = client.getDistinguishedName();
			auth = new JWTDelegation(kernel.getContainerSecurityConfiguration(), 
					new JWTServerProperties(kernel.getContainerProperties().getRawProperties()), user);
		}catch(NullPointerException npe){
			logger.warn("No security info available, running in non-secure mode?");
		}
	}
	
	protected void destroyFileTransferResource()
	{
		try{
			if(ftc!=null){
				ftc.delete();
			}
		}
		catch(Exception e){
			Log.logException("Could not destroy file transfer resource.", e, logger);
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
		IStorageAdapter sms = getStorageAdapter();
		XnjsFile parent=sms.getProperties(s);
		if(parent==null){
			//create
			sms.mkdir(s);
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
		long r = (long)((float)dataSize/(float)consumedMillis);
		String what=isExport()?"sent":"received";
		String dn=client!=null?client.getDistinguishedName():"anonymous";
		String url=storageEndpoint.getUrl();
		logger.debug("{} {} bytes in {} milliseconds, data rate={} kB/s)", what, dataSize, consumedMillis);
		usageLogger.info("[{}] [{}] [{}] [{} kB/s] [{}] [{}] [{}] [{}] [{}]"
				    ,dn, what, dataSize, r,
				    url, info.getSource(), info.getTarget(), info.getProtocol(),
				    info.getParentActionID());
	}
	
}
