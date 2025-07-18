package eu.unicore.xnjs.tsi.local;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;

import org.apache.logging.log4j.Logger;

import eu.unicore.security.Client;
import eu.unicore.xnjs.XNJSProperties;
import eu.unicore.xnjs.ems.ExecutionContext;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.ems.InternalManager;
import eu.unicore.xnjs.io.ChangeACL;
import eu.unicore.xnjs.io.ChangePermissions;
import eu.unicore.xnjs.io.ChangePermissions.Mode;
import eu.unicore.xnjs.io.ChangePermissions.PermissionsClass;
import eu.unicore.xnjs.io.FileFilter;
import eu.unicore.xnjs.io.Permissions;
import eu.unicore.xnjs.io.XnjsFile;
import eu.unicore.xnjs.io.XnjsFileImpl;
import eu.unicore.xnjs.io.XnjsFileWithACL;
import eu.unicore.xnjs.io.XnjsStorageInfo;
import eu.unicore.xnjs.tsi.TSI;
import eu.unicore.xnjs.tsi.TSIBusyException;
import eu.unicore.xnjs.util.ErrorCode;
import eu.unicore.xnjs.util.IOUtils;
import eu.unicore.xnjs.util.LogUtil;
import jakarta.inject.Inject;

/**
 * This is a local target system interface that runs on the XNJS machine<br/>
 * 
 * @author schuller
 */
public class LocalTS implements TSI {

	private static final Logger logger=LogUtil.getLogger(LogUtil.TSI,LocalTS.class);

	@SuppressWarnings("unused")
	private Client client;

	private String storageRoot;

	private final InternalManager manager;

	private final LocalTSIProperties tsiProperties;
	
	private final XNJSProperties xnjsProperties;
	
	/**
	 * inverted umask for rwx perms of the owner (1st 3) and others.
	 */
	private boolean[] invertedUmask = {true, true, true, false, false, false};

	private Set<PosixFilePermission> posixFilePermissions;
	private Set<PosixFilePermission> posixDirPermissions;
	private boolean posixSupport;

	private static final boolean[] DEFAULT_FILE_PERMS = {true, true, false, true, true, false};
	private static final boolean[] DEFAULT_DIR_PERMS = {true, true, true, true, true, true};

	@Inject
	public LocalTS(InternalManager manager, LocalTSIProperties properties, XNJSProperties xnjsProperties) {
		this.manager = manager;
		this.tsiProperties = properties;
		this.xnjsProperties = xnjsProperties;
		updatePosixPermissions();
		posixSupport = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
	}

	public boolean isLocal(){
		return true;
	}

	public void setClient(Client client){
		this.client=client;
	}

	public void chmod(String file, Permissions perm) throws ExecutionException {
		File target=makeTarget(file);
		target.setExecutable(perm.isExecutable());
		target.setReadable(perm.isReadable());
		target.setWritable(perm.isWritable());
	}

	/*
	 *  TODO chunked, because this will go outofmemory on large files
	 */
	public void cp(String source, String target) throws ExecutionException {
		File tFile=makeTarget(target);
		File sFile=makeTarget(source);
		logger.debug("cp: {}->{}", sFile, tFile);
		if(tFile.isDirectory()){
			tFile=new File(tFile,sFile.getName());
		}
		try(FileOutputStream fos = new FileOutputStream(tFile); 
			FileInputStream fis = new FileInputStream(sFile))
		{
			FileChannel out=fos.getChannel();
			FileChannel in=fis.getChannel();
			out.write(in.map(FileChannel.MapMode.READ_ONLY,0,sFile.length()));
		}catch(Exception e) {
			throw ExecutionException.wrapped(e);
		}
	}

	public void rename(String source, String target) throws ExecutionException {
		boolean success=true;
		try{
			File tFile=makeTarget(target);
			File sFile=makeTarget(source);
			logger.debug("rename: {}->{}", sFile, tFile);
			success=sFile.renameTo(tFile);
		}catch(Exception e) {
			throw ExecutionException.wrapped(e);
		}
		if(!success){
			throw new ExecutionException(ErrorCode.ERR_TSI_EXECUTION, "Could not rename file.");
		}
	}

