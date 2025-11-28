package eu.unicore.uas.fts.uftp;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import eu.unicore.client.core.FileList.FileListEntry;
import eu.unicore.client.data.UFTPConstants;
import eu.unicore.client.data.UFTPFileTransferClient;
import eu.unicore.uas.xnjs.RESTFileImportBase;
import eu.unicore.uftp.client.UFTPSessionClient;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.processors.AsyncCommandProcessor.SubCommand;
import eu.unicore.xnjs.tsi.remote.TSIConnectionFactory;
import eu.unicore.xnjs.util.AsyncCommandHelper;
import eu.unicore.xnjs.util.ResultHolder;
import eu.unicore.xnjs.util.UFTPUtils;

/**
 * @author schuller
 */
public class RESTUFTPImport extends RESTFileImportBase implements UFTPConstants {

	private String secret;

	/** whether UNICORE/X should act as UFTP client, which requires sending the
	 * data to the TSI. If set to <code>false</code>, the UFTP client process
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

	public RESTUFTPImport(XNJS xnjs){
		super(xnjs);
		uftpProperties = kernel.getAttribute(UFTPProperties.class);
		if(uftpProperties==null)throw new IllegalArgumentException("UFTP is not enabled.");
		localMode = uftpProperties.getBooleanValue(UFTPProperties.PARAM_CLIENT_LOCAL);
		info.setProtocol("UFTP");
		clientHost = setupClientHost();
		String ip = uftpProperties.getValue(UFTPProperties.PARAM_CLIENT_IP);
		clientIPAddresses = ip!=null? ip : clientHost;
	}

	@Override
	public void setExtraParameters(Map<String,String>params){
		for(Map.Entry<String,String> e: params.entrySet()){
			String key = e.getKey();
			if(UFTPConstants.PARAM_ENABLE_ENCRYPTION.equals(key)){
				useEncryption = Boolean.parseBoolean(e.getValue());
			}
			else if(UFTPConstants.PARAM_STREAMS.equals(key)){
				streams = Integer.parseInt(e.getValue());
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

	/**
	 * in session mode, there are two ways to transfer a file
	 * if local mode, the file is transferred immediately using the session client, 
	 * else, the remote file name and local target are written to the command file
	 */
	@Override
	protected void transferFileFromRemote(FileListEntry currentSource, String currentTarget) throws Exception {
		String fileName=currentSource.path;
		while(fileName.startsWith("/"))fileName=fileName.substring(1);
		while(currentTarget.startsWith("/"))currentTarget=currentTarget.substring(1);

		if(localMode){
			try(OutputStream os=getStorageAdapter().getOutputStream(currentTarget)){
				sessionClient.get(fileName, os);
			}
		}
		else {
			runTSIClient(fileName, currentTarget);
		}
	}

	protected void setupSessionMode()throws Exception{
		Map<String,String>ep = getExtraParameters();
		ftc = storage.createExport(SESSION_TAG, "UFTP", ep);
		if(localMode) {
			UFTPFileTransferClient uftc=(UFTPFileTransferClient)ftc;
			sessionClient=new UFTPSessionClient(uftc.getServerHosts(), uftc.getServerPort());
			sessionClient.setSecret(secret);
			sessionClient.setNumConnections(streams);
			sessionClient.connect();
		}
	}

	protected void finishSessionMode() throws Exception{
		if(localMode){
			sessionClient.close();
		}
	}

	protected Map<String,String>getExtraParameters(){
		Map<String,String>result = new HashMap<>();
		result.put(PARAM_CLIENT_HOST, clientIPAddresses);
		result.put(PARAM_STREAMS,String.valueOf(streams));
		secret = UUID.randomUUID().toString();
		result.put(PARAM_SECRET, secret);
		result.put(PARAM_ENABLE_ENCRYPTION,String.valueOf(useEncryption));
		return result;
	}

	private String setupClientHost(){
		String clientHost = uftpProperties.getValue(UFTPProperties.PARAM_CLIENT_HOST);
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

	private AsyncCommandHelper ach;

	private void runAsync(String cmd, int cmdtype) throws Exception {
		ach = new AsyncCommandHelper(configuration, cmd, info.getUniqueId(), info.getParentActionID(), client);
		ach.setPreferredExecutionHost(clientHost);
		ach.getSubCommand().type = cmdtype;
		ach.submit();
		do{
			try{
				checkCancelled();
			}catch(CancelledException ce){
				try{
					ach.abort();
				}catch(Exception e){}
				throw ce;
			}
			Thread.sleep(2000);
		}while(!ach.isDone());

		ResultHolder res = ach.getResult();
		if(res.getExitCode()==null || res.getExitCode()!=0){
			String message="UFTP data download failed.";
			try{
				String error = res.getErrorMessage();
				if(error!=null && error.length()>0)message+=" Error details: "+error;
			}catch(IOException ex){}
			throw new Exception(message);
		}
	}

	private void runTSIClient(String from, String to) throws Exception {
		UFTPFileTransferClient uftc=(UFTPFileTransferClient)ftc;
		String cmd = UFTPUtils.jsonBuilder()
				.get().from(from).to(to).workdir(workdir)
				.secret(secret)
				.host(uftc.getServerHosts()[0].getHostAddress()).port(uftc.getServerPort())
				.build().toString();
		runAsync(cmd, SubCommand.UFTP);
	}

	@Override
	protected void createNewExport(FileListEntry source)throws Exception{
		throw new IllegalStateException();
	}

}
