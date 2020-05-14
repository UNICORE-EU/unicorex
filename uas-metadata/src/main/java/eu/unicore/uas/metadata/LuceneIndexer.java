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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;
import org.apache.tika.metadata.Metadata;

import de.fzj.unicore.uas.metadata.SearchResult;
import de.fzj.unicore.uas.util.LogUtil;

/**
 * Indexes the metadata and provides a search interface.
 *
 * <p>
 * The location of the Lucene index is determined by the config parameter
 * LuceneIndexer.LUCENE_INDEX_DIR. If the directory does not exists it will be created
 * otherwise an index from the directory will be opened and extended.
 * </p>
 *
 * <p>
 * In typical use case @see{LuceneMetadaManager} indexed are metadata files 
 * (their content) and not the content of the files.
 * </p>
 *
 *
 * TODO:
 * check functionality
 * work with corrupt index exception? should we recover?
 * 
 * @author w.noor
 * @author jrybicki
 * @author schuller
 * 
 */
public class LuceneIndexer {

	private static final Logger LOG = LogUtil.getLogger(LogUtil.DATA, LuceneIndexer.class);

	/**
	 * Content key
	 */
	public static final String CONTENT_KEY = "contents";
	//default search key:
	private static final String SEARCH_KEY = CONTENT_KEY;
	private final String dataDirectory;
	private final Directory directory;

	private static final Version VER = Version.LUCENE_34;
	private final QueryParser parser = new QueryParser(VER, SEARCH_KEY, new StandardAnalyzer(VER));
	private final IndexWriter indexWriter;

	private final static Map<String, LuceneIndexer>indexers=new HashMap<String, LuceneIndexer>();

	/**
	 * @param id - the id of the indexer, usually equal to the storage UUID
	 * @param indexDir - the base directory for the indexes
	 */
	public static synchronized LuceneIndexer get(String id, String indexDir){
		LuceneIndexer indexer=indexers.get(id);
		if(indexer==null){
			indexer=new LuceneIndexer(indexDir + id);
			indexers.put(id,indexer);
		}
		return indexer;
	}

	/**
	 * Initializes Indexer with index in given path
	 *
	 * @param indexLocation
	 */
	public LuceneIndexer(String indexLocation) {
		dataDirectory = indexLocation;
		try {
			directory = initalizeDataDirectory();
			indexWriter = initializeIndex();
		} catch (IOException ex) {
			throw new IllegalArgumentException(String.format("Unable to initialize Lucene index in: %s", dataDirectory), ex);
		}
	}


	/**
	 * Adds resource with metadata and contents to the index.
	 * Old metadata for the resource (if exists) will be removed/overwritten.
	 *
	 * TODO: multiple files with the same resourceName?
	 * 
	 * @param resourceName
	 * @param metadata
	 * @param contents
	 * @throws IOException
	 */
	public void createMetadata(String resourceName, Map<String, String> metadata, String contents) throws IOException {
		Document document = createMetadataDocument(metadata, resourceName, contents);
		indexWriter.deleteDocuments(new Term(Metadata.RESOURCE_NAME_KEY, resourceName));
		indexWriter.addDocument(document);
	}

	/**
	 * remove metadata for the given resource
	 * @param resourceName
	 * @throws IOException
	 */
	public void removeMetadata(final String resourceName) throws IOException {
		indexWriter.deleteDocuments(new Term(Metadata.RESOURCE_NAME_KEY, resourceName));
	}

	/**
	 * Update metadata for existing resource
	 *
	 * Adds provided metadata to existing metadata or creates new metadata if
	 * resource is not already indexed.
	 *
	 * @param metadata
	 * @param resourceName
	 * @param contents
	 * @throws IOException
	 */
	public void updateMetadata(String resourceName, Map<String, String> metadata, String contents) throws IOException {
		Document doc = getDocument(resourceName);
		Map<String, String> mergeMetadata; // = new HashMap<String, String>();
		if (doc != null) {
			Map<String, String> oldMetadata = extractMetadataFromDocument(doc);
			mergeMetadata = LuceneMetadataManager.mergeMetadata(oldMetadata, metadata);
		} else {
			mergeMetadata = metadata;
		}
		createMetadata(resourceName, mergeMetadata, contents);
	}

	/**
	 * Moves the metadata from @code{source} to @code{target}. Target will be overwritten.
	 *<p>
	 * Lucene does not provide any API method to update the existing index. It is updated by getting the
	 * copy of document, making update, delete the old copy and insert the new copy.
	 *</p>
	 *
	 * @param source
	 * @param target
	 * @throws IOException  
	 */
	public void moveMetadata(String source, String target) throws IOException {
		Document doc = getDocument(source);
		if (doc == null) {
			throw new IllegalArgumentException("No metadata indexed for " + source + ": unable to move");
		}
		removeMetadata(source);
		createMetadata(target, extractMetadataFromDocument(doc), doc.get(CONTENT_KEY));
	}

	/**
	 * Simple single attribute search
	 *
	 * @param queryString
	 * @param maximalHits
	 * @return list of search results
	 * @throws IOException
	 */
	public List<SearchResult> search(String queryString, int maximalHits) throws IOException {
		try(IndexSearcher searcher = new IndexSearcher(directory)) {
			List<SearchResult> ret = new ArrayList<SearchResult>();
			Query query = parser.parse(queryString);
			TopDocs results = searcher.search(query, maximalHits);
			for (ScoreDoc scoreDoc : results.scoreDocs) {
				SearchResult result = new SearchResult();
				result.setResourceName(searcher.doc(scoreDoc.doc).get(Metadata.RESOURCE_NAME_KEY));
				ret.add(result);
			}
			return ret;
		} catch (ParseException pe) {
			throw new IOException(pe);
		}
	}

