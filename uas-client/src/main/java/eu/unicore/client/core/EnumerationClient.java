package eu.unicore.client.core;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.hc.core5.net.URIBuilder;
import org.json.JSONArray;

import de.fzj.unicore.uas.json.JSONUtil;
import eu.unicore.client.Endpoint;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * handles lists of things (jobs, storages, ...)
 * 
 * @author schuller
 */
public class EnumerationClient extends BaseServiceClient implements Iterable<String> {

	protected String resourcesName;
	
	protected String[] tags;
	
	public EnumerationClient(Endpoint endpoint, IClientConfiguration security, IAuthCallback auth) {
		super(endpoint, security, auth);
		try{
			resourcesName = new File(new URL(endpoint.getUrl()).getPath()).getName();
		}catch(Exception ex){};
	}

	public void setResourcesName(String resourcesName){
		this.resourcesName = resourcesName;
	}

	/**
	 * limit this enumeration to entries with the specified tags
	 */
	public void setDefaultTags(String[] tags) {
		this.tags = tags;
	}
	
	public List<String>getUrls(int offset, int num) throws Exception {
		return getUrls(offset, num, tags);
	}
	
	public List<String>getUrls(int offset, int num, String... tags) throws Exception {
		String url = bc.getURL();
		URIBuilder ub = new URIBuilder(bc.getURL());
		if(offset>0)ub.addParameter("offset", String.valueOf(offset));
		if(num>0)ub.addParameter("num", String.valueOf(num));
		if(tags==null && this.tags!=null) {
			tags = this.tags;
		}
		if(tags!=null && tags.length>0){
			ub.addParameter("tags",JSONUtil.toCommaSeparated(tags));
		}
		bc.setURL(ub.build().toString());
		JSONArray arr = bc.getJSON().getJSONArray(resourcesName);
		bc.setURL(url);
		List<String> res = JSONUtil.toList(arr);
		return res;
	}

	@Override
	public Iterator<String> iterator() {
		
		return new Iterator<String>(){
			private int offset = 0;
			private int num = 0;
				
			private final List<String> avail = new ArrayList<>();
			
			@Override
			public boolean hasNext() {
				if(avail.size()==0)getChunk();
				return avail.size()>0;
			}

			@Override
			public String next() {
				if(avail.size()==0)getChunk();
				if(avail.size()==0)throw new NoSuchElementException();
				return avail.remove(0);
			}
			
			protected void getChunk() {
				if(num==0)num=50;
				try {
					List<String> chunk = getUrls(offset, num, tags); 
					avail.addAll(chunk);
					offset+=chunk.size();
				}catch(Exception ex) {
					throw new RuntimeException(ex);
				}
			}
			
		};
		
	}

	public <T extends BaseServiceClient> T createClient(String url, Class<T> clazz) throws Exception { 
		return clazz.getConstructor(Endpoint.class, IClientConfiguration.class, IAuthCallback.class)
				.newInstance(endpoint.cloneTo(url), security, auth);
	}
	
}
