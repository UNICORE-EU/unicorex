package eu.unicore.uas.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import eu.unicore.security.Client;
import eu.unicore.services.ContainerProperties;
import eu.unicore.services.Kernel;
import eu.unicore.services.ThreadingServices;
import eu.unicore.uas.json.JSONUtil;
import eu.unicore.uas.util.LogUtil;
import eu.unicore.util.Pair;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.xnjs.io.XnjsFileWithACL;

/**
 * This class manage all core stuff for metadata management.
 *
 *<p>
 * This is an implementation of a metadata storage with indexer. The metadata for
 * resources are stored both in the storage (as separate files) and in a Lucene
 * index. 
 * </p>
 * 
 * <p>	
 * FIXMEs:
 * <ul>
 * <li>
 * one indexer per storage: The idea is to have one MetadataManager per Storage instance. However the
 * storage is set by normal setter (it is possible to change the storage during the
 * lifetime of a Manager).Perfect situation would be to have the indexer and storage as final variables.</li>
 * <li>caseSensitive (with regard to resource name)?</li>
 * <li>all public methods follow safe programming style</li>
 * <li>we might consider the allowance of empty metadata creation to enable search</li>
 * </ul>
 * </p>
 *

 * @author w.noor
 * @author jrybicki
 * @author schuller
 * @author Konstantine Muradov
 * 
 * @see de.fzj.unicore.uas.metadata.MetadataManager
 * @see de.fzj.unicore.uas.metadata.StorageMetadataManager
 */
public class LuceneMetadataManager implements StorageMetadataManager {

    /**
     * Default number of documents returned in the search process
     */
    public static final int DEFAULT_NUMBER_OF_MATCHES = 200;
    private static final Logger LOG = LogUtil.getLogger(LogUtil.DATA, LuceneMetadataManager.class);
    private IStorageAdapter storage;
    private LuceneIndexer indexer;
    private Kernel kernel;
    
    /**
     * Standard constructor.
     */
    public LuceneMetadataManager(Kernel kernel) {
        super();
        this.kernel=kernel;
    }

    @Override
    public void createMetadata(String resourceName, Map<String, String> lstMetadata) throws IOException {
    	isStorageReady();
        isProperResource(resourceName);
        isProperMetadata(lstMetadata);

        String fileName = MetadataFile.getMetadatafileName(resourceName);

        //for security reasons we copy and overwrite the resourceName
        Map<String, String> copy = new HashMap<>(lstMetadata);
        copy.put(LuceneIndexer.RESOURCE_NAME_KEY, resourceName);

        String metadata = JSONUtil.asJSON(copy).toString();
        try {
            writeData(fileName, metadata.toString());
            indexer.createMetadata(resourceName, copy, metadata);
            if(autoCommit)commit();
        } catch (IOException ex) {
            throw new IOException("Unable to create metadata for resource <" + resourceName + ">", ex);
        }
    }

    @Override
    public void updateMetadata(String resourceName, Map<String, String> lstMetadata) throws IOException {
    	isStorageReady();
        isProperResource(resourceName);
        isProperMetadata(lstMetadata);
        
        String fileName = MetadataFile.getMetadatafileName(resourceName);
        try {
            Map<String, String> original = getMetadataByName(resourceName);
            Map<String, String> merged = mergeMetadata(original, lstMetadata);
            merged.put(LuceneIndexer.RESOURCE_NAME_KEY, resourceName);
            String metadata = JSONUtil.asJSON(merged).toString();
            if(!lstMetadata.isEmpty()){
	            writeData(fileName, metadata);
            }
            indexer.updateMetadata(resourceName, merged, metadata);
            if(autoCommit)commit();
        } catch (IOException ex) {
            throw new IOException("Unable to update metadata for resource <" + resourceName + ">", ex);
        }
    }

    @Override
    public void removeMetadata(String resourceName) throws IOException {
        isStorageReady();
        if (!isProperResourceName(resourceName)) {
            throw new IllegalArgumentException("Resource <" + resourceName + " is not proper resource name");
        }
        //we need to cover a situation: resource removed and then md removed (afterwards)
        try {
            if (fileExists(MetadataFile.getMetadatafileName(resourceName))) {
                storage.rm(MetadataFile.getMetadatafileName(resourceName));
            }
            indexer.removeMetadata(resourceName);
            if(autoCommit)commit();
        } catch (Exception ex) {
            throw new IOException("Unable to remove metadata for <" + resourceName + ">", ex);
        }
    }

