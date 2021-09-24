package de.fzj.unicore.uas.fts.uftp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.uas.fts.FileTransferImpl;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.xnjs.io.XnjsFile;
import eu.unicore.client.data.UFTPConstants;
import eu.unicore.security.Client;
import eu.unicore.services.InitParameters;
import eu.unicore.uftp.dpc.Utils;
import eu.unicore.uftp.server.requests.UFTPTransferRequest;
import eu.unicore.uftp.server.workers.UFTPWorker;

/**
 * server-side filetransfer ws-resource using the UFTP library
 * 
 * @author schuller
 */
public class UFTPFileTransferImpl extends FileTransferImpl implements UFTPConstants {
	
	private static final Logger logger = LogUtil.getLogger(LogUtil.DATA, UFTPFileTransferImpl.class);

	static AtomicInteger instancesCreated=new AtomicInteger();
	
	public UFTPFileTransferImpl(){
		super();
	}
	
	@Override
	public UFTPFiletransferModel getModel(){
		return (UFTPFiletransferModel)model;
	}
	
	@Override
	public void initialise(InitParameters map) throws Exception{
		if(model==null){
			model = new UFTPFiletransferModel();
		}
		UFTPFiletransferModel m = getModel();
		
		instancesCreated.incrementAndGet();
		
		super.initialise(map);
		if(logger.isDebugEnabled()){
			logger.debug("Creating new UFTP file transfer for client "+getClient());
		}
		Map<String,String>extraParameters = m.getExtraParameters();
		if(extraParameters==null){
			throw new IllegalArgumentException("Missing parameters for UFTP");	
		}
		m.clientHost=extraParameters.get(PARAM_CLIENT_HOST);
		
		boolean isExport = m.getIsExport();
		
		String sessionRequested = extraParameters.get(PARAM_USE_SESSION);
		if(sessionRequested!=null){
			m.isSession = Boolean.parseBoolean(sessionRequested);	
		}
		else{m.isSession = isExport ? m.getSource().contains(UFTPWorker.sessionModeTag) :
			m.getTarget().contains(UFTPWorker.sessionModeTag);
		}
		if(!m.isSession)checkAccess(isExport);
		
		if(m.clientHost==null){
			m.clientHost=getClient().getSecurityTokens().getClientIP();
			if(m.clientHost==null){
				throw new IllegalArgumentException("Missing parameter: "+PARAM_CLIENT_HOST);
			}
		}
		String streamsP=extraParameters.get(PARAM_STREAMS);
		m.streams=streamsP!=null?Integer.parseInt(streamsP) : 1;
		int streams=checkNumConnectionsValid(m.streams);
		if(streamsP!=null && streams!=m.streams){
			throw new IllegalArgumentException("Requested number of streams exceeds server limit of <"+streams+"> !");
		}
		String secret=extraParameters.get(PARAM_SECRET);
		String keySpec=extraParameters.get(PARAM_ENABLE_ENCRYPTION);
		if(keySpec!=null && Boolean.parseBoolean(keySpec)){
			m.key=Utils.createKey();
		}
		String compress=extraParameters.get(PARAM_ENABLE_COMPRESSION);
		if(compress!=null && Boolean.parseBoolean(compress)){
			m.compress=true;
		}
		setupUFTP(secret);
		setReady();
	}

	@Override
	public Map<String, String> getProtocolDependentParameters() {
		Map<String, String> params = super.getProtocolDependentParameters();

		UFTPFiletransferModel m = getModel();
		
		String serverHost = m.getServerHost();
		int serverPort = m.getServerPort();

		params.put(PARAM_SERVER_HOST,serverHost);
		params.put(PARAM_SERVER_PORT,String.valueOf(serverPort));
		
		int streams = m.streams;
		params.put(PARAM_STREAMS,String.valueOf(streams));
		
		byte[] key=m.key;
		if(key!=null){
			String base64=Utils.encodeBase64(key);
			params.put(PARAM_ENCRYPTION_KEY,base64);
		}
		params.put(PARAM_ENABLE_COMPRESSION,String.valueOf(m.compress));
		
		params.put(PARAM_USE_SESSION,String.valueOf(m.isSession));
		
		return params;
	}

