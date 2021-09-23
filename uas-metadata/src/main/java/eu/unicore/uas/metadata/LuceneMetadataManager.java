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

import java.io.ByteArrayOutputStream;
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

import org.apache.logging.log4j.Logger;
import org.apache.tika.metadata.Metadata;

import de.fzj.unicore.uas.metadata.ExtractionStatistics;
import de.fzj.unicore.uas.metadata.FederatedSearchResultCollection;
import de.fzj.unicore.uas.metadata.SearchResult;
import de.fzj.unicore.uas.metadata.StorageMetadataManager;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.uas.util.Pair;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import de.fzj.unicore.xnjs.io.XnjsFileWithACL;
import eu.unicore.security.Client;
import eu.unicore.services.ContainerProperties;
import eu.unicore.services.Kernel;
import eu.unicore.services.ThreadingServices;
import eu.unicore.uas.metadata.utils.JSONAdapter;

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
    private static final int BUFFERSIZE = 1024;
    

    private static final Logger LOG = LogUtil.getLogger(LogUtil.DATA, LuceneMetadataManager.class);
    private static final JSONAdapter FORMATER = new JSONAdapter();
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


    /*
     * Here come the methods from de.fzj.unicore.uas.metadata.MetadataManager
     */
    @Override
    public void createMetadata(String resourceName, Map<String, String> lstMetadata) throws IOException {
    	isStorageReady();
        isProperResource(resourceName);
        isProperMetadata(lstMetadata);

        String fileName = MetadataFile.getMetadatafileName(resourceName);

        //for security reasons we copy and overwrite the resourceName
        Map<String, String> copy = new HashMap<>(lstMetadata);
        copy.put(Metadata.RESOURCE_NAME_KEY, resourceName);

        byte[] metadata = FORMATER.convert(copy);
        try {
            writeData(fileName, metadata);
            indexer.createMetadata(resourceName, copy, new String(metadata));
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
            merged.put(Metadata.RESOURCE_NAME_KEY, resourceName);
            byte[] metadata = FORMATER.convert(merged);
            if(!lstMetadata.isEmpty()){
	            writeData(fileName, metadata);
            }
            indexer.updateMetadata(resourceName, merged, new String(metadata));
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

        byte[] data = readFully(fileName);
        try {
            metadata = FORMATER.convert(data);
        } catch (Exception ex) {
            throw new IOException("Unkown data format of metadata read from file: <" + fileName + "> ", ex);
        }
        return metadata;
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
     * Write data to a grid file via storage
     *
     * @param fileName filename
     * @param metadata byte array with metadata
     * @throws IOException
     */
    protected void writeData(String fileName, byte[] metadata) throws IOException {
        OutputStream os;
        if (storage == null) {
            throw new IllegalStateException("Storage cannot be null");
        }
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be null nor empty");
        }
        if (metadata == null || metadata.length == 0) {
            throw new IllegalArgumentException("Metadata cannot be null nor empty");
        }

        try {
            //    if (storage.getProperties(fileName).isDirectory()) throw new IllegalArgumentException(String.format("Write target %s is a directory",fileName));
            os = storage.getOutputStream(fileName, false);
            os.write(metadata);
            os.flush();
            os.close();
        } catch (ExecutionException ex) {
            throw new IOException("Unable to write metadata", ex);
        }
    }

    /**
     * Read grid file via storage into a byte array
     *
     * @param fileName
     * @return byte [] array 
     * @throws IOException
     */
    protected byte[] readFully(String fileName) throws IOException {

        if (storage == null) {
            throw new IllegalStateException("Storage cannot be null");
        }
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null nor empty");
        }

        InputStream is = null;
        try {
            is = storage.getInputStream(fileName);
        } catch (ExecutionException ex) {
            throw new IOException(String.format("Unable to open file %s to read metadata", fileName), ex);
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[BUFFERSIZE];
        int r = 0;
        while (true) {
            r = is.read(buf);
            if (r < 0) {
                break;
            }
            bos.write(buf, 0, r);
        }
        is.close();
        return bos.toByteArray();
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
