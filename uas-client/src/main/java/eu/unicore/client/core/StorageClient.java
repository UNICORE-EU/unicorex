package eu.unicore.client.core;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

import org.apache.http.HttpResponse;
import org.json.JSONObject;

import de.fzj.unicore.uas.CoreClientCapabilities.FTClientCapability;
import de.fzj.unicore.uas.CoreClientCapabilities.RESTFTClientCapability;
import de.fzj.unicore.uas.client.Configurable;
import de.fzj.unicore.uas.client.FileTransferClient;
import de.fzj.unicore.uas.fts.FiletransferOptions;
import de.fzj.unicore.uas.json.JSONUtil;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.FileList.FileListEntry;
import eu.unicore.client.data.FileClient;
import eu.unicore.client.data.FiletransferClient;
import eu.unicore.client.data.HttpFileTransferClient;
import eu.unicore.client.data.TransferControllerClient;
import eu.unicore.services.ClientCapabilities;
import eu.unicore.services.ClientCapability;
import eu.unicore.services.rest.client.BaseClient;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * storage access
 * 
 * @author schuller
 */
public class StorageClient extends BaseServiceClient {

	protected final static Map<String, Class<? extends FiletransferClient>> registeredClients = new HashMap<>();

	public StorageClient(Endpoint endpoint, IClientConfiguration security, IAuthCallback auth) {
		super(endpoint, security, auth);
		initRegisteredClients();
	}

	public FileList getFiles(String basedir) throws Exception {
		if(basedir==null)basedir = "/";
		if(!basedir.startsWith("/"))basedir="/"+basedir;
		String url = getLinkUrl("files")+encode(basedir);
		Endpoint ep = endpoint.cloneTo(url);
		return new FileList(this, ep, security, auth);
	}
	
	/**
	 * get file properties for a path 
	 * @param path
	 * @return
	 * @throws Exception
	 */
	public FileListEntry stat(String path) throws Exception {
		if(path==null)path = "/";
		if(!path.startsWith("/"))path="/"+path;
		String url = getLinkUrl("files")+encode(path);
		Endpoint ep = endpoint.cloneTo(url);
		JSONObject props = new BaseServiceClient(ep, security, auth).getProperties();
		return new FileListEntry(path, props);
	}

	public FileClient getFileClient(String path) throws Exception {
		if(path==null)path = "/";
		if(!path.startsWith("/"))path="/"+path;
		String url = getLinkUrl("files")+encode(path);
		Endpoint ep = endpoint.cloneTo(url);
		return new FileClient(ep, security, auth);
	}
	

	public void mkdir(String path) throws Exception {
		getFileClient(path).mkdir();
	}
	
	public void chmod(String path, String unixPermissions) throws Exception {
		getFileClient(path).chmod(unixPermissions);
	}
	
	public boolean supportsMetadata() throws Exception {
		return getProperties().getBoolean("metadataSupported");
	}
	
	public String getMountPoint() throws Exception {
		return getProperties().getString("mountPoint");
	}
	
	public String getFileSystemDescription() throws Exception {
		return getProperties().optString("filesystemDescription", null);
	}
	
	public FiletransferClient createImport(String filename,  boolean append, long numBytes, String protocol, Map<String,String>extraParameters) throws Exception {
		
		Class<? extends FiletransferClient> clazz = getFiletransferClientClass(protocol);
		
		try {
			JSONObject json = new JSONObject();
			json.put("file", filename);
			json.put("protocol",protocol);
			json.put("overwrite", !append);
			json.put("extraParameters", JSONUtil.asJSON(extraParameters));
			if(numBytes>-1)json.put("numBytes", BigInteger.valueOf(numBytes));
			
			BaseClient c = createTransport(endpoint.getUrl()+"/imports", security, auth);
			HttpResponse res = c.post(json);
			c.checkError(res);
			JSONObject response = c.asJSON(res);
			
			Endpoint ep = new Endpoint(res.getFirstHeader("Location").getValue());
			FiletransferClient fts = clazz.getConstructor(
					new Class[] { Endpoint.class, JSONObject.class, IClientConfiguration.class, IAuthCallback.class }
					).newInstance(
									new Object[] { ep, response, security, auth});
			
			if(append) {
				if(fts instanceof FiletransferOptions.IAppendable) {
					((FiletransferOptions.IAppendable)fts).setAppend();
				}
				else throw new Exception("Append requested, but not supported by client implementation <"
						+fts.getClass()+"> for protocol <"+protocol+">");
			}
			if(fts instanceof Configurable){
				((Configurable)fts).configure(extraParameters);
			}
			return fts;
		} catch (Exception e) {
			String msg=Log.createFaultMessage("Can't create import.", e);
			throw new IOException(msg,e);
		}
	}
	
	/**
	 * create BFT upload
	 */
	public HttpFileTransferClient upload(String filename) throws Exception {
		return (HttpFileTransferClient)createImport(filename, false, -1, "BFT", null);
	}
	
