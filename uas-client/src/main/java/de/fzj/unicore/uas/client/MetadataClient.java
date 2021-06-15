package de.fzj.unicore.uas.client;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.unigrids.services.atomic.types.MetadataType;
import org.unigrids.services.atomic.types.ProtocolType;
import org.unigrids.services.atomic.types.TextInfoType;
import org.unigrids.x2006.x04.services.metadata.CreateMetadataDocument;
import org.unigrids.x2006.x04.services.metadata.CreateMetadataResponseDocument;
import org.unigrids.x2006.x04.services.metadata.DeleteMetadataDocument;
import org.unigrids.x2006.x04.services.metadata.DeleteMetadataResponseDocument;
import org.unigrids.x2006.x04.services.metadata.DirectoryDocument.Directory;
import org.unigrids.x2006.x04.services.metadata.FederatedMetadataSearchDocument;
import org.unigrids.x2006.x04.services.metadata.FederatedMetadataSearchResponseDocument;
import org.unigrids.x2006.x04.services.metadata.GetMetadataDocument;
import org.unigrids.x2006.x04.services.metadata.GetMetadataResponseDocument;
import org.unigrids.x2006.x04.services.metadata.SearchMetadataDocument;
import org.unigrids.x2006.x04.services.metadata.SearchMetadataResponseDocument;
import org.unigrids.x2006.x04.services.metadata.SearchResultDocument.SearchResult;
import org.unigrids.x2006.x04.services.metadata.StartMetadataExtractionDocument;
import org.unigrids.x2006.x04.services.metadata.StartMetadataExtractionDocument.StartMetadataExtraction;
import org.unigrids.x2006.x04.services.metadata.StartMetadataExtractionResponseDocument;
import org.unigrids.x2006.x04.services.metadata.UpdateMetadataDocument;
import org.unigrids.x2006.x04.services.metadata.UpdateMetadataResponseDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.MetadataManagement;
import de.fzj.unicore.uas.util.Pair;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * client for managing metadata, connecting to a single {@link MetadataManagement} service
 *
 * @author schuller
 */
public class MetadataClient extends BaseUASClient {

	private final MetadataManagement metadataManagement;

	public static final String CONTENT_TYPE="Content-Type";
	
	public static final String CONTENT_MD5="Content-MD5";
	
	public static final String TAGS="Tags";
	
	public static final String CRAWLER_CONTROL_FILENAME = ".unicore_metadata_control";

	public MetadataClient(EndpointReferenceType epr, IClientConfiguration sec)
			throws Exception {
		this(epr.getAddress().getStringValue(), epr, sec);
	}

	public MetadataClient(String url, EndpointReferenceType epr, IClientConfiguration sec) throws Exception {
		super(url, epr, sec);
		metadataManagement=makeProxy(MetadataManagement.class);
	}

	/**
	 * get resource metadata and parse it into a map
	 * 
	 * @param resourceName
	 * @throws Exception
	 */
	public Map<String, String> getMetadata(String resourceName)throws Exception{
		return asMap(getRawMetadata(resourceName));
	}
	
	/**
	 * get the "raw" metadata XML for the given resource
	 * 
	 * @param resourceName
	 * @return {@link MetadataType}
	 * @throws Exception
	 */
	public MetadataType getRawMetadata(String resourceName)throws Exception{
		GetMetadataDocument in=GetMetadataDocument.Factory.newInstance();
		in.addNewGetMetadata().setResourceName(resourceName);
		GetMetadataResponseDocument res=metadataManagement.GetMetadata(in);
		MetadataType md=res.getGetMetadataResponse().getMetadata();
		return md;
	}
	
	/**
	 * create new metadata for the given resource
	 * @param resourceName - the resource name
	 * @param meta - the new metadata
	 * @return <code>true</code> if metadata was created
	 * @throws Exception
	 */
	public boolean createMetadata(String resourceName, Map<String,String>meta)throws Exception{
		CreateMetadataDocument req=CreateMetadataDocument.Factory.newInstance();
		req.addNewCreateMetadata().setResourceName(resourceName);
		req.getCreateMetadata().setMetadata(convert(meta));
		CreateMetadataResponseDocument res=metadataManagement.CreateMetadata(req);
		return res.getCreateMetadataResponse().getMetadataCreated();
	}
	
