package de.fzj.unicore.uas.cdmi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.bouncycastle.util.encoders.Base64;
import org.json.JSONException;
import org.json.JSONObject;

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
import de.fzj.unicore.xnjs.util.BackedInputStream;
import de.fzj.unicore.xnjs.util.BackedOutputStream;
import de.fzj.unicore.xnjs.util.IOUtils;

/**
 * Access CDMI backend
 *  
 * @author schuller
 */
public class CDMIStorageAdapter implements IStorageAdapter {

	private final CDMIClient cdmiClient;

	private String storageRoot = "/";

	private int BUFSIZE = 256*1024;

	public CDMIStorageAdapter(CDMIClient cdmiClient){
		this.cdmiClient = cdmiClient;
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
		return IOUtils.getFullPath(getStorageRoot(), relativePath, true);
	}

	@Override
	public InputStream getInputStream(final String resource)
			throws ExecutionException {
		try{
			final String path = makePath(resource);
			JSONObject info = cdmiClient.getResourceInfo(path);
			InputStream is = new BackedInputStream(1024*1024, getSize(info)) {

				@Override
				protected long getTotalDataSize() throws IOException {
					return length;
				}

				@Override
				protected void fillBuffer() throws IOException {
					//compute bytes to be read: either buffer size, or remainder of file
					long numBytes=Math.min(length-bytesRead,buffer.length);
					try{
						avail=cdmiClient.readObjectData(path,buffer,bytesRead,numBytes);
						bytesRead+=avail;
					}catch(Exception e){
						throw new IOException(e);
					}
					pos=0;
				}
			};
			return is;
		}
		catch(Exception e){
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
			OutputStream os = new BackedOutputStream(append, BUFSIZE) {
				String path = makePath(resource);
				long bytesWritten = 0;
				// TODO check parent dir exists...

				@Override
				protected void writeBuffer() throws IOException {
					if(pos==0)return; // nothing to do
					try{
						JSONObject req = getWriteRequest();
						cdmiClient.writeObject(path, req, bytesWritten, bytesWritten+pos, !closing);
						bytesWritten += pos;
					}
					catch(Exception ex){
						throw new IOException(ex);
					}
				}

				private JSONObject getWriteRequest() throws Exception {
					JSONObject req = new JSONObject();
					req.put("mimetype",MediaType.APPLICATION_OCTET_STREAM);
					req.put("metadata",new JSONObject().put("unicoresms_datasize", bytesWritten+pos));
					req.put("valuetransferencoding","base64");
					req.put("value",Base64.toBase64String(buffer, 0, pos));
					return req;
				}

			};

			return os;
		}
		catch(Exception e){
			throw new ExecutionException(e);
		}
	}

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
		try{
			String path = makePath(dir);
			createDirs(path);
		}catch(Exception ex){
			throw new ExecutionException(ex);
		}
	}

	/**
	 * create dir including any parent dirs
	 * 
	 * @param dir
	 */
	protected void createDirs(String dir) throws Exception {
		File file = new File(dir);
		String parent = file.getParent();
		if(parent!=null && parent!="/"){
			if(!cdmiClient.directoryExists(parent)){
				createDirs(parent);
			}
		}
		cdmiClient.createDirectory(dir);
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
		throw new ExecutionException("ACLs not supported!");
	}

	@Override
	public void rmdir(String target) throws ExecutionException {
		rm(target);
	}

	@Override
	public void rm(String target) throws ExecutionException {
		XnjsFile f = getProperties(target);
		if(f!=null){
			if(f.isDirectory()){
				for(XnjsFile child: ls(target)){
					rmdir(child.getPath());
				}
			}
			else try{
				String fullPath = makePath(target);
				cdmiClient.setURL(cdmiClient.getEndpoint()+fullPath);
				cdmiClient.delete();
			}catch(Exception ex){
				throw new ExecutionException(ex);
			}
		}
		else throw new ExecutionException("File not found: <"+target+">");
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
		List<XnjsFile>res = new ArrayList<>();
		try{
			List<String>children = cdmiClient.listChildren(path,offset,limit);
			for(String child: children){
				res.add(getProperties(base+"/"+child));
			}
		}catch(Exception ex){
			throw new ExecutionException(ex);
		}
		return res.toArray(new XnjsFile[res.size()]);
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
		JSONObject info;
		try{
			boolean isDir = false;
			if(cdmiClient.directoryExists(path)){
				info = cdmiClient.getDirectoryInfo(path);
				isDir = true;
			}
			else{
				info = cdmiClient.getResourceInfo(path);
			}
			convert(res,path,info,isDir);
		}
		catch(FileNotFoundException fne){
			res = null;
		}catch(Exception ex){
			throw new ExecutionException(ex);
		}
		return res;
	}

	@Override
	public String getFileSeparator() {
		return "/";
	}

	@Override
	public String getFileSystemIdentifier() {
		return "cdmi-"+cdmiClient.getEndpoint();
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

	private void convert(XnjsFileImpl target, String fullpath, JSONObject infoFromCdmi, boolean isDir){
		String path = IOUtils.getRelativePath(fullpath, getStorageRoot());
		target.setPath(path);
		target.setDirectory(isDir);
		target.setSize(getSize(infoFromCdmi));
		target.setPermissions(new Permissions(true, true, true));
		JSONObject meta = infoFromCdmi.optJSONObject("metadata");
		if(meta!=null){
			target.setMetadata(meta.toString());
		}
	}

	private long getSize(JSONObject info){
		long l = -1;
		try{
			// implementation should(!) support cdmi_size
			l= info.getJSONObject("metadata").getLong("cdmi_size");
		}catch(JSONException ex){
			// we might have added this ourselves :)
			try{
				l= info.getJSONObject("metadata").getLong("unicoresms_datasize");
			}catch(JSONException ex1){}
		}
		return l;
	}

}