	public void rmdir(String target) throws ExecutionException {
		File f=makeTarget(target);
		logger.debug("rmdir: {}", f);
		_rmdir(f);
	}

	private void _rmdir(File f)throws ExecutionException{
		if(f.isDirectory()){
			File[] children=f.listFiles();
			if(children!=null)for(File child: children)_rmdir(child);
		}
		if(!f.delete()){
			throw new ExecutionException(ErrorCode.ERR_TSI_EXECUTION, "Could not delete.");
		}
	}

	public void mkdir(String dir) throws ExecutionException {
		try{
			File f=makeTarget(dir);
			if(f.exists() && !f.isDirectory())
				throw new IOException("Cannot create directory <"+dir+">, file exists!");

			if (f.mkdirs() || f.isDirectory()) {
				setPermissions(f);
			} else {
				logger.error("Failed to create directory <{}>", f.getAbsolutePath());
				throw new IOException("Could not create dir <"+dir+">");
			}
		}catch(Exception e) {
			throw ExecutionException.wrapped(e);
		}
	}

	public void mkfifo(String dir) throws ExecutionException {
		try{
			File f=makeTarget(dir);
			if (IOUtils.isNonUnix())
				throw new ExecutionException(ErrorCode.ERR_OPERATION_NOT_POSSIBLE,
						"FIFO can not be created on a non-UNIX operating system.");
			execAndWait("mkfifo "+f.getAbsolutePath(), new ExecutionContext());
			setPermissions(f);
		}catch(Exception e) {
			throw ExecutionException.wrapped(e);
		}
	}

	public void link(String target, String linkName) throws ExecutionException {
		try{
			File newLink=makeTarget(linkName);
			File targetFile=makeTarget(target);
			logger.debug("linking: {} -> {}", newLink, targetFile);
			if (IOUtils.isNonUnix()){
				throw new ExecutionException(ErrorCode.ERR_OPERATION_NOT_POSSIBLE,
						"Link can not be created on a non-UNIX operating system.");
			}
			ExecutionContext ec = new ExecutionContext();
			ec.setWorkingDirectory(".");
			ec.setDiscardOutput(true);
			execAndWait("ln -s "+targetFile.getAbsolutePath()+" "+newLink.getAbsolutePath(),
					ec);
		}catch(Exception e) {
			throw ExecutionException.wrapped(e);
		}
	}

	public String getFileSeparator(){
		return File.separator;
	}

	public String getHomePath() throws ExecutionException{
		return System.getProperty("user.home");
	}

	/**
	 * get an environment value - for the embedded TSI this does not make
	 * any sense, so <code>null</code> is returned in all cases
	 */
	public String getEnvironment(String name) throws ExecutionException{
		return System.getenv(name);
	}

	public String resolve(String name) throws ExecutionException{
		return name;
	}

	/* (non-Javadoc)
	 * @see de.fzj.unicore.xnjs.ems.TSI#rm(java.lang.String)
	 */
	public void rm(String target) throws ExecutionException {
		File f=makeTarget(target);
		try{
			logger.debug("Delete: {}", f);
			boolean OK=f.delete();
			if(!OK)throw new IOException("Did not delete file.");
		}catch(Exception e) {
			throw ExecutionException.wrapped(e);
		}
	}

	public XnjsFile[] ls(String base)throws ExecutionException{
		return ls(base,0,Integer.MAX_VALUE,false);
	}

	/**
	 * a directory listing from the working directory
	 * @param limit
	 * @param offset
	 */
	public XnjsFile[] ls(String base, int offset, int limit, boolean filter)throws ExecutionException{
		try{
			logger.debug("listing {}", base);
			File dir=makeTarget(base);
			if(!dir.isDirectory())throw new IOException("Not a directory.");
			File fs[]=dir.listFiles();
			if(offset>fs.length)throw new IllegalArgumentException("Specified offset <"+offset+"> is larger than the total number of results <"+fs.length+">");
			int numResults=Math.min(fs.length-offset, limit);
			XnjsFile[] res=new XnjsFileImpl[numResults];
			for(int i=0;i<numResults;i++){
				File f=fs[offset+i];
				Permissions permissions=Permissions.getPermissions(f);
				if (filter && !permissions.isAccessible())continue;
				res[i]=new XnjsFileImpl(
						makePathRelativeToRoot(f.getAbsolutePath()), //for the time being...
						f.length(),
						f.isDirectory(),
						f.lastModified(),
						permissions,
						//can't fully support the owner flag in Java TSI
						true);
			}
			return res;
		}
		catch(Exception e) {
			throw ExecutionException.wrapped(e);
		}
	}

