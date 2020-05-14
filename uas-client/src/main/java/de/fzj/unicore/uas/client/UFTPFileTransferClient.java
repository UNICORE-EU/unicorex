package de.fzj.unicore.uas.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.unigrids.services.atomic.types.ProtocolType;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.fts.FiletransferOptions;
import de.fzj.unicore.uas.fts.ProgressListener;
import eu.unicore.uftp.client.AbstractUFTPClient;
import eu.unicore.uftp.client.UFTPClient;
import eu.unicore.uftp.client.UFTPProgressListener;
import eu.unicore.uftp.client.UFTPSessionClient;
import eu.unicore.uftp.dpc.AuthorizationFailureException;
import eu.unicore.uftp.dpc.Utils;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * UFTP file transfer client. It connects to an UFTP file transfer instance and 
 * is used to download or upload data. Uses the session mode of UFTP, if possible.
 *
 * @author schuller
 */
public class UFTPFileTransferClient extends FileTransferClient 
implements Configurable, FiletransferOptions.IMonitorable, FiletransferOptions.SupportsPartialRead,
	UFTPConstants, UFTPProgressListener{

	private String secret;

	private InetAddress[] serverHosts;
	private final int serverPort;
	private final int streams;
	private final byte[] key;
	private final boolean compress;
	
	private ProgressListener<Long>listener;
	
	private String remoteFile;
	
	private boolean isSessionMode = true;

	public UFTPFileTransferClient(EndpointReferenceType epr,
			IClientConfiguration sec) throws Exception {
		this(epr.getAddress().getStringValue(),epr, sec);
	}

	public UFTPFileTransferClient(String url, EndpointReferenceType epr,
			IClientConfiguration sec) throws Exception {
		super(url, epr, sec);
		Map<String,String>params=getProtocolDependentRPs();
		updateServerHost(params.get(PARAM_SERVER_HOST));
		serverPort=Integer.parseInt(params.get(PARAM_SERVER_PORT));
		streams=Integer.parseInt(params.get(PARAM_STREAMS));
		String keySpec=params.get(PARAM_ENCRYPTION_KEY);
		key = keySpec!=null ? Utils.decodeBase64(keySpec) : null;
		compress=Boolean.parseBoolean(params.get(PARAM_ENABLE_COMPRESSION));
		isSessionMode = Boolean.parseBoolean(params.get(PARAM_USE_SESSION));
		if(isSessionMode){
			remoteFile = getSource()!=null? getSource() : getTarget();
		}
	}

	@Override
	public void configure(Map<String,String>params){
		String secret=params.get(UFTPConstants.PARAM_SECRET);
		if(secret!=null)setSecret(secret);
		String overrideHost = params.get(PARAM_SERVER_HOST);
		if(overrideHost!=null){
			updateServerHost(overrideHost);
		}
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
	public void readAllData(OutputStream target) throws Exception {
		if(!isSessionMode){
			readAllDataNoSession(target);
			return;
		}
		try(UFTPSessionClient c=new UFTPSessionClient(serverHosts, serverPort)){
			configureClient(c);
			c.connect();
			c.get("./"+remoteFile, target);
		}
	}
	
	private void readAllDataNoSession(OutputStream target) throws Exception {
		try(UFTPClient c=new UFTPClient(serverHosts,serverPort,target)){
			configureClient(c);
			c.run();
		}
	}

	@Override
	public long readPartial(long offset, long length, OutputStream target) throws IOException {
		if(!isSessionMode)throw new IOException("Partial read not (yet) implemented");
		try(UFTPSessionClient c=new UFTPSessionClient(serverHosts, serverPort)){
			configureClient(c);
			c.connect();
			return c.get("./"+remoteFile, offset, length, target);
		}
		catch(AuthorizationFailureException e){
			throw new IOException(e);
		}
	}

	@Override
	public void writeAllData(InputStream source) throws Exception {	
		if(!isSessionMode){
			writeAllDataNoSession(source);
			return;
		}
		try(UFTPSessionClient c=new UFTPSessionClient(serverHosts,serverPort)){
			configureClient(c);
			c.connect();
			if(append){
				c.append("./"+remoteFile, -1, source);
			}
			else{
				c.put("./"+remoteFile, -1, source);
			}
		}
	}

	private void writeAllDataNoSession(InputStream source) throws Exception {	
		try(UFTPClient c=new UFTPClient(serverHosts,serverPort,source)){
			configureClient(c);
			c.run();
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

	/**
	 * convenience method to create a UFTP file import
	 * 
	 * @param path - the file name
	 * @param sms -  the storage
	 * @param append - whether to append data
	 * @param clientHost - the client host (name or IP)
	 * @param numConnections - the requested number of connctions
	 * @param secret - the authz token that the server should use to authorize this transfer
	 * @param encrypt - if <code>true</code>, data will be encrypted
	 * @param compress - if <code>true</code>, data will be compressed
	 * @return UFTP client
	 */
	public static UFTPFileTransferClient createImport(String path, StorageClient sms, boolean append,
			String clientHost, int numConnections, String secret, boolean encrypt, boolean compress)throws Exception{
		UFTPFileTransferClient c=(UFTPFileTransferClient)sms.getImport(path, append, 
				makeParams(clientHost, numConnections, secret, encrypt,compress), ProtocolType.UFTP);
		c.secret=secret;
		return c;
	}

	/**
	 * convenience method to create a UFTP file export
	 * @param path - the file name
	 * @param sms -  the storage
	 * @param clientHost - the client host (name or IP)
	 * @param numConnections - the requested number of connctions
	 * @param secret - the authz token that the server should use to authorize this transfer
	 * @param encrypt - if <code>true</code>, data will be encrypted
	 * @param compress - if <code>true</code>, data will be compressed
	 * @return UFTP client
	 */
	public static UFTPFileTransferClient createExport(String path, StorageClient sms, 
			String clientHost, int numConnections, String secret, boolean encrypt,boolean compress)throws Exception{
		UFTPFileTransferClient c=(UFTPFileTransferClient)sms.getExport(path, 
				makeParams(clientHost, numConnections, secret, encrypt, compress), ProtocolType.UFTP);
		c.secret=secret;
		return c;
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
	
	private long lastTotal=0;
	
	@Override
	public void notifyTotalBytesTransferred(long totalBytesTransferred) {
		if(listener!=null){
			//need to calculate difference to last call
			listener.notifyProgress(totalBytesTransferred-lastTotal);
			lastTotal=totalBytesTransferred;
		}
	}

	private static Map<String,String>makeParams(String host, int numConnections, String secret, boolean encrypt,boolean compress){
		Map<String,String>p=new HashMap<String, String>();
		p.put(PARAM_CLIENT_HOST,host);
		p.put(PARAM_STREAMS,String.valueOf(numConnections));
		p.put(PARAM_SECRET,secret);
		p.put(PARAM_ENABLE_ENCRYPTION, String.valueOf(encrypt));
		p.put(PARAM_ENABLE_COMPRESSION, String.valueOf(compress));
		return p;
	}

	public InetAddress[]asHosts(String hostsProperty){
		List<InetAddress>hostList=new ArrayList<InetAddress>();
		String[] hosts=hostsProperty.split("[ ,]+");
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
		StringBuilder sb=new StringBuilder();
		for(InetAddress ip: ips){
			if(sb.length()>0)sb.append(',');
			sb.append(ip.getHostName());
		}
		return sb.toString();
	}
}
