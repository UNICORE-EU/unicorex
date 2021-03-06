package de.fzj.unicore.uas.fts.uftp;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.io.IOUtils;

import de.fzj.unicore.uas.client.UFTPConstants;
import de.fzj.unicore.uas.xnjs.RESTFileImportBase;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.tsi.remote.TSIConnectionFactory;
import de.fzj.unicore.xnjs.util.AsyncCommandHelper;
import de.fzj.unicore.xnjs.util.ResultHolder;
import eu.unicore.client.core.FileList.FileListEntry;
import eu.unicore.client.data.UFTPFileTransferClient;
import eu.unicore.uftp.client.UFTPSessionClient;
import eu.unicore.uftp.server.workers.UFTPWorker;
import eu.unicore.util.Log;

/**
 * @author schuller
 */
public class RESTUFTPImport extends RESTFileImportBase implements UFTPConstants{

	private String secret;

	/** whether the Java UFTP library should be used directly, which requires that
	 * the UNICORE/X has access to the file system<br/>
	 * If set to <code>false</code>, the TSI is used to run the UFTP client   
	 */
	private final boolean localMode;

	// host address of the host where the UFTP client code runs
	// this is either (in local mode) the UNICORE/X host, or one of the TSI
	// nodes
	private final String clientHost;
	
	// client host address as sent to the UFTPD server
	// in general this will be the same as "clientHost", but
	// in some cases it can be required that they differ
	private final String clientIPAddresses;

	// can be set by client
	private boolean useEncryption = false;
	private int streams = 1;
	
	private final UFTPProperties uftpProperties;

	private UFTPSessionClient sessionClient;

	public RESTUFTPImport(XNJS config){
		super(config);
		uftpProperties = kernel.getAttribute(UFTPProperties.class);
		localMode = uftpProperties.getBooleanValue(UFTPProperties.PARAM_CLIENT_LOCAL);
		info.setProtocol("UFTP");
		clientHost = setupClientHost();
		String ip = uftpProperties.getValue(UFTPProperties.PARAM_CLIENT_IP);
		clientIPAddresses = ip!=null? ip : clientHost;
	}

	@Override
	public void setExtraParameters(Map<String,String>params){
		for(Map.Entry<String,String> e: params.entrySet()){
			String key=e.getKey();
			String value=e.getValue();
			if(UFTPConstants.PARAM_ENABLE_ENCRYPTION.equals(key)){
				useEncryption = Boolean.parseBoolean(value);
			}
			if(UFTPConstants.PARAM_STREAMS.equals(key)){
				streams = Integer.parseInt(value);
			}
		}
	}

	/**
	 * optimized implementation using session mode
	 */
	@Override
	protected void runTransfers() throws Exception {
		setupSessionMode();
		super.runTransfers();
		finishSessionMode();
		info.setTransferredBytes(info.getDataSize());
	}

	private StringBuilder fileList=new StringBuilder();

	/**
	 * in session mode, there are two ways to transfer a file
	 * if local mode, the file is transferred immediately using the session client, 
	 * else, the remote file name and local target are written to the command file
	 */
	@Override
	protected void transferFileFromRemote(FileListEntry currentSource, String currentTarget) throws Exception {
		String fileName=currentSource.path;
		while(fileName.startsWith("/"))fileName=fileName.substring(1);

		if(localMode){
			OutputStream os=getStorageAdapter().getOutputStream(currentTarget);
			try{
				sessionClient.get(fileName, os);
			}
			finally{
				os.close();
			}
		}
		else{
			fileList.append("GET ").
					append("\"").append(fileName).append("\" ").
					append("\"").append(currentTarget).append("\"").append("\n");
		}
	}

	protected void setupSessionMode()throws Exception{
		Map<String,String>ep=getExtraParameters();
		ftc = storage.createExport(UFTPWorker.sessionModeTag, "UFTP", ep);
		if(localMode) {
			UFTPFileTransferClient uftc=(UFTPFileTransferClient)ftc;
			sessionClient=new UFTPSessionClient(uftc.getServerHosts(), uftc.getServerPort());
			sessionClient.setSecret(secret);
			sessionClient.setNumConnections(getNumberOfStreams());
			sessionClient.connect();
		}
	}

