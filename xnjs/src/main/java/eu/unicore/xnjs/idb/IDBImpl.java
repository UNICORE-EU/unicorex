package eu.unicore.xnjs.idb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import com.google.common.primitives.Longs;

import eu.unicore.security.Client;
import eu.unicore.security.Queue;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.xnjs.XNJSProperties;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.io.FileFilter;
import eu.unicore.xnjs.io.SimpleFindOptions;
import eu.unicore.xnjs.io.XnjsFile;
import eu.unicore.xnjs.json.JsonIDB;
import eu.unicore.xnjs.resources.Resource;
import eu.unicore.xnjs.resources.Resource.Category;
import eu.unicore.xnjs.resources.ResourceSet;
import eu.unicore.xnjs.resources.StringResource;
import eu.unicore.xnjs.resources.ValueListResource;
import eu.unicore.xnjs.tsi.TSI;
import eu.unicore.xnjs.tsi.TSIFactory;
import eu.unicore.xnjs.util.ErrorCode;
import eu.unicore.xnjs.util.LogUtil;

/**
 * An abstract IDB implementation supporting multiple IDB files (with a single main file) and
 * per-user extensions. 
 * Concrete subclasses have to deal with the representation of the IDB content
 * 
 * @author schuller
 */
@Singleton
public class IDBImpl implements IDB {
	
	private static final String UNIX_VAR = "\\$\\{?(\\w+)\\}?";
	private static final String WIN_VAR = "%(\\w+?)%";
	private static final String VAR = UNIX_VAR+"|"+WIN_VAR;
	public static final Pattern ARG_PATTERN=Pattern.compile("\\s?(.*?)"+"("+VAR+")(.*?)\\s*", Pattern.DOTALL);
	public static final String DEFAULT_PARTITION = "DEFAULT_PARTITION";

	public static final String DEFAULT_SCRIPT_HEADER = "#!/bin/bash -l\n";

	protected static final Logger logger=LogUtil.getLogger(LogUtil.JOBS,IDBImpl.class);

	private final Collection<ApplicationInfo> idb = new ArrayList<>();

	protected final Map<String, String> filespaces = Collections.synchronizedMap(new HashMap<>());

	protected final Map<String, String> textInfoProperties = new HashMap<>();

	protected String submitTemplate;

	protected String executeTemplate;

	protected String scriptHeader;

	protected final List<Partition> partitions = new ArrayList<>();

	protected final TSIFactory tsiFactory;

	protected File idbFile;

	protected long lastUpdate = 0;
	
	protected byte[] directoryHash = new byte[0];

	protected boolean isDirectory;
	
	private File mainFile;

	protected boolean havePerUserExtensions;

	// list of paths on the TSI to look for extensions
	protected final List<ExtensionInfo>extensionInfo = new ArrayList<>();

	// app definitions keyed by user DN
	protected final Map<String,Collection<ApplicationInfo>>extensions = 
			new HashMap<>();
	
	// last refresh instant keyed by user DN
	protected final Map<String,Long>extensionsLastRefreshed = new HashMap<>();

	// list of per-user IDB extensions keyed by user DN
	protected final Map<String,List<ExtensionInfo>>resolvedExtensionsPerUser = new HashMap<>();

	// interval to update user-specific extensions (milliseconds)
	protected int extensionUpdateInterval;

	protected final XNJSProperties xnjsProperties;
	
	@Inject
	public IDBImpl(XNJSProperties xnjsProperties, TSIFactory tsiFactory) {
		this.tsiFactory = tsiFactory;
		this.xnjsProperties = xnjsProperties;
		setupIDBSources();
	}
	