	/**
	 * update metadata for the given resource with the provided new values
	 * @param resourceName - the resource name
	 * @param meta - the new metadata
	 * @return <code>true</code> if metadata was updated
	 * @throws Exception
	 */
	public boolean updateMetadata(String resourceName, Map<String,String>meta)throws Exception{
		UpdateMetadataDocument req=UpdateMetadataDocument.Factory.newInstance();
		req.addNewUpdateMetadata().setResourceName(resourceName);
		req.getUpdateMetadata().setMetadata(convert(meta));
		UpdateMetadataResponseDocument res=metadataManagement.UpdateMetadata(req);
		return res.getUpdateMetadataResponse().getMetadataUpdated();
	}
	
	/**
	 * remove metadata for the given resource
	 * @param resourceName - the resource name
	 * 
	 * @return <code>true</code> if metadata was deleted
	 * @throws Exception
	 */
	public boolean deleteMetadata(String resourceName)throws Exception{
		DeleteMetadataDocument req=DeleteMetadataDocument.Factory.newInstance();
		req.addNewDeleteMetadata().setResourceName(resourceName);
		DeleteMetadataResponseDocument res=metadataManagement.DeleteMetadata(req);
		return res.getDeleteMetadataResponse().getMetadataDeleted();
	}
	
	/**
	 * search metadata
	 * @param searchString - the search string
	 * @param advanced
	 * @return matching resource names
	 * @throws Exception
	 */
	public Collection<String> search(String searchString, boolean advanced)throws Exception{
		SearchMetadataDocument req=SearchMetadataDocument.Factory.newInstance();
		req.addNewSearchMetadata().setSearchString(searchString);
		req.getSearchMetadata().setIsAdvanced(advanced);
		SearchMetadataResponseDocument res=metadataManagement.SearchMetadata(req);
		Set<String>result=new HashSet<String>();
		for(SearchResult sr: res.getSearchMetadataResponse().getSearchResultArray()){
			result.add(sr.getResourceName());
		}
		return result;
	}
	
	/**
	 * federated metadata search 
	 * @param searchString - the search string
	 * @param storagesList
         * @param isAdvanced - whether it is an advanced search
	 * @return TaskClient for monitoring the search and retrieving the result
	 * @throws Exception
	 */
	public TaskClient federatedMetadataSearch(String searchString, String[] storagesList, boolean isAdvanced)throws Exception{
		
		FederatedMetadataSearchDocument federatedMetadataSearchDocument = FederatedMetadataSearchDocument.Factory.newInstance();
		federatedMetadataSearchDocument.addNewFederatedMetadataSearch().setSearchString(searchString);
		if(storagesList!=null && storagesList.length>0){
			federatedMetadataSearchDocument.getFederatedMetadataSearch().setStoragesListArray(storagesList);
		}
		federatedMetadataSearchDocument.getFederatedMetadataSearch().setIsAdvanced(isAdvanced);
		
		FederatedMetadataSearchResponseDocument response = metadataManagement.FederatedMetadataSearch(federatedMetadataSearchDocument);
		EndpointReferenceType epr=response.getFederatedMetadataSearchResponse().getTaskReference();
		TaskClient task = new TaskClient(epr, getSecurityConfiguration());
		
		return task;
	}
	
	/**
	 * start metadata extraction on a single directory
	 *  
	 * @param base - base path
	 * @param depthLimit - depth limit
	 * @return a TaskClient for monitoring
	 * @throws Exception
	 */
	public TaskClient startMetadataExtraction(String base, int depthLimit)throws Exception{
		if(base==null)base="/";
		Pair<String,Integer>dir=new Pair<String, Integer>(base, depthLimit);
		List<String> files = Collections.emptyList();
		return startMetadataExtraction(files, Collections.singletonList(dir));
	}
	
	/**
	 * extract metadata
	 * 
	 * @param files - list of files
	 * @param dirs - list of directories given as path plus depth limit
	 * @throws Exception
	 */
	public TaskClient startMetadataExtraction(List<String> files, List<Pair<String, Integer>>dirs)throws Exception{
		StartMetadataExtractionDocument req=StartMetadataExtractionDocument.Factory.newInstance();
		StartMetadataExtraction sme = req.addNewStartMetadataExtraction();
		if(files!=null){
			for(String file: files){
				sme.addFile(file);
			}
		}
		if(dirs!=null){
			for(Pair<String, Integer> dir: dirs){
				Directory d = sme.addNewDirectory();
				d.setBasePath(dir.getM1());
				d.setDepthLimit(dir.getM2());
			}
		}
		StartMetadataExtractionResponseDocument res=metadataManagement.StartMetadataExtraction(req);
		EndpointReferenceType epr=res.getStartMetadataExtractionResponse().getTaskReference();
		TaskClient task=new TaskClient(epr,getSecurityConfiguration());
		return task;
	}
	
