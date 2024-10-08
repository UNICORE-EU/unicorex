package eu.unicore.xnjs.json;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collection;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.unicore.xnjs.idb.ApplicationInfo;
import eu.unicore.xnjs.idb.IDBImpl;
import eu.unicore.xnjs.idb.IDBParser;
import eu.unicore.xnjs.idb.Partition;
import eu.unicore.xnjs.util.JSONUtils;
import eu.unicore.xnjs.util.LogUtil;

public class JsonIDB implements IDBParser {

	protected static final Logger logger=LogUtil.getLogger(LogUtil.JOBS,IDBImpl.class);

	final IDBImpl idb;
	
	String data;
	
	public JsonIDB(IDBImpl idb) {
		this.idb = idb;
	}

	public JsonIDB(IDBImpl idb, String data) {
		this.idb = idb;
		this.data = data;
	}
	
	public void handleFile(File file, boolean singleFile) throws Exception {
		try(FileInputStream fis = new FileInputStream(file)){
			JSONObject jsonidb = new JSONObject(IOUtils.toString(fis, "UTF-8"));
			boolean isIDB = singleFile || jsonidb.optJSONObject("Partitions")!=null;
			boolean readAllInfo = idb.getMainFile()==null || file.getAbsolutePath().equals(idb.getMainFile().getAbsolutePath());
			if(readAllInfo && isIDB){
				logger.info("Reading main IDB <"+file+">");
				readIDB(jsonidb);
			}
			else {
				logger.info("Reading apps from <"+file+">");
				// only read apps definitions
				readApplications(jsonidb, idb.getIdb());
			}
		}
	}

	// read the main IDB file containing partition definitions 
	// and applications
	protected void readIDB(JSONObject jsonidb) throws Exception {
		readPartitions(jsonidb.optJSONObject("Partitions"));
		readApplications(jsonidb, idb.getIdb());
		readInfo(jsonidb.optJSONObject("Info"));
		idb.setScriptHeader(getScriptHeader(jsonidb));	
	}

	private String getScriptHeader(JSONObject jsonidb) {
		String scriptHeader = JSONUtils.readMultiLine("ScriptHeader", null, jsonidb);
		String execTemplate = JSONUtils.readMultiLine("SubmitScriptTemplate", null, jsonidb);
		if(execTemplate==null) {
			execTemplate  = JSONUtils.readMultiLine("ExecuteScriptTemplate", null, jsonidb);
		}
		if(execTemplate!=null) {
			logger.warn("DEPRECATION: IDB uses deprecated settings "
					+ "ExecuteScriptTemplate / SubmitScriptTemplate. "
					+ "These are superseded by the 'ScriptHeader' element.");
			if(scriptHeader==null) {
				scriptHeader = execTemplate;
			}
		}
		return scriptHeader;
	}
	
	protected void readPartitions(JSONObject source) throws Exception {
		if(source!=null) {
			boolean haveDefault = false;
			for(String name: source.keySet()) {
				Partition p = JSONParser.parsePartition(name, source.getJSONObject(name));
				idb.getPartitionsInternal().add(p);
				logger.info("Read: <"+p+">");
				if(p.isDefaultPartition()) {
					if(haveDefault) {
						logger.warn("Multiple 'default' partitions defined - this may lead to unintended behaviour.");
					}
					else {
						logger.info("Default partition: <"+name+">");
						haveDefault = true;
					}
				}
			}
		}
		if(idb.getPartitionsInternal().size()==0) {
			logger.info("No partitions defined - adding generic definition allowing any requests.");
			Partition p = new Partition();
			p.setName("*");
			p.setDescription("Accepts any Queue / partition request");
			idb.getPartitionsInternal().add(p);
		}
	}

	protected void readApplications(InputStream source, Collection<ApplicationInfo> idb) throws Exception {
		readApplications(new JSONObject(IOUtils.toString(source, "UTF-8")), idb);
	}

	protected void readApplications(JSONObject source, Collection<ApplicationInfo> idb) throws Exception {
		// try array of applications first
		JSONArray apps = source.optJSONArray("Applications");
		if(apps!=null) {
			for(int i=0; i<apps.length();i++) {
				idb.add(JSONParser.parseApplicationInfo(apps.getJSONObject(i)));
			}
		}
		else {
			if(source.optString("Name", null)!=null) {
				idb.add(JSONParser.parseApplicationInfo(source));
			}
		}
	}

	public void readApplications(Collection<ApplicationInfo> idb) throws Exception {
		if(data==null)throw new IllegalStateException("no data!");
		readApplications(new JSONObject(data), idb);
	}

	protected void readInfo(JSONObject source) throws Exception {
		if(source==null)return;
		for(String name: source.keySet()) {
			idb.getTextInfoPropertiesNoUpdate().put(name, source.getString(name));
		}
	}

}
