package eu.unicore.uas.jclouds;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Logger;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.ContainerNotFoundException;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.BlobMetadata;
import org.jclouds.blobstore.domain.PageSet;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.jclouds.blobstore.domain.StorageType;
import org.jclouds.blobstore.options.ListContainerOptions;
import org.jclouds.domain.Location;
import org.jclouds.domain.LocationBuilder;
import org.jclouds.domain.LocationScope;
import org.json.JSONException;
import org.json.JSONObject;

import eu.unicore.services.Kernel;
import eu.unicore.uas.json.JSONUtil;
import eu.unicore.util.Log;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.io.ChangeACL;
import eu.unicore.xnjs.io.ChangePermissions;
import eu.unicore.xnjs.io.FileFilter;
import eu.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.xnjs.io.Permissions;
import eu.unicore.xnjs.io.XnjsFile;
import eu.unicore.xnjs.io.XnjsFileImpl;
import eu.unicore.xnjs.io.XnjsFileWithACL;
import eu.unicore.xnjs.io.XnjsStorageInfo;
import eu.unicore.xnjs.util.IOUtils;

/**
 * storage adapter using a JClouds blob store
 * 
 * @author schuller
 */
public class BlobStoreStorageAdapter implements IStorageAdapter {

	private static final Logger logger = Log.getLogger(Log.SERVICES, BlobStoreStorageAdapter.class);

	private final BlobStore blobStore;

	private final String endpoint;

	private final String bucket;

	private String storageRoot = "/";

	private final Kernel kernel;

	private final String region;

	/**
	 * @param kernel
	 * @param endpoint
	 * @param bucket
	 * @param blobStore
	 * @param region
	 */
	public BlobStoreStorageAdapter(Kernel kernel, String endpoint, String bucket, BlobStore blobStore, String region){
		this.kernel = kernel;
		this.blobStore = blobStore;
		this.endpoint = endpoint;
		this.region = region;
		this.bucket = bucket;
	}

	@Override
	public void setStorageRoot(String root) {
		storageRoot = root;
	}

	@Override
	public String getStorageRoot() {
		return storageRoot;
	}

	private String makePath(String relativePath){
		String p = new File(getStorageRoot()+getFileSeparator()+FilenameUtils.normalize(relativePath)).getPath();
		while(p.startsWith("/"))p=p.substring(1);
		return p;
	}

	@Override
	public ReadableByteChannel getReadChannel(String file) throws IOException {
		throw new IOException("Not implemented.");
	}

	@Override
	public InputStream getInputStream(String resource) throws IOException {
		String path = makePath(resource);
		Blob blob = blobStore.getBlob(bucket, path);
		if(blob == null)throw new FileNotFoundException("Not found: "+resource);
		return blob.getPayload().openStream();
	}

	/**
	 * get an output stream for writing the specified number of bytes
	 * 
	 * @param resource - the target resource name
	 * @param append - whether to append data
	 * @param numBytes - number of bytes that will be written. If negative, the number of bytes is not known.
	 * 
	 * @throws IOException
	 */
	@Override
	public OutputStream getOutputStream(final String resource, boolean append, long numBytes)
			throws IOException {
		if(numBytes<0)throw new IOException("Need content-length, streaming is not supported.");
		logger.debug("Prepare write to {} numbytes={}", resource, numBytes);
		final String path = makePath(resource);
		PipedOutputStream os = new PipedOutputStream();
		PipedInputStream is = new PipedInputStream(1*1024*1024);
		os.connect(is);
		final Blob blob = blobStore.blobBuilder(path).build();
		blob.setPayload(is);
		blob.getPayload().getContentMetadata().setContentLength(numBytes);
		// due to the piped streams, we need to do the actual upload
		// on a different thread
		if(kernel!=null) {
			kernel.getContainerProperties().getThreadingServices().
			getExecutorService().execute(()->blobStore.putBlob(bucket, blob));
		}else {
			new Thread(()->blobStore.putBlob(bucket, blob)).start();
		}
		return os;
	}

	@Override
	public OutputStream getOutputStream(final String resource, boolean append)
			throws IOException {
		return getOutputStream(resource, append, -1);
	}

	@Override
	public OutputStream getOutputStream(String resource) throws IOException {
		return getOutputStream(resource, false);
	}

	@Override
	public void mkdir(String dir) throws ExecutionException {
		String path = makePath(dir);
		if("/".equals(path) || path.isEmpty()) {
			createBucket();
		}else {
			createDir(bucket, path);
		}
	}

	protected void createBucket() {
		Location location = null;
		if(region!=null) {
			location = new LocationBuilder().id(region).scope(LocationScope.REGION).
					description("").metadata(Collections.emptyMap()).
					iso3166Codes(Collections.emptyList()).
					build();
		}
		boolean created = blobStore.createContainerInLocation(location, bucket);
		logger.debug("Connected to bucket {} (existing={})", bucket, !created);
	}

	protected void createDir(String container, String dir) throws ExecutionException {
		if(!dir.endsWith("/"))dir+="/";
		Blob x = blobStore.getBlob(container, dir);
		if(x==null){
			Blob blob = blobStore.blobBuilder(dir).payload("").type(StorageType.FOLDER).build();
			blobStore.putBlob(container, blob);
		}
		// create parent, if necessary
		File file = new File(dir);
		String parent = file.getParent();
		if(parent!=null && parent!="/"){
			createDir(container, parent);
		}
	}

