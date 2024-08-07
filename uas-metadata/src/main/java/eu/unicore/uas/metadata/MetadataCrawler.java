package eu.unicore.uas.metadata;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;

import eu.unicore.client.data.Metadata.CrawlerControl;
import eu.unicore.services.Kernel;
import eu.unicore.uas.metadata.MetadataFile.MD_State;
import eu.unicore.uas.util.LogUtil;
import eu.unicore.util.Log;
import eu.unicore.util.Pair;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.xnjs.io.XnjsFile;

/**
 * Crawlers through the file system and collect metadata for files
 *
 * @author w.noor
 * @author schuller
 * @author jrybicki
 */
public class MetadataCrawler implements Callable<ExtractionStatistics> {

	private static final Logger LOG = LogUtil.getLogger(LogUtil.DATA, MetadataCrawler.class);
	//how long the crawling waits when scheduled (in seconds)
	public static final long DEFAULTSCHEDULEDELAY = 1;

	public static final String CRAWLER_CONTROL_FILENAME = ".unicore_metadata_control";

	private final ContentHandler handler = new BodyContentHandler(-1);
	private final ParseContext parseContext = new ParseContext();
	private final Parser parser;

	private final LuceneMetadataManager metadataManager;
	private final IStorageAdapter storage;
	private final List<String> files;
	private final List<Pair<String,Integer>>dirs;

	/**
	 * Crawls through resources available in basepath (and subdirs to @code{dephLimit}) via storage
	 *<p>
	 * Found resources are indexed via metadataManager. Extraction of the metadata is done
	 * via parser (e.g. Apache Tika) defined in the config file.
	 * </p>
	 * 
	 * @param metadataManager
	 * @param storage 
	 * @oaram files - list of files to extract metadata from
	 * @param dirs - list of directories to crawl with depth limit
	 * @param kernel
	 */
	public MetadataCrawler(LuceneMetadataManager metadataManager, IStorageAdapter storage,
			List<String> files, List<Pair<String,Integer>>dirs, Kernel kernel) throws Exception {
		this.files = files;
		this.dirs = dirs;
		this.metadataManager = metadataManager;
		this.storage = storage;

		MetadataProperties cfg = kernel.getAttribute(MetadataProperties.class);
		Class<? extends Parser> parserClass = cfg.getClassValue(MetadataProperties.PARSER_CLASSNAME, 
				Parser.class);
		parser = parserClass.getConstructor().newInstance();
	}

	@Override
	public ExtractionStatistics call() {
		LOG.info("STARTING crawler.");
		long start = System.currentTimeMillis();
		ExtractionStatistics stats = new ExtractionStatistics();
		AtomicInteger docsProcessed = new AtomicInteger(0);
		metadataManager.setAutoCommit(false);
		if(dirs!=null){
			for(Pair<String, Integer> d:dirs){
				extractDir(d.getM1(), d.getM2(), docsProcessed);
			}
		}
		if(files!=null && files.size()>0){
			LOG.info("Extracting from {} files...", files.size());
			Map<String, MetadataFile.MD_State> statuses = new HashMap<>();
			for(String file: files){
				try{
					if(!MetadataFile.isMetadataFileName(file)){
						statuses.put(file, checkFileStatus(file));	
					}
				}catch(Exception e){
					
				}
			}
			try{
				process(statuses, docsProcessed);
			}catch(Exception ex){
				LogUtil.logException("Error while crawling the metadata", ex, LOG);
			}
		}

		try{
			LOG.info("Committing updated index...");
			long startCommit=System.currentTimeMillis();
			metadataManager.commit();
			metadataManager.setAutoCommit(true);
			if(LOG.isDebugEnabled()){
				LOG.debug("Committing updated index took {} ms.",(System.currentTimeMillis()-startCommit));
			}
		} catch (Exception ex) {
			LogUtil.logException("Error committing the metadata index.", ex, LOG);
		}
		
		long time = System.currentTimeMillis() - start;
		LOG.info("EXITING crawler, time {} ms.", time);
		
		stats.setDocumentsProcessed(docsProcessed.get());
		stats.setDurationMillis(time);
		return  stats;
	}
	