	protected void finishSessionMode() throws Exception{
		if(localMode){
			sessionClient.close();
		}
		else{
			String root = getStorageAdapter().getStorageRoot();
			String cmdFile=".uftp-"+info.getUniqueId();
			OutputStream os=getStorageAdapter().getOutputStream(cmdFile);
			try{
				IOUtils.write(fileList.toString(), os, "UTF-8");
			}finally{
				os.close();
			}
			runAsync(root+"/"+cmdFile);
		}
	}

	protected Map<String,String>getExtraParameters(){
		Map<String,String>result=new HashMap<String, String>();
		result.put(PARAM_CLIENT_HOST, clientIPAddresses);
		result.put(PARAM_STREAMS,String.valueOf(getNumberOfStreams()));
		secret=UUID.randomUUID().toString();
		result.put(PARAM_SECRET, secret);
		result.put(PARAM_ENABLE_ENCRYPTION,String.valueOf(useEncryption));
		return result;
	}

	private String setupClientHost(){
		String clientHost=uftpProperties.getValue(UFTPProperties.PARAM_CLIENT_HOST);
		if(clientHost==null){
			if(localMode){
				clientHost = getLocalHost();
			}
			else{
				// select one of the configured TSI nodes
				TSIConnectionFactory tcf = configuration.get(TSIConnectionFactory.class);
				if(tcf == null){
					clientHost = getLocalHost();
				}
				else{
					String[] tsis = tcf.getTSIHosts();
					clientHost = tsis[new Random().nextInt(tsis.length)];
				}
			}
		}
		return clientHost;
	}

	private String getLocalHost(){
		try{
			return InetAddress.getLocalHost().getCanonicalHostName();
		}catch(Exception ex){
			 return "localhost";
		}
	}

	protected int getNumberOfStreams(){
		return streams;
	}

	private AsyncCommandHelper ach;

	protected void runAsync(String commandFile)throws Exception{
		String cmd=getCommandLine(commandFile);
		logger.info("Executing "+cmd);
		ach=new AsyncCommandHelper(configuration, cmd, info.getUniqueId(), info.getParentActionID(), client);
		ach.setPreferredExecutionHost(clientHost);
		ach.submit();
		int interval = 2000;
		do{
			try{
				checkCancelled();
			}catch(CancelledException ce){
				try{
					ach.abort();
				}catch(Exception e){
					logger.warn("Could not abort UFTP client run.");
				}
				throw ce;
			}
			Thread.sleep(interval);
		}while(!ach.isDone());

		ResultHolder res=ach.getResult();
		if(res.getExitCode()==null || res.getExitCode()!=0){
			String message="UFTP data download failed.";
			try{
				String error=res.getStdErr();
				if(error!=null)message+=" Error details: "+error;
			}catch(IOException ex){
				Log.logException("Could not read UFTP stderr", ex, logger);
			}
			throw new Exception(message);
		}
		info.setTransferredBytes(info.getDataSize());
	}

	private String getCommandLine(String commandFile)throws Exception{
		String uftp = uftpProperties.getValue(UFTPProperties.PARAM_CLIENT_EXECUTABLE);
		UFTPFileTransferClient client = (UFTPFileTransferClient)ftc;
		String host=client.asString(client.getServerHosts());
		int port=client.getServerPort();
		int streams=client.getStreams();
		String key=client.getEncryptionKey();
		int buf = uftpProperties.getIntValue(UFTPProperties.PARAM_BUFFERSIZE);
		boolean compress = client.isCompressionEnabled();
		return uftp+" -S "+UFTPSessionClient.makeCommandline(host, port, workdir, secret, streams, key, buf, compress, commandFile);
	}
	
	@Override
	protected void createNewExport(FileListEntry source)throws Exception{
		throw new IllegalStateException();
	}

}
