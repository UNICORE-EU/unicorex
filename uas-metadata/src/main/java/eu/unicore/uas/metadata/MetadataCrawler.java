/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 *
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * DISCLAIMER
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/
package eu.unicore.uas.metadata;

import java.io.IOException;
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
import org.xml.sax.SAXException;

import de.fzj.unicore.uas.metadata.ExtractionStatistics;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.uas.util.Pair;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import de.fzj.unicore.xnjs.io.XnjsFile;
import eu.unicore.client.data.Metadata.CrawlerControl;
import eu.unicore.services.Kernel;
import eu.unicore.uas.metadata.MetadataFile.MD_State;
import eu.unicore.util.Log;

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
	 * @param metadataManager reference to the metadata manager (for creating metadata)
	 * @param storage reference to the storage that should be crawled
	 * @param base directory to be crawled
	 * @param depthLimit depth of the crawling process
	 * 
	 * @throws InstantiationException thrown when parser instantiation fails
	 * @throws IllegalAccessException  thrown when parser instantiation fails
	 */
	public MetadataCrawler(LuceneMetadataManager metadataManager, IStorageAdapter storage, List<String> files, List<Pair<String,Integer>>dirs, Kernel kernel) 
	throws Exception {
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
			LOG.info("Extracting from "+files.size()+" files...");
			Map<String, MetadataFile.MD_State> statuses = new HashMap<String, MD_State>();
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
				LOG.debug("Committing updated index took "+(System.currentTimeMillis()-startCommit)+" ms.");
			}
		} catch (Exception ex) {
			LogUtil.logException("Error committing the metadata index.", ex, LOG);
		}
		
		long time = System.currentTimeMillis() - start;
		LOG.info("EXITING crawler, time " + time + " ms.");
		
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
		LOG.info("Entering directory " + fullBase+" crawling depth "+depthLimit);
		
		long start = System.currentTimeMillis();
		List<String> fileList = new ArrayList<String>();
		
		try {
			long startSingle=System.currentTimeMillis();
			getFiles(fullBase, fileList, 0, depthLimit, createBaseFilter(fullBase));
			if(LOG.isDebugEnabled()){
				LOG.debug("Getting file list (size "+fileList.size()+") took "+(System.currentTimeMillis()-startSingle)+" ms.");
			}
			startSingle=System.currentTimeMillis();
			Map<String, MD_State> list = statusCheck(fileList);
			if(LOG.isDebugEnabled()){
				LOG.debug("Checking file stati took "+(System.currentTimeMillis()-startSingle)+" ms.");
			}
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

		Map<String, MetadataFile.MD_State> statuses = new HashMap<String, MD_State>();

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

	private Map<String, String> extractMetadata(String file) throws ExecutionException, IOException, SAXException, TikaException {
		Map<String, String> ret = new HashMap<String, String>();
		Metadata meta = new Metadata();
		meta.add(LuceneIndexer.RESOURCE_NAME_KEY, file);
		InputStream is = storage.getInputStream(file);
		parser.parse(is, handler, meta, parseContext);
		is.close();
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
				LOG.info("Found crawler control file "+f.getPath());
				Properties p=new Properties();
				InputStream is=storage.getInputStream(f.getPath());
				try{
					p.load(is);
				}finally{
					is.close();
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