	protected String makePathRelativeToRoot(String absolutePath){
		return IOUtils.getRelativePath(new File(absolutePath), getStorageRoot());
	}

	public XnjsFileWithACL getProperties(String file)throws ExecutionException{
		try{
			File f=makeTarget(file);
			if(!f.exists())return null;
			XnjsFileImpl res=new XnjsFileImpl();
			res.setDirectory(f.isDirectory());
			Calendar lastModified=Calendar.getInstance();
			lastModified.setTimeInMillis(f.lastModified());
			res.setLastModified(lastModified);
			res.setPath(makePathRelativeToRoot(f.getAbsolutePath()));
			res.setSize(f.length());
			Permissions permissions=Permissions.getPermissions(f);
			res.setPermissions(permissions);
			return res;
		}catch(Exception e) {
			throw ExecutionException.wrapped(e);
		}
	}


	public XnjsFile[] find(String path, FileFilter options, int offset, int limit)throws ExecutionException{
		try{
			File dir=new File(getStorageRoot(),path);
			if(!dir.isDirectory())throw new IOException("Not a directory: "+path);
			File fs[]=dir.listFiles();
			if(fs==null){
				throw new IOException("IO problem when listing files in <"+path+">");
			}
			List<XnjsFile> res=new ArrayList<XnjsFile>();
			for(int i=0;i<fs.length;i++){
				if(options.recurse() && fs[i].isDirectory()){
					//TODO check limits/offset
					XnjsFile[] subResult=find(path+getFileSeparator()+fs[i].getName(),options, offset, limit);
					for(XnjsFile f: subResult)res.add(f);
					continue;
				}
				Permissions permissions=Permissions.getPermissions(fs[i]);
				XnjsFile f=new XnjsFileImpl(
						makePathRelativeToRoot(fs[i].getAbsolutePath()),
						fs[i].length(),
						fs[i].isDirectory(),
						fs[i].lastModified(),
						permissions,
						//can't fully support the owner flag in Java TSI
						true);
				if(options.accept(f, this))	res.add(f);
			}
			return res.toArray(new XnjsFile[res.size()]);
		}
		catch(IOException e) {
			throw new ExecutionException("Could not execute find",e);
		}
	}

	/**
	 * Execute a command. This delegates execution to a executor thread pool.
	 */
	public void exec(String command, ExecutionContext ec) throws TSIBusyException,ExecutionException {
		try{
			new LocalExecution(null, tsiProperties, manager, command, ec).execute();
		}catch(RejectedExecutionException re){
			throw new TSIBusyException("Execution currently not possible.");
		}catch(Exception ex){
			throw new ExecutionException("Error while executing <"+command+">",ex);}
	}

	/**
	 * Execute a command synchronously
	 */
	public void execAndWait(String orig, ExecutionContext ec) throws TSIBusyException,ExecutionException {
		try{
			LocalExecution ex=new LocalExecution(null, tsiProperties, manager, orig, ec);
			ex.execute(false);
		}catch(RejectedExecutionException re){
			throw new TSIBusyException("Execution currently not possible.");
		}catch(Exception ex){
			throw new ExecutionException("Error while executing <"+orig+">",ex);}
	}

	// ---  helpers

	/**
	 *
	 * <ul>
	 *  <li>makes the given file name full, i.e. including the storage root</li>
	 *  <li>On windows systems, <code>'/'</code> will be replaced by <code>'\\'</code></li>
	 * </ul>
	 * @param target
	 * @return a normalised, full filename
	 */
	private File makeTarget(String target) {
		if (IOUtils.isNonUnix()){
			target = target.replace('/',File.separatorChar);
			File f=new File(target);
			if(f.isAbsolute())return f;
		}
		File f=new File(getStorageRoot(),target);
		return f;
	}