	public FiletransferClient createExport(String filename,  String protocol, Map<String,String>extraParameters) throws Exception {
		
		Class<? extends FiletransferClient> clazz = getFiletransferClientClass(protocol);
		
		try {
			JSONObject json = new JSONObject();
			json.put("file", filename);
			json.put("protocol",protocol);
			json.put("extraParameters", JSONUtil.asJSON(extraParameters));
			
			BaseClient c = createTransport(endpoint.getUrl()+"/exports", security, auth);
			HttpResponse res = c.post(json);
			c.checkError(res);
			JSONObject response = c.asJSON(res);
			
			Endpoint ep = new Endpoint(res.getFirstHeader("Location").getValue());
			FiletransferClient fts = clazz.getConstructor(
					new Class[] { Endpoint.class, JSONObject.class, IClientConfiguration.class, IAuthCallback.class }
					).newInstance(
									new Object[] { ep, response, security, auth});
			
			if(fts instanceof Configurable){
				((Configurable)fts).configure(extraParameters);
			}
			return fts;
		} catch (Exception e) {
			String msg=Log.createFaultMessage("Can't create import.", e);
			throw new IOException(msg,e);
		}
	}
	
	/**
	 * create BFT download
	 */
	public HttpFileTransferClient download(String filename) throws Exception {
		return (HttpFileTransferClient) createExport(filename, "BFT", null);
	}
	
	/**
	 * initiate a server-server transfer, which will fetch a remote file to this storage
	 * 
	 * @param sourceURL
	 * @param fileName
	 * @param protocol
	 */
	public TransferControllerClient fetchFile(String sourceURL, String fileName, String protocol) throws Exception {
		JSONObject json = new JSONObject();
		json.put("source", sourceURL);
		json.put("file", fileName);
		json.put("protocol",protocol);
		BaseClient c = createTransport(endpoint.getUrl()+"/transfers", security, auth);
		HttpResponse res = c.post(json);
		c.checkError(res);
		Endpoint ep = new Endpoint(res.getFirstHeader("Location").getValue());
		return new TransferControllerClient(ep, security, auth);
	}
	
	/**
	 * initiate a server-server transfer, which will send a file from this storage to a remote location
	 * 
	 * @param fileName
	 * @param targetURL
	 * @param protocol
	 */
	public TransferControllerClient sendFile(String fileName, String targetURL, String protocol) throws Exception {
		JSONObject json = new JSONObject();
		json.put("file", fileName);
		json.put("target", targetURL);
		json.put("protocol",protocol);
		BaseClient c = createTransport(endpoint.getUrl()+"/transfers", security, auth);
		HttpResponse res = c.post(json);
		c.checkError(res);
		Endpoint ep = new Endpoint(res.getFirstHeader("Location").getValue());
		return new TransferControllerClient(ep, security, auth);
	}
	
	@SuppressWarnings("unchecked")
	protected Class<? extends FiletransferClient> getFiletransferClientClass(String protocol) throws IOException {
		Class<? extends FiletransferClient> clazz = null;
		protocol = protocol.toUpperCase();
		String className = System.getProperty(String.valueOf(protocol)+".clientClass");
		if(className != null){
			try{
				clazz = (Class<? extends FiletransferClient>)(Class.forName(className));
			}catch(Exception ex){
				throw new IOException("Custom client class <"+className+"> for protocol <"+protocol+"> cannot be instantiated!", ex);
			}
		}
		else{
			clazz = registeredClients.get(protocol);
		}
		if (clazz == null){
			throw new IOException("No matching client class supporting the <"
					+ protocol + "> protocol found.");
		}
		return clazz;
	}
	
	/**
	 * register a client class supporting the given protocol. Note that clients are usually registered 
	 * using the service loader mechanism 
	 * @see FTClientCapability
	 * @param proto - the protocol
	 * @param clazz - the {@link FileTransferClient} class
	 */
	public static synchronized void registerClient(String proto, Class<? extends FiletransferClient> clazz) {
		initRegisteredClients();
		doRegister(proto, clazz);
	}

	private static void doRegister(String proto, Class<? extends FiletransferClient> clazz){
		registeredClients.put(proto, clazz);
	}
	//register client classes via the META-INF/services mechanism
	private static synchronized void initRegisteredClients(){
		if(registeredClients.size()==0){
			ServiceLoader<ClientCapabilities>sl=ServiceLoader.load(ClientCapabilities.class);
			Iterator<ClientCapabilities>iter=sl.iterator();
			while(iter.hasNext()){
				ClientCapability[]cs=iter.next().getClientCapabilities();
				for(int j=0; j<cs.length;j++){
					ClientCapability c=cs[j];
					if(c instanceof RESTFTClientCapability){
						RESTFTClientCapability ftc=(RESTFTClientCapability)c;
						doRegister(ftc.getProtocol(), ftc.getImplementation());
					}
				}
			}
		}
	}

	public static String encode(String path) {
		return path.replace(" ", "%20");
	}

}