	/**
	 * Advanced multiattribute query
	 * 
	 * Search for @code{numberOfrecords} documents fulfilling the queries
	 *
	 * @param queryStrings
	 * @param numberOfrecords
	 * @return List of aggregated results
	 * @throws IOException  
	 */
	public List<SearchResult> search(String[] queryStrings, int numberOfrecords) throws IOException {
		List<SearchResult> lstMetadataFiles = new ArrayList<SearchResult>();

		for (String queryString : queryStrings) {
			List<SearchResult> partialResult = search(queryString, numberOfrecords);
			lstMetadataFiles.addAll(partialResult);
			//            logger.debug(String.format("Searching for [%s]: [%d] results found",queryString,partialResult.size()));
		}

		return lstMetadataFiles;
	}

	public void commit()throws IOException{
		indexWriter.commit();
	}

	/**
	 * Commit changes and optimize index
	 * <p>
	 * Should be called after adding a lot of documents.
	 * </p>
	 *
	 * @throws java.io.IOException
	 */
	public void optimizeIndex() throws IOException {
		indexWriter.commit();
		indexWriter.optimize();
	}

	/**
	 * delete the index
	 * @throws IOException
	 */
	public void deleteAll()throws IOException{
		indexWriter.deleteAll();
	}

	/**
	 * Returns Lucene document for given resource or @code{null} if the resource
	 * is not in index.
	 * 
	 * @param resourceName
	 * @return Document
	 * @throws IOException  if the search index cannot be initialized
	 */
	protected Document getDocument(final String resourceName) throws IOException {
		try(IndexSearcher searcher = new IndexSearcher(directory)) {
			Query query = new TermQuery(new Term(Metadata.RESOURCE_NAME_KEY, resourceName));
			//XXX: it might be possible that there are more documents for the resourceName... should we merge?
			TopDocs result = searcher.search(query, 1);
			//XXX: return empty or throw an exception?
			if (result.scoreDocs.length == 0) {
				return null;
			}
			return searcher.doc(result.scoreDocs[0].doc);
		}
	}

	/**
	 * Returns the number of documents currently indexed.
	 * 
	 * @return number of documents currently in index
	 * @throws IOException
	 */
	protected int getNumberOfDocuments() throws IOException {
		return indexWriter.numDocs();
	}

	private Directory initalizeDataDirectory() throws IOException {
		File chkDir = new File(dataDirectory);
		boolean mkdirs = true;
		if (!chkDir.exists()) {
			mkdirs = chkDir.mkdirs();
		}

		if (!mkdirs || !chkDir.isDirectory()) {
			throw new IOException(String.format("Unable to create/find: %s Lucene index directory", dataDirectory));
		}
		return new NIOFSDirectory(new File(dataDirectory));
	}

	private IndexWriter initializeIndex() throws IOException {
		
		// Sometimes a server crash may leave the lock file, so check and unlock if necessary
		// This is only called when creating the LuceneIndexer, and indexers are per-storage,
		// so it should be safe to forcibly unlock
		boolean locked = IndexWriter.isLocked(directory);
		if (locked) {
			IndexWriter.unlock(directory);
			locked = IndexWriter.isLocked(directory);
			LOG.debug(String.format("Directory %s locked status: %s", dataDirectory, String.valueOf(locked)));
		}
		IndexWriterConfig cfg = new IndexWriterConfig(VER, new StandardAnalyzer(VER));
		cfg.setWriteLockTimeout(10000);
		IndexWriter indexWriter=new IndexWriter(directory, cfg);
		//create index files (to avoid errors when searching is started before files are added)
		indexWriter.commit();
		LOG.info(String.format("Lucene index initialized in: %s", dataDirectory));
		return indexWriter;
	}

	/**
	 * Extracts metadata from Lucene.Document
	 *
	 * @param document
	 * @return metadata map
	 */
	protected static Map<String, String> extractMetadataFromDocument(final Document document) {
		if (document == null) {
			throw new IllegalArgumentException("Document for metadata extraction cannot be null");
		}
		Map<String, String> ret = new HashMap<String, String>();
		for (Object object : document.getFields()) {
			Field field = (Field) object;
			String name = field.name();
			String value = document.get(name);
			ret.put(name, value);
		}
		return ret;
	}

	/**
	 * Creates Lucene.Document for provided resource, metadata and content
	 *
	 * @param metadata
	 * @param resource
	 * @param contents
	 * @return Document to be inserted in index.
	 */
	protected static Document createMetadataDocument(Map<String, String> metadata, String resource, String contents) {
		if (metadata == null || metadata.isEmpty()) {
			throw new IllegalArgumentException("Metadata cannot be null or empty");
		}
		if (resource == null || resource.trim().isEmpty()) {
			throw new IllegalArgumentException("Resource name cannot be null or empty");
		}

		Document doc = new Document();
		for (Map.Entry<String, String> entry : metadata.entrySet()) {
			doc.add(new Field(entry.getKey(), entry.getValue(), Field.Store.YES,
					Field.Index.ANALYZED));
		}
		//it might be already in the metadata: update
		doc.removeField(Metadata.RESOURCE_NAME_KEY);
		doc.add(new Field(Metadata.RESOURCE_NAME_KEY, resource,
				Field.Store.YES, Field.Index.NOT_ANALYZED));


		if (contents != null && !contents.trim().isEmpty()) {
			doc.add(new Field(LuceneIndexer.CONTENT_KEY, contents, Field.Store.NO,
					Field.Index.ANALYZED));
		}

		return doc;
	}
}