	/**
	 * get an input stream for reading from the specified file 
	 */
	public InputStream getInputStream(String file) throws IOException {
		return new FileInputStream(makeTarget(file));
	}
	
	public ReadableByteChannel getReadChannel(String file) throws IOException {
		return FileChannel.open(Path.of(file));
	}

	public OutputStream getOutputStream(String file, boolean append, long numbytes)
			throws IOException {
		File f = makeTarget(file);
		if (!f.exists()) {
			f.createNewFile();
			setPermissions(f);
		}
		return new FileOutputStream(f,append);
	}


	public OutputStream getOutputStream(String file, boolean append)
			throws IOException {
		return getOutputStream(file,append,-1);
	}
	
	public OutputStream getOutputStream(String file) throws IOException {
		return getOutputStream(file,false);
	}

	//no-op for the embedded TSI
	public void setPreferredBufferSize(int size){}

	/**
	 * get random access file 
	 */
	public RandomAccessFile getRandomAccessFile(String file) throws ExecutionException {
		try{
			return new RandomAccessFile(makeTarget(file),"rws");
		}
		catch(Exception e){
			throw new ExecutionException(e);
		}
	}

	public String getFileSystemIdentifier(){
		try{
			return "LOCAL TSI at "+InetAddress.getLocalHost().getHostName();
		}catch(Exception ex){
			return null;
		}
	} 

	public XnjsStorageInfo getAvailableDiskSpace(String path){
		XnjsStorageInfo x=new XnjsStorageInfo();
		File f=new File(path);
		long total=f.getTotalSpace();
		if(total>-1){
			x.setTotalSpace(total);
			x.setFreeSpace(f.getFreeSpace());
			x.setUsableSpace(f.getUsableSpace());
		}
		return x;
	}

	public String getStorageRoot() {
		if(storageRoot==null)storageRoot=getRoot();
		return storageRoot;
	}

	private String getRoot(){
		if(IOUtils.isNonUnix()){
			return new File("/").getAbsolutePath();
		}
		else return "/";
	}

	public void setStorageRoot(String storageRoot) {
		//make absolute, which is possible since we are local
		this.storageRoot = new File(storageRoot).getAbsolutePath();
	}

	@Override
	public String[] getGroups() throws TSIBusyException, ExecutionException {
		return new String[0];
	}

	@Override
	public void chmod2(String file, ChangePermissions[] perm, boolean recursive)
			throws ExecutionException {
		if (recursive){
			logger.warn("Recursive permissions manipulation " +
					"is unsupported in Java TSI.");
		}
		File f = makeTarget(file);
		boolean r,w,x;
		Permissions current;
		for (ChangePermissions pSpec: perm) {
			if (!pSpec.getClazz().equals(PermissionsClass.OWNER)){
				// not really important
				logger.warn("Only OWNER permissions can be modified in Java TSI.");
				continue;
			}
			String p = pSpec.getPermissions();
			r = p.charAt(0) == 'r';
			w = p.charAt(1) == 'w';
			x = p.charAt(2) == 'x';
			if (!pSpec.getMode().equals(Mode.SET)) {
				current = Permissions.getPermissions(f);
				boolean newValue = pSpec.getMode().equals(Mode.ADD); 
				if (r) 
					current.setReadable(newValue);
				if (w)
					current.setWritable(newValue);
				if (x)
					current.setExecutable(newValue);
			} else
				current = new Permissions(r, w, x);
			chmod(file, current);
		}
	}

	@Override
	public void chgrp(String file, String newGroup, boolean recursive)
			throws ExecutionException {
		logger.warn("Owning group manipulation " +
				"is unsupported in Java TSI");
	}

	@Override
	public boolean isACLSupported(String path) {
		return false;
	}

	@Override
	public void setfacl(String file, boolean clearAll, ChangeACL[] changeACL, boolean recursive)
			throws ExecutionException {
		logger.warn("ACL manipulation is unsupported in Java TSI");
	}

