package eu.unicore.uas.fts.uftp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Logger;

import eu.unicore.client.data.UFTPConstants;
import eu.unicore.security.Client;
import eu.unicore.services.InitParameters;
import eu.unicore.uas.fts.FileTransferImpl;
import eu.unicore.uas.util.LogUtil;
import eu.unicore.uftp.dpc.Utils;
import eu.unicore.uftp.dpc.Utils.EncryptionAlgorithm;
import eu.unicore.uftp.server.requests.UFTPSessionRequest;
import eu.unicore.xnjs.io.XnjsFile;

/**
 * Server-side UFTP filetransfer resource
 *
 * @author schuller
 */
public class UFTPFileTransferImpl extends FileTransferImpl implements UFTPConstants {

	private static final Logger logger = LogUtil.getLogger(LogUtil.DATA, UFTPFileTransferImpl.class);

	static final AtomicInteger instancesCreated=new AtomicInteger();

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
		logger.debug("Creating new UFTP file transfer for client {}", getClient());
		Map<String,String>extraParameters = m.getExtraParameters();
		if(extraParameters==null){
			throw new IllegalArgumentException("Missing parameters for UFTP");	
		}
		m.clientHost = extraParameters.get(PARAM_CLIENT_HOST);
		if(m.clientHost==null){
			m.clientHost = getClient().getSecurityTokens().getClientIP();
			if(m.clientHost==null){
				throw new IllegalArgumentException("Missing parameter: "+PARAM_CLIENT_HOST);
			}
		}
		String streamsP = extraParameters.get(PARAM_STREAMS);
		m.streams = streamsP!=null?Integer.parseInt(streamsP) : 1;
		int streams = checkNumberOfRequestedStreams(m.streams);
		if(streamsP!=null && streams!=m.streams){
			throw new IllegalArgumentException("Requested number of streams exceeds server limit of <"+streams+"> !");
		}
		String secret = extraParameters.get(PARAM_SECRET);
		String keySpec = extraParameters.get(PARAM_ENABLE_ENCRYPTION);
		if(keySpec!=null && Boolean.parseBoolean(keySpec)){
			m.key = Utils.createKey(EncryptionAlgorithm.BLOWFISH);
		}
		m.compress = Boolean.parseBoolean(extraParameters.get(PARAM_ENABLE_COMPRESSION));
		m.persistent = Boolean.parseBoolean(extraParameters.get(PARAM_PERSISTENT_SESSION));
		setupUFTP(secret);
		setReady();
	}

	@Override
	public Map<String, String> getProtocolDependentParameters() {
		Map<String, String> params = super.getProtocolDependentParameters();
		UFTPFiletransferModel m = getModel();
		params.put(PARAM_SERVER_HOST, m.serverHost);
		params.put(PARAM_SERVER_PORT,String.valueOf(m.serverPort));
		params.put(PARAM_STREAMS,String.valueOf(m.streams));
		if(m.key!=null){
			params.put(PARAM_ENCRYPTION_KEY, Utils.encodeBase64(m.key));
		}
		params.put(PARAM_ENABLE_COMPRESSION,String.valueOf(m.compress));
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
	protected int checkNumberOfRequestedStreams(int streamsRequested){
		UFTPProperties cfg = kernel.getAttribute(UFTPProperties.class);
		int maxStreams = cfg.getIntValue(UFTPProperties.PARAM_STREAMS_LIMIT);
		return Math.min(maxStreams, streamsRequested);
	}

	/**
	 * check whether the target file can be read or written.
	 * Do this here to fail fast
	 */
	protected void checkAccess(boolean isExport)throws IOException{
		if(isExport)checkReadAccess();
		else checkWriteAccess();
	}
	
	protected void checkReadAccess()throws IOException{
		try(InputStream is = createNewInputStream()){
			is.read();
		}
		catch(Exception e){
			throw new IOException("Can't read source file.",e);
		}
	}
	
	protected void checkWriteAccess()throws IOException{
		try(OutputStream os = createNewOutputStream(true)){
			os.write(new byte[0]);
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
		if(secret==null || secret.equalsIgnoreCase("null")) {
			throw new IllegalArgumentException("Secret cannot be null.");
		}
		String user = "nobody";
		String group = "nobody";
		Client c = getClient();
		if(c.getXlogin()!=null){
			user = c.getXlogin().getUserName();
			group = c.getXlogin().getGroup();
			logger.debug("Initiating UFTP file transfer for user '{}' group '{}", user, group);
		}
		String reply = initUFTPJob(secret,user,group,getModel().key);
		if(reply==null || ! (reply.startsWith("OK") || reply.startsWith("200 OK") )){
			String err="UFTPD server reported an error";
			if(reply!=null){
				err+=": "+reply.trim();
			}
			throw new IOException(err);
		}
	}

	/**
	 * sends the UFTP "job" (i.e. announce that a client will connect) to UFTPD
	 */
	protected String initUFTPJob(String secret, String user, String group, byte[] key)throws IOException{
		UFTPFiletransferModel m = getModel();
		UFTPProperties cfg = kernel.getAttribute(UFTPProperties.class);
		boolean append = m.getOverWrite()==false;
		boolean compress = m.compress;
		int rateLimit = cfg.getIntValue(UFTPProperties.PARAM_RATE_LIMIT);
		InetAddress[] clientHosts = Utils.parseInetAddresses(m.clientHost, null);
		UFTPSessionRequest job = new UFTPSessionRequest(clientHosts, user, secret, m.getWorkdir()); 
		job.setGroup(group);
		job.setStreams(m.streams);
		job.setKey(key);
		job.setCompress(compress);
		job.setAppend(append);
		job.setRateLimit(rateLimit);
		job.setPersistent(m.persistent);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		job.writeEncoded(os);
		System.out.println("UFTP request: "+os.toString("UTF-8"));
		LogicalUFTPServer connector = kernel.getAttribute(LogicalUFTPServer.class);
		UFTPDInstance uftpd = connector.getUFTPDInstance();
		m.serverHost = uftpd.getHost();
		m.serverPort = uftpd.getPort();
		return uftpd.sendRequest(job);
	}

}