	@Override
	public void chmod(String file, Permissions perm) throws ExecutionException {
		// NOP
	}

	@Override
	public void chmod2(String file, ChangePermissions[] perm, boolean recursive)
			throws ExecutionException {
		// NOP
	}

	@Override
	public void chgrp(String file, String newGroup, boolean recursive)
			throws ExecutionException {
		// NOP
	}

	@Override
	public void setfacl(String file, boolean clearAll, ChangeACL[] changeACL,
			boolean recursive) throws ExecutionException {
		// NOP
	}

	@Override
	public void rm(String target) throws ExecutionException {
		blobStore.removeBlob(bucket, makePath(target));
	}

	@Override
	public void rmdir(String target) throws ExecutionException {
		rm(target);
	}

	@Override
	public void cp(String source, String target) throws ExecutionException {
		throw new ExecutionException("Not implemented!");
	}

	@Override
	public void link(String source, String linkName) throws ExecutionException {
		throw new ExecutionException("Symlinking is not supported!");
	}

	@Override
	public void rename(String source, String target) throws ExecutionException {
		throw new ExecutionException("Not implemented!");
	}

	@Override
	public XnjsFile[] ls(String base) throws ExecutionException {
		return ls(base,0,1000,false);
	}

	@Override
	public XnjsFile[] ls(String base, int offset, int limit, boolean filter)
			throws ExecutionException {
		String path = makePath(base);
		if(path.length()>0 && !path.endsWith("/"))path+="/";
		ListContainerOptions options = ListContainerOptions.Builder.
					maxResults(limit).delimiter("/");
		if(path.length()>0)options.prefix(path);
		return convert(blobStore.list(bucket, options), path);
	}

	@Override
	public XnjsFile[] find(String base, FileFilter options, int offset,
			int limit) throws ExecutionException {
		return new XnjsFile[0];
	}

	@Override
	public XnjsFileWithACL getProperties(String file) throws ExecutionException {
		String path = makePath(file);
		XnjsFileImpl res = new XnjsFileImpl();
		res.setPath(file);
		if(path.isEmpty() || path=="/"){
			res.setDirectory(true);
			res.setPermissions(new Permissions(true, true, true));
		}
		else {
			try{
				ListContainerOptions options = ListContainerOptions.Builder.maxResults(10000).recursive();
				options.prefix(path);
				for(StorageMetadata r: blobStore.list(bucket, options)){
					if(path.equals(r.getName())|| (path+"/").equals(r.getName())){
						convert(res,r);
						return res;
					}
				}
				return null;
			}
			catch(ContainerNotFoundException cnfe){
				return null;	
			}
		}
		return res;
	}

	@Override
	public String getFileSeparator() {
		return "/";
	}

	@Override
	public String getFileSystemIdentifier() {
		return "s3-"+endpoint+"/"+bucket;
	}

	@Override
	public XnjsStorageInfo getAvailableDiskSpace(String path) {
		return new XnjsStorageInfo(); 
	}

	@Override
	public boolean isACLSupported(String path) throws ExecutionException {
		return false;
	}

	@Override
	public void setUmask(String umask) {
		// NOP
	}

	@Override
	public String getUmask() {
		return null;
	}

	XnjsFile[]convert(PageSet<? extends StorageMetadata> results, String target) {
		Collection<XnjsFile> converted = new ArrayList<XnjsFile>();
		String root = getStorageRoot();
		if(target!=null)target=IOUtils.getRelativePath(target, root);
		for(StorageMetadata r: results){
			XnjsFileImpl xf = new XnjsFileImpl();
			String path = IOUtils.getRelativePath(r.getName(), root);
			if(target!=null && target.equals(path)){
				continue;
			}
			xf.setPath(path);
			convert(xf,r);
			converted.add(xf);
		}
		return converted.toArray(new XnjsFile[converted.size()]);
	}

	void convert(XnjsFileImpl target, StorageMetadata r){
		JSONObject meta = new JSONObject();
		if(r.getUserMetadata()!=null){
			meta = JSONUtil.asJSON(r.getUserMetadata());
		}
		if(r instanceof BlobMetadata){
			BlobMetadata bm = (BlobMetadata)r;
			target.setSize(bm.getContentMetadata().getContentLength());
			try{
				meta.put("Content-Type",bm.getContentMetadata().getContentType());
				meta.put("Content-MD5",bm.getContentMetadata().getContentMD5AsHashCode());
			}catch(JSONException e){}
		}
		target.setMetadata(meta.toString());
		boolean isDir = r.getName().endsWith("/")
					  || StorageType.CONTAINER == r.getType() 
				      || StorageType.FOLDER == r.getType() 
				      || StorageType.RELATIVE_PATH == r.getType();
		target.setDirectory(isDir);
		if(r.getLastModified()!=null){
			Calendar lastMod = Calendar.getInstance();
			lastMod.setTime(r.getLastModified());
			target.setLastModified(lastMod);
		}
		else target.setLastModified(Calendar.getInstance());
		// TODO
		target.setPermissions(new Permissions(true, true, target.isDirectory()));
	}

}