	/**
	 * Do the crawling process for a directory
	 * <p>
	 * Process:
	 * 1) extract a list of files from the storage
	 * 2) check status changes (file added, file removed, etc)
	 * 3) update md index 
	 */
	public void extractDir(String base, int depthLimit, AtomicInteger docsProcessed) {
		String fullBase = base;
		LOG.info("Entering directory {} crawling depth {}", fullBase, depthLimit);
		
		long start = System.currentTimeMillis();
		List<String> fileList = new ArrayList<String>();
		
		try {
			long startSingle=System.currentTimeMillis();
			getFiles(fullBase, fileList, 0, depthLimit, createBaseFilter(fullBase));
			LOG.debug("Getting file list (size {}) took {} ms.", fileList.size(), System.currentTimeMillis()-startSingle);
			startSingle=System.currentTimeMillis();
			Map<String, MD_State> list = statusCheck(fileList);
			LOG.debug("Checking file stati took {} ms.", System.currentTimeMillis()-startSingle);
			process(list, docsProcessed);
		} catch (Exception ex) {
			LogUtil.logException("Error while crawling the metadata", ex, LOG);
		}

		long time = System.currentTimeMillis() - start;
		LOG.info("Exiting directory " + fullBase + " time " + time + " ms.");
	}
	
	private void process(Map<String, MD_State> list, AtomicInteger docsProcessed) throws Exception {
		
		for (Map.Entry<String, MD_State> entry : list.entrySet()) {
			long startSingle=System.currentTimeMillis();
			String file=entry.getKey();

			switch (entry.getValue()) {
			case CHK_CONSISTENCE:
				//there is already a md file, update md
				//XXX: we might also use tika here (complementary)
				Map<String, String> metadata = Collections.emptyMap();
				metadataManager.updateMetadata(file, metadata);
				LOG.info("Updated index for <{}> took {} ms.", file, System.currentTimeMillis()-startSingle);
				break;
			case NEW:
				//it is new and no user metadata was created-> try extract
				try{
					Map<String, String> extracted = extractMetadata(file);
					metadataManager.createMetadata(file, extracted);
					LOG.debug("Extracted metadata for <{}> in {} ms.", file, System.currentTimeMillis()-startSingle);
				}catch(TikaException te){
					LogUtil.logException("Error while extracting metadata for <"+file+">", te, LOG);
				}
				break;

			case RESOURCE_DELETED:
				metadataManager.removeMetadata(file);
				break;
			default:
				//ignore for now?
				//throw new IllegalArgumentException("State: "+resourceState+" is unknown");
			}
			docsProcessed.incrementAndGet();
		}
	}
	
	/**
	 * Filters list of files to detect:
	 * -removal of resource files without removal of metadata files (md sould be removed)
	 * -creation of resource files without metadata files (resource should be indexed)
	 * -updates in metadata file (index should be updated)
	 * 
	 * FIXME:
	 * -it might improve the performance to remove resource from file list when
	 * resource.md file was found (and other way round). In some cases 2 speed-up
	 * but concurrent modification exception.
	 * -possible solution would be to have a callback in the listFiles method 
	 * where such cases could be caught... further refactoring.
	 * 
	 * @param files
	 * @return map of resource names with respective 
	 */
	protected static Map<String, MetadataFile.MD_State> statusCheck(List<String> files) {

		Map<String, MetadataFile.MD_State> statuses = new HashMap<>();

		for (String file : files) {
			String resource = null;
			if (MetadataFile.isMetadataFileName(file)) {
				resource = MetadataFile.getResourceName(file);
				if (statuses.containsKey(resource)) {
					//overwrite NEW with CHK
					statuses.put( resource, MD_State.CHK_CONSISTENCE);
				} else {
					statuses.put(resource, MD_State.RESOURCE_DELETED);
				}

			} else {
				resource = file;
				if (statuses.containsKey(resource)) {
					//overwrite DELETED with CHK
					statuses.put(resource, MD_State.CHK_CONSISTENCE);
				} else {
					statuses.put(resource, MD_State.NEW);
				}
			}
		}
		return statuses;
	}
	
	/**
	 * check the status for a single resource
	 * @param file
	 * @return status (to be updated or new)
	 */
	protected MetadataFile.MD_State checkFileStatus(String file) throws ExecutionException {
		XnjsFile md=storage.getProperties(MetadataFile.getMetadatafileName(file));
		return md==null ? MD_State.NEW : MD_State.CHK_CONSISTENCE;
	}

	private void getFiles(String directoryName, List<String> list, int level, int limit, NameFilter nameFilter) throws ExecutionException {
		level++;

		if (level > limit) {
			return;
		}

		XnjsFile x = storage.getProperties(directoryName);
		if (x != null){
			if(x.isDirectory()) {
				XnjsFile[] gridFiles = storage.ls(directoryName);
				for (int j = 0; j < gridFiles.length; j++) {
					XnjsFile x2 = gridFiles[j];
					String name=x2.getPath();
					if(nameFilter==null || nameFilter.accept(name)){
						LOG.debug("Include: {}", name);
						if (x2.isDirectory()) {
							getFiles(name, list, level, limit, createChildFilter(nameFilter));
						} else {
							list.add(name);
						}
					}
					else LOG.debug("Exclude: {}", name);
				}
			}
			else{
				//single file
				String resource=x.getPath();
				list.add(resource);
				XnjsFile md=storage.getProperties(MetadataFile.getMetadatafileName(resource));
				if(md!=null){
					list.add(md.getPath());
				}
			}
		}

		level--;
	}