	/**
	 * Tries to set permissions as close as possible to the umask.
	 * In Java it is impossible to set group's permissions. Therefore 
	 * group permissions are set as the others.
	 */
	private void setPermissions(File file) {
		if (posixSupport) {
			try {
				Files.setPosixFilePermissions(file.toPath(), file.isDirectory() ? posixDirPermissions : posixFilePermissions);
				return;
			} catch (IOException e) {
				// should we log this? We'll try and default to old behavior
			}
		}
		// default to old behavior
		boolean perms[];
		if (file.isDirectory())
			perms = DEFAULT_DIR_PERMS;
		else
			perms = DEFAULT_FILE_PERMS;
		file.setReadable(perms[3]&&invertedUmask[3], false);
		file.setWritable(perms[4]&&invertedUmask[4], false);
		file.setExecutable(perms[5]&&invertedUmask[5], false);
		file.setReadable(perms[0]&&invertedUmask[0], true);
		file.setWritable(perms[1]&&invertedUmask[1], true);
		file.setExecutable(perms[2]&&invertedUmask[2], true);
	}

	@Override
	public void setUmask(String umaskStr) {
		if (umaskStr == null) {
			umaskStr = xnjsProperties.getValue(XNJSProperties.DEFAULT_UMASK);
		}
		int mask = Integer.parseInt(umaskStr, 8);
		for (int i=0; i<6; i++) {
			invertedUmask[i] = (mask & (1<<(8-i))) == 0;
		}
		updatePosixPermissions();
	}

	
	private void updatePosixPermissions() {
		Set<PosixFilePermission> posixFilePermissions = getPosixPermissions(DEFAULT_FILE_PERMS);
		Set<PosixFilePermission> posixDirPermissions = getPosixPermissions(DEFAULT_DIR_PERMS);
		
		this.posixFilePermissions = posixFilePermissions;
		this.posixDirPermissions = posixDirPermissions;
	}

	/**
	 * Generate {@link Set<PosixFilePermission>} to be used for setting
	 * file/directory permissions
	 * 
	 * @param defaultPerms
	 *            default permissions for either files or directories
	 * @return
	 */
	private Set<PosixFilePermission> getPosixPermissions(boolean[] defaultPerms) {
		Set<PosixFilePermission> posixPermissions = new HashSet<PosixFilePermission>();
		if(defaultPerms[0] && invertedUmask[0]) {
			posixPermissions.add(PosixFilePermission.OWNER_READ);
		}
		if(defaultPerms[1] && invertedUmask[1]) {
			posixPermissions.add(PosixFilePermission.OWNER_WRITE);
		}
		if(defaultPerms[2] && invertedUmask[2]) {
			posixPermissions.add(PosixFilePermission.OWNER_EXECUTE);
		}
		if(defaultPerms[3] && invertedUmask[3]) {
			posixPermissions.add(PosixFilePermission.GROUP_READ);
			posixPermissions.add(PosixFilePermission.OTHERS_READ);
		}
		if(defaultPerms[4] && invertedUmask[4]) {
			posixPermissions.add(PosixFilePermission.GROUP_WRITE);
			posixPermissions.add(PosixFilePermission.OTHERS_WRITE);
		}
		if(defaultPerms[5] && invertedUmask[5]) {
			posixPermissions.add(PosixFilePermission.GROUP_EXECUTE);
			posixPermissions.add(PosixFilePermission.OTHERS_EXECUTE);
		}
		return posixPermissions;
	}

	@Override
	public String getUmask() {
		int ret = 0;
		for (int i=0, v=4; i<3; i++, v/=2)
			ret += ((invertedUmask[i]?0:1) * v) * 64;  
		for (int i=3, v=4; i<6; i++, v/=2)
			ret += ((invertedUmask[i]?0:1) * v) * 8;  
		for (int i=3, v=4; i<6; i++, v/=2)
			ret += ((invertedUmask[i]?0:1) * v);  
		return "0"+Integer.toOctalString(ret);
	}

	@Override
	public SocketChannel openConnection(String address) throws Exception {
		if(address.startsWith("file:"))throw new Exception("Domain sockets not supported");
		String[] t = address.split(":");
		String host;
		int port;
		if(t.length<2) {
			host="localhost";
			port = Integer.parseInt(t[0]);
		}
		else {
			host = t[0];
			port = Integer.parseInt(t[1]);
		}
		return SocketChannel.open(new InetSocketAddress(host, port));
	}
	
}