    @Override
    public void renameResource(String source, String target) throws IOException {
    	isStorageReady();
        isProperResource(source);
        if (!isProperResourceName(target)) {
            throw new IllegalArgumentException("Resource <" + target + "> is not a proper resource name");
        }

        String sourceFileName = MetadataFile.getMetadatafileName(source);
        String targetFileName = MetadataFile.getMetadatafileName(target);
        if (fileExists(targetFileName)) {
            try {
                storage.rm(targetFileName);
            } catch (ExecutionException ex) {
                throw new IllegalArgumentException("Target resource metadata file <" + targetFileName + "> exists and cannot be overwritten", ex);
            }
        }
        if (fileExists(sourceFileName)) {
        	try {
        		storage.rename(sourceFileName, targetFileName);
        		try {
        			indexer.moveMetadata(source, target);
        			if(autoCommit)commit();
        		}catch(Exception ex){}
        	} catch (Exception ex) {
        		throw new IOException(String.format("Unable to move metadata from %s to %s", source, target), ex);
        	}
        }
    }

    @Override
    public void copyResourceMetadata(String source, String target) throws IOException {
        isStorageReady();
        isProperResource(source);
        if (!isProperResourceName(target)) {
            throw new IllegalArgumentException("Resource <" + target + "> is not a proper resource name");
        }

        try {
            Map<String, String> metadata = getMetadataByName(source);
            //XXX: create (overwrite) or update?
            createMetadata(target, metadata);
        } catch (IOException ex) {
            throw new IOException(String.format("Unable to copy metadata from %s to %s", source, target), ex);
        }
    }

    @Override
    public Future<ExtractionStatistics> startAutoMetadataExtraction(final List<String>files, final List<Pair<String,Integer>>dirs) throws InstantiationException, IllegalAccessException {
        isStorageReady();
        try {
            MetadataCrawler metaCrawler = new MetadataCrawler(this, storage, files, dirs, kernel);
            Future<ExtractionStatistics> future= kernel.getContainerProperties().getThreadingServices().
                getExecutorService().submit(metaCrawler);
            return future;
        }catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public List<SearchResult> searchMetadataByContent(String searchString, boolean isAdvancedSearch) {
        isStorageReady();

        List<SearchResult> lstMetafiles = null;
        List<String> searchTokens = new ArrayList<String>();

        try {
            if (isAdvancedSearch) {
                searchTokens.add(searchString);
            } else {
                StringTokenizer strTokens = new StringTokenizer(searchString, " ");
                while (strTokens.hasMoreTokens()) {
                    searchTokens.add(strTokens.nextToken());
                }
            }
            lstMetafiles = indexer.search(searchTokens.toArray(new String[searchTokens.size() - 1]), DEFAULT_NUMBER_OF_MATCHES);
        } catch (Exception e) {
            LogUtil.logException("Error searching metadata: ", e, LOG);
        }
        return lstMetafiles;
    }
    
    @Override
	public Future<FederatedSearchResultCollection> federatedMetadataSearch(Client client, String searchString, List<String> storagesList, boolean isAdvanced)
			throws Exception {
    	FederatedSearchProvider searchProvider = new FederatedSearchProvider(kernel, client, searchString, storagesList);
    	ContainerProperties containerProperties =  kernel.getContainerProperties();
    	ThreadingServices threadServices = containerProperties.getThreadingServices();
    	ScheduledExecutorService executorService = threadServices.getScheduledExecutorService();
    	return executorService.schedule(searchProvider, 1, TimeUnit.SECONDS);
	}

    @Override
    public Map<String, String> getMetadataByName(final String resourceName) throws IOException {
        isStorageReady();
        isProperResource(resourceName);

        Map<String, String> metadata = new HashMap<>();

        String fileName = MetadataFile.getMetadatafileName(resourceName);
        if (!fileExists(fileName)) {
            //XXX: we might consider creating the file for the future.
            return metadata;
        }
        try {
            JSONObject o = new JSONObject(readFully(fileName));
            return JSONUtil.asMap(o);
        } catch (Exception ex) {
            throw new IOException("Unkown data format of metadata read from file: <" + fileName + "> ", ex);
        }
    }
    
    @Override
    public synchronized void setStorageAdapter(IStorageAdapter storage, String storageID) {
        if (storage == null) {
            throw new IllegalStateException("StorageAdapter cannot be null");
        }
        this.storage = storage;
        String id = storageID != null ? storageID : storage.getFileSystemIdentifier();
        MetadataProperties cfg = kernel.getAttribute(MetadataProperties.class);
        indexer = LuceneIndexer.get(id, cfg.getValue(MetadataProperties.LUCENE_INDEX_DIR));
    }

    public void commit()throws IOException{
    	indexer.commit();
    }
    
    private boolean autoCommit=true;

    public void setAutoCommit(boolean autocommit){
    	autoCommit=autocommit;
    }

    /**
     * Write data to file via storage
     *
     * @param fileName filename
     * @param metadata string
     * @throws IOException
     */
    protected void writeData(String fileName, String metadata) throws IOException {
        if (storage == null) {
            throw new IllegalStateException("Storage cannot be null");
        }
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be null nor empty");
        }
        if (metadata == null || metadata.length() == 0) {
            throw new IllegalArgumentException("Metadata cannot be null nor empty");
        }

        try(OutputStream os = storage.getOutputStream(fileName, false)){
            os.write(metadata.getBytes("UTF-8"));
        }
    }

