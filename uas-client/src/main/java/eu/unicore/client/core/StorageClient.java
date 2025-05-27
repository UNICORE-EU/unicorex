package eu.unicore.client.core;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.apache.commons.io.FilenameUtils;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.net.URIBuilder;
import org.json.JSONObject;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.FileList.FileListEntry;
import eu.unicore.client.data.FileClient;
import eu.unicore.client.data.FiletransferClient;
import eu.unicore.client.data.HttpFileTransferClient;
import eu.unicore.client.data.TransferControllerClient;
import eu.unicore.client.utils.Configurable;
import eu.unicore.services.restclient.BaseClient;
import eu.unicore.services.restclient.ClientCapabilities;
import eu.unicore.services.restclient.ClientCapability;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.uas.CoreClientCapabilities.RESTFTClientCapability;
import eu.unicore.uas.fts.FiletransferOptions;
import eu.unicore.uas.json.JSONUtil;
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

	public FileList ls(String basedir) throws Exception {
		String url = getLinkUrl("files")+normalize(basedir);
		Endpoint ep = endpoint.cloneTo(url);
		return new FileList(this, basedir, ep, security, auth);
	}

	/**
	 * get file properties for a path 
	 *
	 * @param path
	 * @throws Exception
	 */
	public FileListEntry stat(String path) throws Exception {
		String url = getLinkUrl("files")+normalize(path);
		Endpoint ep = endpoint.cloneTo(url);
		JSONObject props = new BaseServiceClient(ep, security, auth).getProperties();
		return new FileListEntry(path, props);
	}

	public FileClient getFileClient(String path) throws Exception {
		String url = getLinkUrl("files")+normalize(path);
		Endpoint ep = endpoint.cloneTo(url);
		return new FileClient(ep, security, auth);
	}

	public void mkdir(String path) throws Exception {
		getFileClient(path).mkdir();
	}

	public void chmod(String path, String unixPermissions) throws Exception {
		getFileClient(path).chmod(unixPermissions);
	}

	public void copy(String from, String to) throws Exception {
		JSONObject op = new JSONObject();
		op.put("from", from);
		op.put("to", to);
		executeAction("copy", op);
	}

	public void rename(String from, String to) throws Exception {
		JSONObject op = new JSONObject();
		op.put("from", from);
		op.put("to", to);
		executeAction("rename", op);
	}

	public boolean supportsMetadata() throws Exception {
		return getProperties().getBoolean("metadataSupported");
	}

	public List<String> searchMetadata(String query) throws Exception {
		BaseClient bc = createTransport(endpoint.getUrl()+"/search", security, auth);
		URIBuilder ub = new URIBuilder(bc.getURL());
		ub.addParameter("q", query);
		bc.setURL(ub.build().toString());
		JSONObject searchResult = bc.getJSON();
		if(!"OK".equals(searchResult.getString("status"))){
			String msg = "Error searching metadata: "+searchResult.optString("statusMessage", "n/a");
			throw new Exception(msg);
		}
		List<String> result = new ArrayList<>();
		JSONObject links = searchResult.getJSONObject("_links");
		for(String name: links.keySet()) {
			if(name.startsWith("search-result")) {
				result.add(links.getJSONObject(name).getString("href"));
			}
		}
		return result;
	}

	public String getMountPoint() throws Exception {
		return getProperties().optString("mountPoint", null);
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
			try(ClassicHttpResponse res = c.post(json)){
				JSONObject response = c.asJSON(res);
				Endpoint ep = new Endpoint(res.getFirstHeader("Location").getValue());
				FiletransferClient fts = clazz.getConstructor(new Class[] { 
						Endpoint.class, JSONObject.class, 
						IClientConfiguration.class, IAuthCallback.class }
						).newInstance(new Object[] { ep, response, security, auth});
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
			}
		} catch (Exception e) {
			String msg=Log.createFaultMessage("Can't create import.", e);
			throw new IOException(msg,e);
		}
	}

	/**
	 * create BFT upload
	 * @param target file name
	 * @param number of bytes (or -1 if not known)
	 */
	public HttpFileTransferClient upload(String filename, long numberOfBytes) throws Exception {
		return (HttpFileTransferClient)createImport(filename, false, numberOfBytes, "BFT", null);
	}

	/**
	 * create BFT upload
	 * @param target file name
	 */
	public HttpFileTransferClient upload(String filename) throws Exception {
		return upload(filename, -1);
	}

	public FiletransferClient createExport(String filename,  String protocol, Map<String,String>extraParameters) throws Exception {

		Class<? extends FiletransferClient> clazz = getFiletransferClientClass(protocol);

		try {
			JSONObject json = new JSONObject();
			json.put("file", filename);
			json.put("protocol",protocol);
			json.put("extraParameters", JSONUtil.asJSON(extraParameters));
			
			BaseClient c = createTransport(endpoint.getUrl()+"/exports", security, auth);
			try(ClassicHttpResponse res = c.post(json)){
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
			}
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
	 * @param sourceURL - the URL of the remote file to fetch
	 * @param fileName  - the local file name
	 * @param extraParameters - additional parameters (can be null)
	 * @param protocol  - protocol to use, can be null if default BFT is used or protocol is encoded into source URL
	 */
	public TransferControllerClient fetchFile(String sourceURL, String fileName,
			Map<String,String>extraParameters, String protocol) throws Exception {
		JSONObject json = new JSONObject();
		json.put("source", sourceURL);
		json.put("file", fileName);
		if(extraParameters!=null && extraParameters.size()>0) {
			json.put("extraParameters", JSONUtil.asJSON(extraParameters));
		}
		if(protocol!=null)json.put("protocol",protocol);
		BaseClient c = createTransport(endpoint.getUrl()+"/transfers", security, auth);
		String url = c.create(json);
		return new TransferControllerClient(new Endpoint(url), security, auth);
	}

	/**
	 * initiate a server-server transfer, which will send a file from this storage to a remote location
	 * 
	 * @param fileName  - local file to send
	 * @param targetURL - the remote file URL
	 * @param extraParameters - additional parameters (can be null)
	 * @param protocol  - protocol, can be null if default BFT is used or protocol is encoded into the target URL
	 */
	public TransferControllerClient sendFile(String fileName, String targetURL,
			Map<String,String>extraParameters, String protocol) throws Exception {
		JSONObject json = new JSONObject();
		json.put("file", fileName);
		json.put("target", targetURL);
		if(extraParameters!=null && extraParameters.size()>0) {
			json.put("extraParameters", JSONUtil.asJSON(extraParameters));
		}
		if(protocol!=null)json.put("protocol",protocol);
		BaseClient c = createTransport(endpoint.getUrl()+"/transfers", security, auth);
		String url = c.create(json);
		return new TransferControllerClient(new Endpoint(url), security, auth);
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
	 * @see RESTFTClientCapability
	 * @param proto - the protocol
	 * @param clazz - the {@link FiletransferClient} class
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

	/**
	 * <ul>
	 *   <li>make sure path is not null and starts with a "/"
	 *   <li>remove double "/"
	 *   <li>replace windows slashes with "/"
	 *   <li>encode spaces
	 * </ul>
	 * @param path
	 * @return normalized path
	 */
	public static String normalize(String path) {
		if(path==null)path = "/";
		if(!path.startsWith("/"))path="/"+path;
		path = FilenameUtils.normalize(path, true);
		return path.replace(" ", "%20");
	}

}