	protected void setupIDBSources() {
		String idbSpec = xnjsProperties.getValue(XNJSProperties.IDBFILE);
		if(idbSpec==null) {
			throw new ConfigurationException("IDB component is used but property <XNJS.idbfile> is not set.");
		}
		
		idbFile=new File(idbSpec);
		
		isDirectory = idbFile.isDirectory();
		
		if(!idbFile.exists()) {
			throw new ConfigurationException("IDB location <XNJS.idbfile> must point to a valid file or directory.");
		}
		
		if(isDirectory){
			logger.info("Using IDB directory <{}>", idbFile.getAbsolutePath());
			String main = xnjsProperties.getValue("idbfile.main");
			if(main!=null){
				mainFile = new File(main);
				if(mainFile.exists()) {
					logger.info("Main IDB file <{}>", mainFile.getAbsolutePath());
				}
				else {
					throw new ConfigurationException("IDB main file location <XNJS.idbfile.main> must point to a valid file.");
				}
			}
		}
		else{
			logger.info("Using IDB file <"+idbFile.getAbsolutePath()+">");
		}

		// load user extensions
		int i=1;
		while(true){
			String ext = xnjsProperties.getValue("idbfile.ext."+i);
			if(ext == null || ext.equals(idbSpec))break;
			logger.info("Will read user-specific applications from <{}>", ext);
			extensionInfo.add(new ExtensionInfo(ext));
			i++;
		}
		havePerUserExtensions = extensionInfo.size()>0;
		if(havePerUserExtensions){
			// get update interval in seconds
			String update = xnjsProperties.getValue("XNJS.idbfile.ext.updateInterval");
			if(update==null)update = "300";
			extensionUpdateInterval = 1000*Integer.parseInt(update);
		}
	}
	
	public File getMainFile() {
		return mainFile;
	}
	
	protected void clear(){
		getIdb().clear();
		partitions.clear();
		filespaces.clear();
		textInfoProperties.clear();
	}

	@Override
	public List<Partition> getPartitions() throws ExecutionException{
		doCheckAndUpdateIDB();
		return Collections.unmodifiableList(partitions);
	}
	
	public List<Partition> getPartitionsInternal(){
		return partitions;
	}
	

	@Override
	public Collection<ApplicationInfo> getApplications(Client client) {
		doCheckAndUpdateIDB();
		Collection<ApplicationInfo>apps = new HashSet<>();
		apps.addAll(getIdb());
		if(havePerUserExtensions && client!=null && client.getXlogin()!=null && client.getXlogin().getUserName()!=null){
			apps.addAll(getPerUserApps(client));
		}
		return apps;
	}

	public ApplicationInfo getApplication(String name, String version, Client client){
		for(ApplicationInfo app: getApplications(client)){
			if(!name.equals(app.getName()))continue;
			if(version==null || app.getVersion().equals(version))return app;
		}
		return null;
	}

	protected synchronized Collection<ApplicationInfo> getPerUserApps(Client client){
		String dn = client.getDistinguishedName();
		Collection<ApplicationInfo> ext = extensions.get(dn);
		Long lastRefresh = extensionsLastRefreshed.get(dn);
		boolean mustRefresh = lastRefresh ==null || lastRefresh+extensionUpdateInterval<System.currentTimeMillis();
		if(ext == null || mustRefresh){
			extensionsLastRefreshed.put(dn, System.currentTimeMillis());
			ext = new HashSet<>();
			extensions.put(dn, ext);
			TSI tsi = tsiFactory.createTSI(client);
			List<ExtensionInfo> extensionsPerUser = resolvedExtensionsPerUser.get(dn);
			if(extensionsPerUser==null){
				extensionsPerUser = new ArrayList<>();
				for(ExtensionInfo e: extensionInfo){
					extensionsPerUser.add(new ExtensionInfo(e.path));
				}
				resolvedExtensionsPerUser.put(dn, extensionsPerUser);
			}
			for(ExtensionInfo extension: extensionsPerUser){
				try{
					if(extension.resolvedPath==null){
						extension.resolvedPath = tsi.resolve(extension.path);
					}
					String realPath = extension.resolvedPath;
					logger.info("Reading user-specific apps from <{}>", realPath);
					Collection<String>files = getFiles(realPath, client);
					for(String file : files){
						try(InputStream is = tsi.getInputStream(file)){
							readApplications(is, ext);
						}catch(Exception ex){
							Log.logException("Could not load apps from <"+file+">", ex, logger);
						}
					}
				}catch(Exception ex){
					Log.logException("Could not load apps from <"+extension.path+">", ex, logger);
				}
			}
		}
		return ext;
	}

