package de.fzj.unicore.uas.metadata;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import eu.unicore.security.Client;
import eu.unicore.util.Pair;


/**
 * basic metadata interface, used to perform metadata operations on file resources
 * 
 * @author Waqas Noor
 * @author schuller
 * @author Konstantine Muradov
 */
public interface MetadataManager {
	
    /**
     * Create metadata on given resource
     * 
     * @param resourceName	
     * @param metadata	- metadata key-value pairs
     * @throws IOException
     */
    public void createMetadata(String resourceName, Map<String, String> metadata) throws IOException;

    /**
     * This method updates the metadata
     * 
     * @param resourceName The resource who has the metadata
     * @param metadata - metadata key-value pairs
     * @throws Exception
     */
    public void updateMetadata(String resourceName, Map<String, String> metadata) throws Exception;

    /**
     * This method searches the index using the provided search string
     * 
     * @param searchString search string space is used as delimeter.
     * @param isAdvancedSearch If advance search is true then query is translated to boolean/range query.
     */
    public List<SearchResult> searchMetadataByContent(String searchString, boolean isAdvancedSearch) throws Exception;
    
    /**
     * Perform an asynchronous search on multiple storages
     * 
     * @param client  - the Client to work as 
     * @param searchString - search string
     * @param storagesList - list of storages
     * @param isAdvanced - true if it is an advanced search
     * @return a Future for monitoring the search and collecting the results
     */
    public Future<FederatedSearchResultCollection> federatedMetadataSearch(Client client, String searchString, List<String> storagesList, boolean isAdvanced) throws Exception;
    
    /**
     * This method retrieves the metadata
     * 
     * @param resourceName
     * @return metadata key-value pairs
     */
    public Map<String, String> getMetadataByName(String resourceName) throws Exception;

    /**
     * This method removes the metadata from both the index and the storage
     * 
     * @param resourceName
     */
    public void removeMetadata(String resourceName) throws Exception;

    /**
     * rename a resource
     * 
     * @param source
     * @param target
     */
    public void renameResource(String source, String target) throws Exception;

    /**
     * copy metadata from source to target resource
     * 
     * @param source
     * @param target
     */
    public void copyResourceMetadata(String source, String target) throws Exception;

    /**
     * This method asynchronously crawls the file system and create the metadata.
     * @param files - list of single files 
     * @param directories - list of directories given as base : the path where crawler starts it job plus depthLimit: The number of level, crawler do its job. It should be greater than 0.
     * @return a {@link Future} for monitoring execution and potentially canceling, or <code>null</code> if
     * this functionality is not provided by the underlying code
     */
    public Future<ExtractionStatistics> startAutoMetadataExtraction(List<String>files, List<Pair<String, Integer>> directories) throws Exception;

}
