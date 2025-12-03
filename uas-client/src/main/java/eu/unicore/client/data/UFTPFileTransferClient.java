package eu.unicore.client.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import eu.unicore.client.Endpoint;
import eu.unicore.client.utils.Configurable;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.uas.fts.FiletransferOptions;
import eu.unicore.uas.fts.ProgressListener;
import eu.unicore.uas.json.JSONUtil;
import eu.unicore.uftp.client.AbstractUFTPClient;
import eu.unicore.uftp.client.UFTPProgressListener;
import eu.unicore.uftp.client.UFTPSessionClient;
import eu.unicore.uftp.dpc.AuthorizationFailureException;
import eu.unicore.uftp.dpc.Utils;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * UFTP file transfer client. It connects to an UFTP file transfer instance and 
 * is used to download or upload data. Uses the session mode of UFTP, if possible.
 *
 * @author schuller
 */
public class UFTPFileTransferClient extends FiletransferClient 
 implements Configurable, FiletransferOptions.IMonitorable, FiletransferOptions.IAppendable,
 			FiletransferOptions.Read, FiletransferOptions.SupportsPartialRead,  
 			FiletransferOptions.Write,
			UFTPConstants, UFTPProgressListener 
{

	private static final Logger logger = Log.getLogger(Log.CLIENT, UFTPFileTransferClient.class);

	private String secret;
	private InetAddress[] serverHosts;
	private final int serverPort;
	private final int streams;
	private final byte[] key;
	private final boolean compress;

	private ProgressListener<Long>listener;

	private String remoteFile;

	private boolean append;

	public UFTPFileTransferClient(Endpoint endpoint, JSONObject initialProperties, IClientConfiguration security, IAuthCallback auth) throws Exception {
		super(endpoint, initialProperties, security, auth);
		Map<String,String>params = JSONUtil.asMap(initialProperties);
		updateServerHost(params.get(PARAM_SERVER_HOST));
		serverPort = Integer.parseInt(params.get(PARAM_SERVER_PORT));
		streams = Integer.parseInt(params.get(PARAM_STREAMS));
		String keySpec = params.get(PARAM_ENCRYPTION_KEY);
		key = keySpec!=null ? Utils.decodeBase64(keySpec) : null;
		compress = Boolean.parseBoolean(params.get(PARAM_ENABLE_COMPRESSION));
		remoteFile = params.get("fileName");
	}

	@Override
	public void configure(Map<String,String>params){
		String secret = params.get(UFTPConstants.PARAM_SECRET);
		if(secret!=null)setSecret(secret);
		String overrideHost = params.get(PARAM_SERVER_HOST);
		if(overrideHost!=null){
			updateServerHost(overrideHost);
		}
	}

	@Override
	public Long getTransferredBytes() {
		return lastTotal;
	}

	protected void updateServerHost(String hostSpec){
		serverHosts = asHosts(hostSpec);
		if(serverHosts.length==0){
			throw new ConfigurationException("No usable UFTP server host could be determined from <"+hostSpec+">");
		}
	}

	public void setSecret(String secret){
		this.secret=secret;
	}

	@Override
	public void readFully(OutputStream target) throws Exception {
		try(UFTPSessionClient c = new UFTPSessionClient(serverHosts, serverPort)){
			configureClient(c);
			c.connect();
			lastTotal += c.get("./"+remoteFile, target);
		}
	}

	@Override
	public long read(long offset, long length, OutputStream target) throws IOException {
		try(UFTPSessionClient c = new UFTPSessionClient(serverHosts, serverPort)){
			configureClient(c);
			c.connect();
			long n = c.get("./"+remoteFile, offset, length, target);
			lastTotal += n;
			return n;
		}
		catch(AuthorizationFailureException e){
			throw new IOException(e);
		}
	}

	@Override
	public void write(InputStream source) throws Exception {	
		write(source, -1);
	}

	@Override
	public void write(InputStream source, long numBytes) throws Exception {	
		try(UFTPSessionClient c = new UFTPSessionClient(serverHosts,serverPort)){
			configureClient(c);
			c.connect();
			if(append){
				lastTotal += c.append("./"+remoteFile, numBytes, source);
			}
			else{
				lastTotal += c.put("./"+remoteFile, numBytes, source);
			}
		}
	}

	private void configureClient(AbstractUFTPClient c){
		c.setNumConnections(streams);
		c.setSecret(secret);
		c.setCompress(compress);
		c.setKey(key);
		if(listener!=null)c.setProgressListener(this);
	}

	@Override
	public void setProgressListener(ProgressListener<Long> listener) {
		this.listener=listener;
	}

	public InetAddress[] getServerHosts() {
		return serverHosts;
	}

	public int getServerPort() {
		return serverPort;
	}

	public int getStreams() {
		return streams;
	}

	public boolean isCompressionEnabled() {
		return compress;
	}

	/**
	 * the base64 encoded encryption key, or <code>null</code> if no encryption
	 */
	public String getEncryptionKey() {
		return key!=null? Utils.encodeBase64(key) : null;
	}

	@Override
	public void setAppend() {
		append = true;
	}

	private long lastTotal = 0;

	@Override
	public void notifyTotalBytesTransferred(long totalBytesTransferred) {
		if(listener!=null){
			//need to calculate difference to last call
			listener.notifyProgress(totalBytesTransferred-lastTotal);
			lastTotal=totalBytesTransferred;
		}
	}

	public InetAddress[]asHosts(String hostsProperty){
		List<InetAddress>hostList = new ArrayList<>();
		String[] hosts = hostsProperty.split("[ ,]+");
		for(String h: hosts){
			try{
				hostList.add(InetAddress.getByName(h));
			}catch(IOException io){
				logger.trace("Un-usable UFTP host address <"+h+"> : "+io);
			}
		}
		return hostList.toArray(new InetAddress[hostList.size()]);
	}

	public String asString(InetAddress[] ips){
		StringBuilder sb = new StringBuilder();
		for(InetAddress ip: ips){
			if(sb.length()>0)sb.append(',');
			sb.append(ip.getHostName());
		}
		return sb.toString();
	}
}