    /**
     * Read file on storage into string
     *
     * @param fileName
     * @return string
     * @throws IOException
     */
    protected String readFully(String fileName) throws IOException {

        if (storage == null) {
            throw new IllegalStateException("Storage cannot be null");
        }
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null nor empty");
        }
        try (InputStream is = storage.getInputStream(fileName)){
            return IOUtils.toString(is, "UTF-8");
        }
    }

    /**
     * Merges two metadata maps. For common keys values from @code{oldData} are
     * replaced with values from @code{newData}
     *
     * TODO: fix the mutability?
     *
     * @param oldData 
     * @param newData
     * @return merged map of metadata
     */
    public static Map<String, String> mergeMetadata(Map<String, String> oldData, Map<String, String> newData) {
        if (oldData == null || newData == null) {
            throw new IllegalArgumentException("Metadata to merge cannot be null");
        }
        Map<String, String> map = new HashMap<String, String>();
        map.putAll(oldData);
        map.putAll(newData);
        return map;
    }

    private static boolean isProperResourceName(String resourceName) {
        if (resourceName == null || resourceName.trim().isEmpty()) {
            return false;
        }
        if (MetadataFile.isMetadataFileName(resourceName)) {
            return false;
        }
        return true;
    }

    /**
     * Check if the resource is a proper resource to be metadated
     * <p>
     * That includes: name cannot be empty or null, file must exist
     * 
     * @param resourceName name of the resource to be checked
     * @throws IllegalArgumentException when the name is not proper resource name
     * @return true if the check is ok
     */
    public boolean isProperResource(String resourceName) {
        if (resourceName == null || resourceName.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource name cannot be empty or null.");
        }

        if (MetadataFile.isMetadataFileName(resourceName)) {
            throw new IllegalArgumentException("Resource <" + resourceName + "> is not proper resource name (it is a metadata file)");
        }

        if (!fileExists(resourceName)) {
            throw new IllegalArgumentException("Resource <" + resourceName + "> does not exist");
        }
        return true;
    }

    private synchronized boolean isStorageReady() {
        if (storage == null || indexer == null) {
            throw new IllegalStateException("This metadata manager does not have storage and/or indexer. Unable to index resource");
        }
        return true;
    }

    private static void isProperMetadata(Map<String, String> lstMetadata) {
        if (lstMetadata == null){
            throw new IllegalArgumentException("Metadata cannot be null");
        }
    }

    private boolean fileExists(String resourceName) {
        try {
            //check if original file exists
            XnjsFileWithACL properties = storage.getProperties(resourceName);
            if (properties == null) {
                return false;
            }
        } catch (ExecutionException ex) {
            return false;
        }
        return true;
    }
}
