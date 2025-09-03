package eu.unicore.uas.jclouds.s3;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import eu.unicore.security.Client;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.jwt.JWTDelegation;
import eu.unicore.services.rest.jwt.JWTServerProperties;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.services.utils.Utilities;
import eu.unicore.uas.fts.ProgressListener;
import eu.unicore.uas.fts.StatusTracker;
import eu.unicore.uas.util.LogUtil;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.io.IFileTransfer;
import eu.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.xnjs.io.TransferInfo;
import eu.unicore.xnjs.io.TransferInfo.Status;
import eu.unicore.xnjs.io.XnjsFile;


/**
 * Base class for file staging from/to S3 <br/>
 * 
 * @author schuller
 */
public abstract class S3FileTransferBase implements IFileTransfer, ProgressListener<Long> {

	protected static final Logger logger = LogUtil.getLogger(LogUtil.DATA, S3FileTransferBase.class);

	protected static final Logger usageLogger = Log.getLogger(Log.SERVICES+".datatransfer.USAGE", S3FileTransferBase.class);

	protected OverwritePolicy overwrite = OverwritePolicy.OVERWRITE;

	protected ImportPolicy importPolicy = ImportPolicy.PREFER_COPY;

	protected String workdir;

	protected Client client;

	protected IClientConfiguration sec;

	protected IAuthCallback auth;

	protected boolean export;

	private String preferredLoginNode;

	private volatile boolean isCancelled = false;

	protected IStorageAdapter localStorage;

	protected final XNJS configuration;

	protected final Kernel kernel;

	protected long startTime = -1;

	protected long finishTime = -1;

	protected final TransferInfo info;

	protected StatusTracker statusTracker;

	protected Map<String,String> s3Params;
	
	protected IStorageAdapter s3Adapter;

	protected String permissions = null;

	public S3FileTransferBase(XNJS configuration){
		this.configuration=configuration;
		this.kernel=configuration.get(Kernel.class);
		this.info = new TransferInfo(Utilities.newUniqueID(), null, null);
		this.info.setProtocol("S3");
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
		client=null;
		localStorage=null;
		sec=null;
	}
	
	public void setClient(Client client){
		this.client=client;
	}

	@Override
	public void abort() {
		isCancelled=true;
	}

	@Override
	public void setOverwritePolicy(OverwritePolicy overwrite) {
		this.overwrite=overwrite;
	}

	public boolean isOverwrite() {
		return OverwritePolicy.OVERWRITE.equals(overwrite);
	}

	@Override
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
	@Override
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

	@Override
	public boolean isCancelled(){
		return isCancelled;
	}

	@Override
	public void setStorageAdapter(IStorageAdapter adapter) {
		this.localStorage=adapter;
	}

	public String getWorkdir() {
		return workdir;
	}

	public void setWorkdir(String workdir) {
		this.workdir = workdir;
	}

	@Override
	public void setPermissions(String permissions) {
		this.permissions = permissions;
	}

	public Client getClient() {
		return client;
	}

	@Override
	public void setPreferredLoginNode(String loginNode) {
		this.preferredLoginNode = loginNode;
	}

	protected void initSecurityProperties()
	{
		try{
			Kernel kernel = configuration.get(Kernel.class);
			sec = kernel.getClientConfiguration();
			String user = client.getDistinguishedName();
			auth = new JWTDelegation(kernel.getContainerSecurityConfiguration(), 
					new JWTServerProperties(kernel.getContainerProperties().getRawProperties()), user);
		}catch(NullPointerException npe){
			logger.warn("No security info available, running in non-secure mode?");
		}
	}

	protected synchronized IStorageAdapter getLocalStorage()throws ExecutionException{
		if(localStorage==null) {
			localStorage = configuration.getTargetSystemInterface(client, preferredLoginNode);
			localStorage.setStorageRoot(workdir);
		}
		return localStorage;
	}

	protected IStorageAdapter createS3Adapter() throws IOException {
		String accessKey = s3Params.get("accessKey");
		String secretKey = s3Params.get("secretKey");
		String endpoint = s3Params.get("endpoint");
		String bucket = s3Params.get("bucket");
		String provider = s3Params.get("provider");
		boolean sslValidate = Boolean.parseBoolean(s3Params.get("validate"));
		if(provider==null)provider="aws-s3";
		if(bucket==null)throw new IllegalArgumentException("Parameter 'bucket' is required");
		if(endpoint==null)throw new IllegalArgumentException("Parameter 'endpoint' is required");
		return new S3StorageAdapterFactory().createStorageAdapter(kernel, accessKey, secretKey, 
				endpoint, provider, bucket, null, sslValidate);
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
		return file.replaceAll("/+","/").replace("\\/","/").replace("/", getLocalStorage().getFileSeparator());
	}

	/**
	 * creates any missing directories
	 */
	protected void createParentDirectories(String target)throws ExecutionException{
		String s = getParentOfLocalFilePath(target);
		IStorageAdapter sms = getLocalStorage();
		XnjsFile parent=sms.getProperties(s);
		if(parent==null){
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
		int i=result.lastIndexOf(getLocalStorage().getFileSeparator());
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

	/**
	 * setup a map containing protocol dependent extra parameters
	 */
	public void setS3Params(Map<String,String> params){
		this.s3Params = params;
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
		String what=isExport()?"Sent":"Received";
		String dn=client!=null?client.getDistinguishedName():"anonymous";
		logger.debug("{} {} bytes in {} milliseconds, data rate={} kB/s)", what, dataSize, consumedMillis, r);
		usageLogger.info("[{}] [{}] [{}] [{} kB/s] [{}] [{}] [{}] [{}]"
				    ,dn, what, dataSize, r,
				    info.getSource(), info.getTarget(), info.getProtocol(),
				    info.getParentActionID());
	}
	
	//copy all data from an input stream to an output stream, while tracking progress via
	//the transferredBytes field
	protected void copy(InputStream in, OutputStream out)throws Exception{
		int bufferSize=65536;
		byte[] buffer = new byte[bufferSize];
		int len=0;
		long transferredBytes=0;
		while (!isCancelled) {
			len=in.read(buffer,0,bufferSize);
			if(len<0)break;
			out.write(buffer,0,len);
			transferredBytes+=len;
			info.setTransferredBytes(transferredBytes);
		}
	}
}