	@Override
	public Long getTransferredBytes() {
		UFTPFiletransferModel m = getModel();
		try{
			if(!m.getIsExport()){
				XnjsFile f = getStorageAdapter().getProperties(m.getTarget());
				if(f!=null)m.setTransferredBytes(f.getSize());
			}
		}catch(Exception e){}
		return m.getTransferredBytes();
	}

	/**
	 * check and enforce server limit on numconnections
	 * @param streamsRequested number of streams
	 * @return number of streams, possibly smaller than the request
	 */
	protected int checkNumConnectionsValid(int streamsRequested){
		UFTPProperties cfg = kernel.getAttribute(UFTPProperties.class);
		int maxStreams = cfg.getIntValue(UFTPProperties.PARAM_STREAMS_LIMIT);
		return Math.min(maxStreams, streamsRequested);
	}
	
	/**
	 * check whether the target file can be read or written.
	 * Do this here to fail fast
	 * 
	 * @throws IOException
	 */
	protected void checkAccess(boolean isExport)throws IOException{
		if(isExport)checkReadAccess();
		else checkWriteAccess();
	}
	
	protected void checkReadAccess()throws IOException{
		InputStream is=null;
		try{
			is=createNewInputStream();
			is.read();
		}
		catch(Exception e){
			throw new IOException("Can't read source file.",e);
		}
		finally{
			if(is!=null)is.close();
		}
	}
	
	protected void checkWriteAccess()throws IOException{
		OutputStream os=null;
		try{
			os=createNewOutputStream(true);
			os.write(new byte[0]);
			os.close();
		}
		catch(Exception e){
			throw new IOException("Can't write to target file.",e);
		}
	}

	/**
	 * tell the uftpd server that a transfer is coming
	 * @param secret
	 * @throws Exception
	 */
	protected void setupUFTP(String secret)throws Exception{
		String user="nobody";
		String group="nobody";
		Client c=getClient();
		if(c.getXlogin()!=null){
			user=c.getXlogin().getUserName();
			group=c.getXlogin().getGroup();
			logger.debug("Initiating UFTP file transfer for user '"+user+"' group '"+group+"'");
		}
		try{
			String reply=initUFTPJob(secret,user,group,getModel().key);
			if(reply==null || ! (reply.startsWith("OK") || reply.startsWith("200 OK") )){
				String err="UFTPD server reported an error";
				if(reply!=null){
					err+=": "+reply.trim();
				}
				throw new IOException(err);
			}
		}catch(IOException ex){
			throw new Exception("Problem communicating with the UFTP server",ex);
		}
	}

	/**
	 * sends the UFTP "job" (i.e. announce that a client will connect)
	 * directly to the Java UFTPD via a socket
	 */
	protected String initUFTPJob(String secret, String user, String group, byte[] key)throws IOException{
		UFTPFiletransferModel m = getModel();
		UFTPProperties cfg = kernel.getAttribute(UFTPProperties.class);
		
		boolean isExport = m.getIsExport();
		File file;
		if(m.isSession()){
			file = new File(m.getWorkdir(),UFTPWorker.sessionModeTag);
		}
		else{
			file = isExport ? 
					new File(m.getWorkdir()+"/"+m.getSource()) :
					new File(m.getWorkdir()+"/"+m.getTarget());
		}
		boolean append= m.getOverWrite()==false;
		boolean compress = m.isCompress();
		int rateLimit = cfg.getIntValue(UFTPProperties.PARAM_RATE_LIMIT);
		InetAddress[] clientHosts = Utils.parseInetAddresses(m.clientHost, null);
		UFTPTransferRequest job = new UFTPTransferRequest(clientHosts, user, secret, file, isExport); 
		job.setGroup(group);
		job.setStreams(m.streams);
		job.setKey(key);
		job.setCompress(compress);
		job.setAppend(append);
		job.setRateLimit(rateLimit);
		LogicalUFTPServer connector = kernel.getAttribute(LogicalUFTPServer.class);
		UFTPDInstance uftpd = connector.getUFTPDInstance();
		m.setServerHost(uftpd.getHost());
		m.setServerPort(uftpd.getPort());
		return uftpd.sendRequest(job);
	}

}
