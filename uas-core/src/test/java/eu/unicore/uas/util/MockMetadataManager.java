package eu.unicore.uas.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.bouncycastle.util.encoders.Base64;

import eu.unicore.client.data.Metadata;
import eu.unicore.security.Client;
import eu.unicore.uas.metadata.ExtractionStatistics;
import eu.unicore.uas.metadata.FederatedSearchResult;
import eu.unicore.uas.metadata.FederatedSearchResultCollection;
import eu.unicore.uas.metadata.SearchResult;
import eu.unicore.uas.metadata.StorageMetadataManager;
import eu.unicore.util.Pair;
import eu.unicore.xnjs.io.IStorageAdapter;

public class MockMetadataManager implements StorageMetadataManager{

	private IStorageAdapter storage;

	private String basePath;

	private static final Map<String,Map<String,String>>meta=new HashMap<String,Map<String,String>>();

	public MockMetadataManager(){

	}

	public void setStorageAdapter(IStorageAdapter storage, String uniqueID) {
		this.storage = storage;
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	public String getBasePath() {
		return basePath;
	}

	@Override
	public void copyResourceMetadata(String source, String target) {
		Map<String,String>c=meta.get(source);
		if(c!=null)meta.put(target, c);
	}

	@Override
	public void createMetadata(String resourceName, Map<String,String>metadata) throws IOException {
		//auto-generate md5
		try{
			if(storage.getProperties(resourceName)!=null){
				InputStream is=storage.getInputStream(resourceName);
				try{
					String md5=computeMD5(is);
					metadata.put(Metadata.CONTENT_MD5,md5);
				}
				finally{
					try{
						is.close();
					}catch(Exception e){}
				}
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}

		meta.put(resourceName, metadata);
	}

	@Override
	public Map<String,String> getMetadataByName(String resourceName) {
		Map<String,String>result=meta.get(resourceName);
		if(result==null)result=new HashMap<String,String>();
		return result;
	}

	@Override
	public void removeMetadata(String resourceName)
	throws Exception {
		meta.remove(resourceName);
	}

	@Override
	public void renameResource(String source, String target) {
		Map<String,String> r=meta.get(source);
		meta.remove(source);
		meta.put(target,r);
	}

	@Override
	public List<SearchResult> searchMetadataByContent(String searchString,
			boolean isAdvancedSearch) {
		List<SearchResult>result=new ArrayList<SearchResult>();
		for(Map.Entry<String,Map<String,String>> e: meta.entrySet()) {
			if(e.getValue().toString().contains(searchString)) {
				SearchResult r=new SearchResult();
				r.setResourceName(e.getKey());
				result.add(r);
			}
		}
		return result;
	}
	
	@Override
	 public Future<FederatedSearchResultCollection> federatedMetadataSearch(Client client, String searchString, List<String> storagesList, boolean isAdvanced)
	 {
		Future<FederatedSearchResultCollection> result = new Future<FederatedSearchResultCollection>() {
			
			@Override
			public boolean isDone() {
				return true;
			}
			
			@Override
			public boolean isCancelled() {
				return false;
			}
			
			@Override
			public FederatedSearchResultCollection get(long timeout, TimeUnit unit)
					throws InterruptedException, ExecutionException, TimeoutException {
				return get();
			}
			
			@Override
			public FederatedSearchResultCollection get() throws InterruptedException,
					ExecutionException {

				FederatedSearchResultCollection collection = new FederatedSearchResultCollection();
				
				Date endDate = new Date();

				
				
				FederatedSearchResult sre = new FederatedSearchResult();
				
				sre.addResourceURL("https://somestorage/files/some%20resource%201");
				sre.addResourceURL("https://somestorage/files/some%20resource%202");
				sre.addResourceURL("https://somestorage/files/some%20resource%203");
				sre.addResourceURL("https://somestorage/files/some%20resource%204");
				
				collection.addSearchResult(sre);
				
				collection.setSearchEndTime(endDate);
				
				return collection;
			}
			
			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				// TODO Auto-generated method stub
				return false;
			}
		};
    	

		return result; // temporary
	 } 

	@Override
	public Future<ExtractionStatistics> startAutoMetadataExtraction(final List<String>files, final List<Pair<String,Integer>>dirs)
	throws Exception {
		return new Future<ExtractionStatistics>(){
			
			final long start=System.currentTimeMillis();
			final long duration=3000;
			
			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				return false;
			}

			@Override
			public boolean isCancelled() {
				return false;
			}

			@Override
			public boolean isDone() {
				return System.currentTimeMillis()>start+duration;
			}

			@Override
			public ExtractionStatistics get() throws InterruptedException,
					ExecutionException {
				while(!isDone())Thread.sleep(1000);
				ExtractionStatistics res=new ExtractionStatistics();
				res.setDurationMillis(duration);
				res.setDocumentsProcessed(files.size()+dirs.size());
				return res;
			}

			@Override
			public ExtractionStatistics get(long timeout, TimeUnit unit)
					throws InterruptedException, ExecutionException,
					TimeoutException {
				return get();
			}
		};
	}

	@Override
	public void updateMetadata(String resourceName, Map<String,String>metadata) throws Exception {
		meta.put(resourceName, metadata);
	}

	public static String computeMD5(InputStream is)throws Exception{
		MessageDigest md=MessageDigest.getInstance("MD5");
		byte[]buf=new byte[1024];
		int read=0;
		while(read>-1){
			read=is.read(buf);
			if(read<0)break;
			md.update(buf,0,read);
		}
		return new String(Base64.encode(md.digest()));
	}

}