	private Map<String, String> extractMetadata(String file) throws Exception {
		Map<String, String> ret = new HashMap<>();
		Metadata meta = new Metadata();
		meta.add(LuceneIndexer.RESOURCE_NAME_KEY, file);
		try(InputStream is = storage.getInputStream(file)){
			parser.parse(is, handler, meta, parseContext);
		}
		for (String key : meta.names()) {
			ret.put(key, meta.get(key));
		}
		return ret;
	}
	
	
	/**
	 * create a NameFilter which decides whether a certain file should be 
	 * metadata-extracted or not. This checks if a file named 
	 * ".unicore_metadata_control" exists in the directory and reads it
	 * 
	 * @param baseDirectory - the base directory where the crawler starts crawling
	 * @return
	 */
	public NameFilter createBaseFilter(String baseDirectory){
		try{
			XnjsFile f=storage.getProperties(CRAWLER_CONTROL_FILENAME);
			if(f!=null){
				LOG.info("Found crawler control file {}", f.getPath());
				Properties p=new Properties();
				try(InputStream is=storage.getInputStream(f.getPath())){
					p.load(is);
				}
				CrawlerControl cc = CrawlerControl.create(p);
				NameFilter i=cc.getIncludes()!=null?new PatternFilter(cc.getIncludes()):defaultIncludes;
				NameFilter e=defaultExcludes;
				if(cc.getExcludes()!=null){
					e=new PatternFilter(cc.getExcludes());
					if(cc.isUseDefaultExcludes()){
						e=new ChainedFilter(e, defaultExcludes); 
					}
				}
				
				return new CombinedFilter(i, e);
			}
		}
		catch(Exception ex){
			String msg=Log.createFaultMessage("Cannot create crawler include/exclude filter", ex);
			LOG.info(msg);
		}
		
		//return default filter
		return new CombinedFilter(defaultIncludes, defaultExcludes);
	}
	
	/**
	 * create a NameFilter which decides whether a certain file should be 
	 * metadata-extracted or not
	 * 
	 * @param parent - the namefilter valid for the parent directory
	 * @return
	 */
	public NameFilter createChildFilter(NameFilter parent){
		return parent;
	}
	
	public static interface NameFilter {
		/**
		 * @param name - non null file/directory name
		 * @return <code>true</code> if this filter accepts the file
		 */
		public boolean accept(String name);
	}
	
	//by default we crawl every file
	private static NameFilter defaultIncludes=new NameFilter(){
		public boolean accept(String name){
			return true;
		}
	};
	
	//... but not these
	private static NameFilter defaultExcludes=new NameFilter(){
		public boolean accept(String name){
			return name.endsWith(".svn") || 
				   name.endsWith(CRAWLER_CONTROL_FILENAME) ||
				   name.endsWith(".unicore_rft.parts")
				   ;
		}
	};
	
	static class PatternFilter implements NameFilter{
		
		private final Pattern[] patterns;
		
		public PatternFilter(String... patterns){
			this.patterns=makePatterns(patterns);
		}

		//accept files that match patterns
		public boolean accept(String name){
			for(Pattern p: patterns){
				if(p.matcher(name).find())return true;
			}
			return false;
		}
		
		private Pattern[] makePatterns(String[]patterns){
			Pattern[] result=new Pattern[patterns.length];
			for(int i=0; i<patterns.length; i++){
				result[i]=Pattern.compile(patterns[i].replace(".","\\.")
						.replace("*", ".*").replace("?", "."));
			}
			return result;
		}
		
	}
	
	static class ChainedFilter implements NameFilter{
		
		private final NameFilter n1;
		private final NameFilter n2;
		
		public ChainedFilter(NameFilter n1, NameFilter n2){
			this.n1=n1;
			this.n2=n2;
		}
		
		//accept included files that are not excluded
		public boolean accept(String name){
			return n1.accept(name) || n2.accept(name);
		}
	}
	
	// combines include and exclude filters
	static class CombinedFilter implements NameFilter{
		
		private final NameFilter include;
		
		private final NameFilter exclude;
		
		public CombinedFilter(NameFilter include, NameFilter exclude){
			this.include=include;
			this.exclude=exclude;
		}
		
		//accept included files that are not excluded
		public boolean accept(String name){
			return include.accept(name) && !exclude.accept(name);
		}
	}

}