	public static void writeCrawlerControlFile(StorageClient sms, String baseDir, CrawlerControl control)throws Exception{
		FileTransferClient ftc=sms.getImport(baseDir+"/"+CRAWLER_CONTROL_FILENAME, ProtocolType.BFT);
		ftc.writeAllData(IOUtils.toInputStream(control.toString(), "UTF-8"));
		ftc.destroy();
	}
	
	public static class CrawlerControl {
		
		private final String[] includes;
		private final String[] excludes;
		
		private final boolean useDefaultExcludes;

		public CrawlerControl(String[] includes, String[] excludes){
			this(includes,excludes,true);
		}
		
		public CrawlerControl(String[] includes, String[] excludes, boolean useDefaultExcludes){
			this.includes=includes;
			this.excludes=excludes;
			this.useDefaultExcludes=useDefaultExcludes;
		}
		
		public boolean isUseDefaultExcludes() {
			return useDefaultExcludes;
		}

		public String[] getIncludes() {
			return includes;
		}

		public String[] getExcludes() {
			return excludes;
		}
		
		public static CrawlerControl create(Properties p){
			String[]incl=null;
			String[]excl=null;
			
			String exclS=p.getProperty("exclude");
			if(exclS!=null){
				excl=exclS.split(",");
				for(int i=0;i<excl.length;i++){
					excl[i]=excl[i].trim();
				}
			}
			String inclS=p.getProperty("include");
			if(inclS!=null){
				incl=inclS.split(",");
				for(int i=0;i<incl.length;i++){
					incl[i]=incl[i].trim();
				}
			}
			boolean useDefaultExcludes=Boolean.parseBoolean(p.getProperty("useDefaultExcludes","true"));
			return new CrawlerControl(incl, excl, useDefaultExcludes);
		}
		
		public String toString(){
			StringBuilder sb=new StringBuilder();
			boolean first=true;
			if(excludes!=null && excludes.length>0){
				sb.append("exclude=");
				for(String e: excludes){
					if(!first){
						sb.append(",");
					}
					else{
						first=false;
					}
					sb.append(e);
				}
				sb.append("\n\n");
			}
			if(includes!=null && includes.length>0){
				sb.append("include=");
				for(String e: includes){
					if(!first){
						sb.append(",");
					}
					else{
						first=false;
					}
					sb.append(e);
				}
				sb.append("\n\n");
			}
			
			return sb.toString();
		}
	}
	
	public static Map<String,String>asMap(MetadataType meta){
		Map<String,String>result=new HashMap<String, String>();
		if(meta.getContentMD5()!=null){
			result.put(CONTENT_MD5, meta.getContentMD5());
		}
		if(meta.getContentType()!=null){
			result.put(CONTENT_TYPE, meta.getContentType());
		}
		for(TextInfoType t: meta.getPropertyArray()){
			result.put(t.getName(),t.getValue());
		}
		
		String[] tags=meta.getTagArray();
		if(tags!=null && tags.length>0){
			StringBuilder sb=new StringBuilder();
			boolean first=true;
			for(String t: tags){
				if(!first){
					sb.append(",");
				}
				else first=false;
				sb.append(t);
				
			}
			result.put(TAGS, sb.toString());
		}
		return result;
	}
	
	public static MetadataType convert(Map<String,String>meta){
		MetadataType result=MetadataType.Factory.newInstance();
		for(Map.Entry<String, String> md: meta.entrySet()){
			String k=md.getKey();
			String v=md.getValue();
			if(CONTENT_MD5.equals(k))result.setContentMD5(v);
			else if(CONTENT_TYPE.equals(k))result.setContentType(v);
			else if(TAGS.equalsIgnoreCase(k)){
				result.setTagArray(v.split(","));
			}
			else {
				TextInfoType p=result.addNewProperty();
				p.setName(k);
				p.setValue(v);
			}
		}
		return result;
	}
	
}