	protected Collection<String> getFiles(String pathPattern, Client client) throws ExecutionException {
		Collection<String> results = new HashSet<>();
		if(SimpleFindOptions.isWildCard(pathPattern)){
			File path = new File(pathPattern);
			String base = path.getParent();
			String pattern = path.getName();
			TSI tsi= tsiFactory.createTSI(client);
			FileFilter options = SimpleFindOptions.stringMatch(pattern, false);
			XnjsFile[]files = tsi.find(base, options, 0, 1000);
			for(XnjsFile match : files){
				if(!match.isDirectory()){
					results.add(match.getPath());
				}
			}
		}
		else{
			results.add(pathPattern);
		}
		return results;
	}

	@Override
	public String getTextInfo(String name) {
		return getTextInfoProperties().get(name);
	}

	@Override
	public Map<String, String> getTextInfoProperties() {
		doCheckAndUpdateIDB();
		return textInfoProperties;
	}

	public Map<String, String> getTextInfoPropertiesNoUpdate() {
		return textInfoProperties;
	}
	
	public Map<String, String> getFilespaces() {
		return filespaces;
	}

	@Override
	public String getScriptHeader() {
		doCheckAndUpdateIDB();
		return scriptHeader!=null ? scriptHeader : DEFAULT_SCRIPT_HEADER;
	}

	public void setScriptHeader(String header) {
		if(header!=null && !header.endsWith("\n")) {
			header=header+"\n";
		}
		this.scriptHeader = header;
	}

	@Override
	public String getFilespace(String name){
		doCheckAndUpdateIDB();
		return filespaces.get(name);
	}
	
	@Override
	public String[] getFilesystemNames(){
		doCheckAndUpdateIDB();
		return filespaces.keySet().toArray(new String[filespaces.size()]);
	}

	public static OptionDescription parseArgument(String text){
		Matcher m=ARG_PATTERN.matcher(text);		
		if(m.matches()){
			OptionDescription arg = new OptionDescription();
			String name = m.group(3) == null ? m.group(4) : m.group(3);
			arg.setName(name);
			return arg;
		}
		else return null;
	}

	protected synchronized void doCheckAndUpdateIDB(){
		if(idbWasModified()) {//update first
			try {
				logger.info("IDB modified/touched, re-reading...");
				updateIDB();					
			} catch (Exception e) {
				logger.error("Problems updating IDB...",e);
			}
		}
	}

	@Override
	public Partition getPartition(String partition) throws ExecutionException {
		doCheckAndUpdateIDB();
		if(partition==null) {
			if(DEFAULT_PARTITION.equals(partition)){
				return getFirstPartition();
			}
			else {
				for(Partition p : partitions){
					if(p.isDefaultPartition())return p;
				}
				return getFirstPartition();
			}
		}
		for(Partition p : partitions){
			if(p.getName().equalsIgnoreCase(partition) || "*".equals(p.getName()))
			{
				return p;
			}
		}
		return null;
	}
	
	protected Partition getFirstPartition() {
		return partitions.size()>0 ? partitions.get(0) : null;
	}

	@Override
	public Resource getAllowedPartitions(Client c) throws ExecutionException {
		if(getPartitions().size()==1 && getPartitions().get(0).getName()=="*"){
			return new StringResource(ResourceSet.QUEUE, "*");
		}
		else{
			return getDefinedPartitions(c);
		}
	}

	protected ValueListResource getDefinedPartitions(Client c) throws ExecutionException {
		Set<String> allowed = new HashSet<>();
		String defaultQueue = null;
		try{
			for(Partition p: getPartitions()) {
				ValueListResource queues =  (ValueListResource)p.getResources().getResource(ResourceSet.QUEUE);
				if(queues!=null){
					allowed.addAll(Arrays.asList(queues.getValidValues()));
					defaultQueue = queues.getStringValue();
				}else {
					allowed.add(p.getName());
				}
					
			}
		}catch(Exception ex){}

		if (c != null){
			Queue q = c.getQueue();
			if (q!=null && q.getValidQueues().length != 0){
				allowed.clear();
				allowed.addAll(Arrays.asList(q.getValidQueues()));
				if(q.isSelectedQueueSet() || !allowed.contains(defaultQueue))
				{
					defaultQueue = q.getSelectedQueue();
				}
			}
		}
		if(defaultQueue!=null && !allowed.contains(defaultQueue))
		{
			throw new ExecutionException(new ErrorCode(ErrorCode.ERR_RESOURCE_OUT_OF_RANGE,"Requested queue <"+defaultQueue
					+"> is out of range (not allowed for this user)"));
		}
		List<String> values = new ArrayList<>();
		values.addAll(allowed);
		return new ValueListResource(ResourceSet.QUEUE, defaultQueue, values, Category.QUEUE);
	}

