package de.fzj.unicore.uas.fts.uftp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.io.IOUtils;

import de.fzj.unicore.uas.util.Pair;
import de.fzj.unicore.uas.xnjs.RESTFileExportBase;
import eu.unicore.client.data.UFTPConstants;
import eu.unicore.client.data.UFTPFileTransferClient;
import eu.unicore.uftp.client.UFTPSessionClient;
import eu.unicore.util.Log;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.processors.AsyncCommandProcessor.SubCommand;
import eu.unicore.xnjs.tsi.remote.TSIConnectionFactory;
import eu.unicore.xnjs.util.AsyncCommandHelper;
import eu.unicore.xnjs.util.ResultHolder;
import eu.unicore.xnjs.util.UFTPUtils;

/**
 * @author schuller
 */
public class RESTUFTPExport extends RESTFileExportBase implements UFTPConstants {

	private String secret;

	/** whether UNICORE/X should act as UFTP client, which requires getting the
	 * data from the TSI. If set to <code>false</code>, the UFTP client process
	 * will be launched on the TSI (with much better performance)
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

	private boolean haveJavaClient;

	public RESTUFTPExport(XNJS config){
		super(config);
		uftpProperties = kernel.getAttribute(UFTPProperties.class);
		localMode = uftpProperties.getBooleanValue(UFTPProperties.PARAM_CLIENT_LOCAL);
		haveJavaClient = uftpProperties.getValue(UFTPProperties.PARAM_CLIENT_EXECUTABLE)!=null;
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
	protected void transferFileFromRemote(Pair<String,Long> sourceDesc, String currentTarget) throws Exception {
		String currentSource = sourceDesc.getM1();

		// chop off leading "/" since UFTPD will otherwise treat it as an
		// absolute filename
		while (currentTarget.startsWith("/"))currentTarget = currentTarget.substring(1);
		while (currentSource.startsWith("/"))currentSource = currentSource.substring(1);

		if (localMode) {
			long size = sourceDesc.getM2();
			try(InputStream is = getStorageAdapter().getInputStream(currentSource)){
				sessionClient.put(currentTarget, size, is);
			}
		} else if(haveJavaClient){
			fileList.append("PUT ").append("\"").append(currentSource).append("\" ").append("\"").append(currentTarget)
			.append("\"").append("\n");
		}
		else {
			runTSIClient(currentSource, currentTarget);
		}

	}

	protected void setupSessionMode()throws Exception{
		Map<String,String>ep=getExtraParameters();
		ftc = storage.createExport(SESSION_TAG, "UFTP", ep);
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
		else if(haveJavaClient){
			String root = getStorageAdapter().getStorageRoot();
			String cmdFile=".uftp-"+info.getUniqueId();
			OutputStream os=getStorageAdapter().getOutputStream(cmdFile);
			try{
				IOUtils.write(fileList.toString(), os, "UTF-8");
			}finally{
				os.close();
			}
			runJavaClientOnTSI(root+"/"+cmdFile);
		}
	}

	protected Map<String,String>getExtraParameters(){
		Map<String,String>result = new HashMap<>();
		result.put(PARAM_CLIENT_HOST, clientIPAddresses);
		result.put(PARAM_STREAMS,String.valueOf(getNumberOfStreams()));
		secret=UUID.randomUUID().toString();
		result.put(PARAM_SECRET, secret);
		result.put(PARAM_ENABLE_ENCRYPTION,String.valueOf(useEncryption));
		return result;
	}

	@Override
	protected void doRun(String localFile) throws Exception {
		if(localMode){
			super.doRun(localFile);
		}
		else{
			runJavaClientOnTSI(localFile);
		}
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
					Collection<String> tsis = tcf.getTSIHosts();
					int r = new Random().nextInt(tsis.size());
					for(String h: tsis) {
						if(r==0) {
							clientHost = h;
							break;
						}
						r = r-1;
					}
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


	private void runAsync(String cmd, int cmdtype) throws Exception {
		ach=new AsyncCommandHelper(configuration, cmd, info.getUniqueId(), info.getParentActionID(), client);
		ach.setPreferredExecutionHost(clientHost);
		ach.getSubCommand().type = cmdtype;
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
				String error = res.getErrorMessage();
				if(error!=null && error.length()>0)message+=" Error details: "+error;
			}catch(IOException ex){
				Log.logException("Could not read UFTP error message", ex, logger);
			}
			throw new Exception(message);
		}
	}

	private void runTSIClient(String from, String to) throws Exception {
		UFTPFileTransferClient uftc=(UFTPFileTransferClient)ftc;
		String cmd = UFTPUtils.jsonBuilder()
				.put().from(from).to(to).workdir(workdir)
				.secret(secret)
				.host(uftc.getServerHosts()[0].getHostAddress()).port(uftc.getServerPort())
				.build().toString();
		runAsync(cmd, SubCommand.UFTP);
	}


	private void runJavaClientOnTSI(String commandFile)throws Exception{
		String cmd=getJavaClientCommandLine(commandFile);
		logger.info("Executing "+cmd);
		runAsync(cmd, SubCommand.NORMAL);
		info.setTransferredBytes(info.getDataSize());
	}

	@Override
	protected void copyPermissions(String source, String target) {
		if(localMode) {
			super.copyPermissions(source, target);
		}
	};

	private String getJavaClientCommandLine(String localFile)throws Exception{
		String uftp = uftpProperties.getValue(UFTPProperties.PARAM_CLIENT_EXECUTABLE);
		UFTPFileTransferClient client = (UFTPFileTransferClient)ftc;
		String host=client.asString(client.getServerHosts());
		int port=client.getServerPort();
		int streams=client.getStreams();
		String key=client.getEncryptionKey();
		int buf = uftpProperties.getIntValue(UFTPProperties.PARAM_BUFFERSIZE);
		boolean compress = client.isCompressionEnabled();
		return uftp+" -S "+UFTPSessionClient.makeCommandline(host, port, workdir, secret, streams, key, buf, compress, localFile);
	}

}
