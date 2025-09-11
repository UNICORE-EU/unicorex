package eu.unicore.client.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.hc.core5.net.URIBuilder;
import org.json.JSONObject;

import eu.unicore.client.Endpoint;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.uas.json.JSONUtil;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * lists files
 * 
 * @author schuller
 */
public class FileList extends BaseServiceClient {

	protected StorageClient parentStorage;

	protected String baseDir;

	public FileList(StorageClient parentStorage, String baseDir, Endpoint endpoint, IClientConfiguration security, IAuthCallback auth) {
		super(endpoint, security, auth);
		this.parentStorage = parentStorage;
		this.baseDir = baseDir;
	}

	public StorageClient getStorage(){
		return parentStorage;
	}

	public List<FileListEntry>list() throws Exception {
		return list(0, 1000);
	}

	public List<FileListEntry>list(int offset, int num) throws Exception {
		String url = bc.getURL();
		URIBuilder ub = new URIBuilder(bc.getURL());
		if(offset>0)ub.addParameter("offset", String.valueOf(offset));
		if(num>0)ub.addParameter("num", String.valueOf(num));
		bc.setURL(ub.build().toString());
		JSONObject props = bc.getJSON();
		bc.setURL(url);
		JSONObject content = props.getJSONObject("content");
		List<FileListEntry> res = new ArrayList<>();
		Iterator<String>children = content.keys();
		while(children.hasNext()){
			String child = children.next();
			res.add(new FileListEntry(child, content.getJSONObject(child)));
		}
		return res;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Files @ ").append(parentStorage.getEndpoint().getUrl()).append("/files/").append(baseDir);
		sb.append("\n");
		try {
			for(FileListEntry fle: list()) {
				sb.append(fle.toString()).append("\n");
			}
		}catch(Exception e) {}
		return sb.toString();
	}

	public static class FileListEntry{
		public String path, owner, group;
		public long size;
		public String lastAccessed;
		public boolean isDirectory;
		public String permissions;
		public Map<String,String> metadata;

		public FileListEntry(String path, JSONObject fromJson){			
			this.lastAccessed = fromJson.optString("lastAccessed",null);
			this.path = path;
			this.owner = fromJson.optString("owner", null); 
			this.group = fromJson.optString("group", null);
			this.size = Long.parseLong(fromJson.optString("size", "-1"));
			this.isDirectory = fromJson.optBoolean("isDirectory", false);
			this.permissions = fromJson.optString("permissions", "r--");
			JSONObject m = fromJson.optJSONObject("metadata");
			metadata = JSONUtil.asMap(m);
			if(m!=null) {
				metadata = JSONUtil.asMap(m);
			}else metadata = new HashMap<>();
		}

		@Override
		public String toString(){
			String d = isDirectory? "d" : "-";
			return String.format("%1$1s %2$-11s %3$18s %4$s %5$-30s",
							d, permissions, String.valueOf(size), lastAccessed, path);
		}

	}

}