	private long lastDirectoryHash = 0;

	/**
	 * checks whether the IDB has changed since the last time it was read
	 */
	protected synchronized boolean idbWasModified(){
		if(mainFile!=null && mainFile.lastModified() > lastUpdate) {
			return true;
		}
		if(isDirectory && lastUpdate>0){
			if(lastDirectoryHash+10000>System.currentTimeMillis()){
				return false;
			}
		}
		boolean changed = false;
		if(isDirectory){
			byte[]hash = getDirectoryHash();
			if(directoryHash==null || !Arrays.equals(hash, directoryHash)){
				changed = true;
			}
		}
		else{
			changed = idbFile.lastModified() > lastUpdate;
		}
		return changed;
	}

	private void markUpdated() {
		lastUpdate = System.currentTimeMillis();
		if(isDirectory){
			directoryHash  = getDirectoryHash();
			lastDirectoryHash = System.currentTimeMillis();
		}
	}

	public long getLastUpdateTime(){
		return lastUpdate;
	}
	
	protected void updateIDB() throws Exception {
		synchronized(idb){
			markUpdated();
			clear();
			Collection<File>fileList = getFilesForReading();
			boolean singleFile = fileList.size()==1;
			for(File f: fileList) {
				handleFile(f, singleFile);
			}
		}
	}

	protected Set<File>getFilesForReading(){
		Set<File> fileList = new HashSet<>();
		if(idbFile.isDirectory()) {
			for(File f: idbFile.listFiles(onlyRegularFiles)){
				fileList.add(f.getAbsoluteFile());
			}
		}
		else {
			fileList.add(idbFile.getAbsoluteFile());
		}
		if(mainFile!=null){
			fileList.add(mainFile.getAbsoluteFile());
		}
		return fileList;
	}

	private static FilenameFilter onlyRegularFiles = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			return ! (name.endsWith("~") || name.startsWith(".") ) ;
		}
	};
	
	/**
	 * read stuff from IDB file (main file or apps)
	 * 
	 * @param file
	 * @param singleFile - true if this is the only file we have
	 * @throws Exception
	 */
	protected void handleFile(File file, boolean singleFile)throws Exception{
		IDBParser parser = getParser(file);
		parser.handleFile(file, singleFile);
	}
	
	public IDBParser getParser(File file) throws Exception {
		try (InputStream fis = new FileInputStream(file)){
			return getParser(fis);
		}
	}
	
	protected IDBParser getParser(InputStream source) throws Exception {
		String data = IOUtils.toString(source, "UTF-8");
		boolean json = false;
		try {
			new JSONObject(data);
			json = true;
		}catch(Exception ex) {}
		if(!json)throw new IllegalArgumentException("IDB is not in JSON format");
		return new JsonIDB(this, data);
	}
	
	/**
	 * read and parse applications from the given source and add them to the collection
	 * @param source
	 * @param idb
	 * @throws Exception
	 */
	protected void readApplications(InputStream source, Collection<ApplicationInfo> idb) throws Exception{
		IDBParser parser = getParser(source);
		parser.readApplications(idb);
	}

	private byte[] getDirectoryHash(){
		try{
			MessageDigest md=MessageDigest.getInstance("MD5");
			for(File f: idbFile.listFiles()){
				computeDirHash(md, f);
			}
			return md.digest();
		}catch(Exception ex){
			logger.warn("Error checking for IDB modification",ex);
		}
		return new byte[0];
	}

	private void computeDirHash(MessageDigest md, File file){
		if(file.isDirectory()){
			for(File f: file.listFiles()){
				computeDirHash(md, f);
			}
		}
		else{
			md.update(Longs.toByteArray(file.lastModified()));
		}
	}

	/**
	 * holds user-specific extension path plus some meta info
	 */
	public static class ExtensionInfo {
		public String path;
		public String resolvedPath;
		public ExtensionInfo(String path){
			this.path=path;
		}
	}

	public Collection<ApplicationInfo> getIdb() {
		return idb;
	}
	
}
