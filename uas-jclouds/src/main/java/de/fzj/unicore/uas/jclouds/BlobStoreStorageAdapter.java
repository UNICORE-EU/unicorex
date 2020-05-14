package de.fzj.unicore.uas.jclouds;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.log4j.Logger;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.ContainerNotFoundException;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.BlobMetadata;
import org.jclouds.blobstore.domain.PageSet;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.jclouds.blobstore.domain.StorageType;
import org.jclouds.blobstore.options.ListContainerOptions;
import org.json.JSONException;
import org.json.JSONObject;

import de.fzj.unicore.uas.json.JSONUtil;
import de.fzj.unicore.uas.util.Pair;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.io.ChangeACL;
import de.fzj.unicore.xnjs.io.ChangePermissions;
import de.fzj.unicore.xnjs.io.FileFilter;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import de.fzj.unicore.xnjs.io.Permissions;
import de.fzj.unicore.xnjs.io.XnjsFile;
import de.fzj.unicore.xnjs.io.XnjsFileImpl;
import de.fzj.unicore.xnjs.io.XnjsFileWithACL;
import de.fzj.unicore.xnjs.io.XnjsStorageInfo;
import de.fzj.unicore.xnjs.util.IOUtils;
import eu.unicore.util.Log;

/**
 * storage adapter using a JClouds blob store
 * 
 * @author schuller
 */
public class BlobStoreStorageAdapter implements IStorageAdapter {

	private static final Logger logger = Log.getLogger(Log.SERVICES+".jclouds", BlobStoreStorageAdapter.class);

	private final BlobStore blobStore;

	private final String endpoint;

	private String storageRoot = "/";

	private final String region;
	
	public BlobStoreStorageAdapter(String endpoint, BlobStore blobStore, String region){
		this.blobStore = blobStore;
		this.endpoint = endpoint;
		this.region = region;
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
		return new File(getStorageRoot(),FilenameUtils.normalize(relativePath)).getPath();
	}

	/**
	 * split a full path into container (bucket) name and resource
	 * @param resource - relative path to resource
	 * @return pair (container,resource)
	 */
	public Pair<String,String>splitFullPath(String resource){
		resource = makePath(resource);
		while(resource.startsWith("/"))resource = resource.substring(1);
		String[]comps = resource.split("/",2);
		String container = comps[0];
		String blob = comps.length>1?comps[1]:"";
		while(blob.endsWith("/"))blob = blob.substring(0,blob.length()-1);
		return new Pair<String, String>(container,blob);
	}

	@Override
	public InputStream getInputStream(String resource)
			throws ExecutionException {
		try{
			Pair<String, String>path = splitFullPath(resource);
			Blob blob = blobStore.getBlob(path.getM1(), path.getM2());
			if(blob == null)throw new FileNotFoundException("Not found: "+resource);
			return blob.getPayload().openStream();
		}
		catch(IOException e){
			throw new ExecutionException(e);
		}
	}

	/**
	 * get an output stream for writing the specified number of bytes
	 * 
	 * @param resource - the target resource name
	 * @param append - whether to append data
	 * @param numBytes - number of bytes that will be written. If negative, the number of bytes is not known.
	 * 
	 * @throws ExecutionException
	 */
	public OutputStream getOutputStream(final String resource, boolean append, long numBytes)
			throws ExecutionException {
		try{
			final Pair<String, String>path = splitFullPath(resource);
			if(numBytes<0){
				// do not know the size, so will buffer locally :-/
				final long LIMIT = 1073741824; // 1 gig	
				final File tmp = new File(System.getProperty("java.io.tmpdir"),"s3-upload-+"+UUID.randomUUID().toString());
				logger.info("Upload of unknown size, caching in <"+tmp.getPath()+"> (up to "+LIMIT+ " bytes)");
				final FileOutputStream fos = new FileOutputStream(tmp);
				CountingOutputStream os = new CountingOutputStream(fos){
					public void close() throws IOException {
						super.close();
						try{
							Blob blob = blobStore.blobBuilder(path.getM2()).build();
							blob.setPayload(tmp);
							blobStore.putBlob(path.getM1(), blob);
						}
						finally{
							tmp.delete();
						}
					}
					// check whether limit for local caching was reached
					protected synchronized void beforeWrite(int n) {
						if(getByteCount()>LIMIT)throw new IllegalStateException("File size exceeds limit of <"
								+LIMIT+"> bytes and cannot be cached locally. Update your client!");
						super.beforeWrite(n);
					}
				};
				
				return os;
			}
			else{
				logger.info("Fixed-length upload of size <"+numBytes+">");
				PipedOutputStream os = new PipedOutputStream();
				PipedInputStream is = new PipedInputStream(1*1024*1024);
				os.connect(is);
				final Blob blob = blobStore.blobBuilder(path.getM2()).build();
				blob.setPayload(is);
				blob.getPayload().getContentMetadata().setContentLength(numBytes);

				// due to the piped streams, we need to do the actual upload
				// on a different thread
				Runnable r = new Runnable(){
					public void run(){
						blobStore.putBlob(path.getM1(), blob);	
					}
				};
				Thread uploader = new Thread(r);
				uploader.start();

				return os;
			}
		}
		catch(Exception e){
			throw new ExecutionException(e);
		}
	}

	/**
	 * s3 does not support streaming, so this will buffer the data in local /tmp storage
	 */
	@Override
	public OutputStream getOutputStream(final String resource, boolean append)
			throws ExecutionException {
		return getOutputStream(resource, append, -1);
	}

	@Override
	public OutputStream getOutputStream(String resource)
			throws ExecutionException {
		return getOutputStream(resource, false);
	}

	@Override
	public void mkdir(String dir) throws ExecutionException {
		Pair<String, String>path = splitFullPath(dir);
		if(path.getM2().isEmpty()){
			blobStore.createContainerInLocation(null, path.getM1());
		}
		else{
			createDir(path.getM1(), path.getM2());
		}
	}
	
	protected void createDir(String container, String dir){
		try{
			Blob x = blobStore.getBlob(container, dir);
			if(x==null){
				Blob blob = blobStore.blobBuilder(dir).payload("").type(StorageType.FOLDER).build();
				blobStore.putBlob(container, blob);
			}
		}catch(Exception ex){return;}
		
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
		Pair<String, String>path = splitFullPath(target);
		String container = path.getM1();
		String resource = path.getM2();
		if(resource.isEmpty()){
			blobStore.deleteContainer(container);
		}
		else{
			blobStore.removeBlob(container, resource);
		}
	}

	@Override
	public void rmdir(String target) throws ExecutionException {
		try{
			rm(target);
		}catch(Exception ex){
			Log.logException("Error deleting", ex);
		}
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
		Pair<String, String>path = splitFullPath(base);
		String container = path.getM1();
		base = path.getM2();
		if(base.isEmpty() && container.isEmpty()){
			return convert("",blobStore.list(), null);
		}
		else{
			ListContainerOptions options = ListContainerOptions.Builder.
					prefix(base).
					maxResults(limit).
					recursive();
			return convert("/"+container, blobStore.list(container, options),base);
		}
	}

	@Override
	public XnjsFile[] find(String base, FileFilter options, int offset,
			int limit) throws ExecutionException {
		return new XnjsFile[0];
	}

	@Override
	public XnjsFileWithACL getProperties(String file) throws ExecutionException {
		Pair<String, String>path = splitFullPath(file);
		String container = path.getM1();
		String resource = path.getM2();
		XnjsFileImpl res = new XnjsFileImpl();
		res.setPath(file);
		if(resource.isEmpty()){
			res.setDirectory(true);
			res.setPermissions(new Permissions(true, true, true));
		}
		else {
			try{
				ListContainerOptions options = ListContainerOptions.Builder.maxResults(10000).recursive();
				String base = FilenameUtils.getFullPath(resource);
				if(!base.isEmpty() && !"/".equals(base)){
					options.prefix(base);
				}
				while(resource.length()>1 && resource.endsWith("/")){
					resource = resource.substring(0,resource.length()-1);
				}
				for(StorageMetadata r: blobStore.list(container, options)){
					if(resource.equals(r.getName())){
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
	public String getFileSeparator() throws ExecutionException {
		return "/";
	}

	@Override
	public String getFileSystemIdentifier() {
		return "s3-"+endpoint;
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

	XnjsFile[]convert(String container, PageSet<? extends StorageMetadata> results, String target) {
		Collection<XnjsFile> converted = new ArrayList<XnjsFile>();
		String root = getStorageRoot();
		if(target!=null)target=IOUtils.getRelativePath(container+"/"+target, root);
		for(StorageMetadata r: results){
			XnjsFileImpl xf = new XnjsFileImpl();
			String path = IOUtils.getRelativePath(container+"/"+r.getName(), root);
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
		boolean isDir = StorageType.CONTAINER == r.getType() 
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
		Permissions perm = new Permissions(true,true,target.isDirectory());
		target.setPermissions(perm);
	}

}
