package eu.unicore.uas.trigger.impl;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import eu.unicore.security.Client;
import eu.unicore.services.Kernel;
import eu.unicore.uas.metadata.MetadataManager;
import eu.unicore.uas.metadata.MetadataSupport;
import eu.unicore.uas.trigger.MultiFileAction;
import eu.unicore.uas.util.LogUtil;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.io.IStorageAdapter;

/**
 * extracts metadata from a list of files
 * 
 * @author schuller
 */
public class ExtractMetadataAction extends BaseAction implements MultiFileAction {

	private static final Logger logger = LogUtil.getLogger(LogUtil.TRIGGER, ExtractMetadataAction.class);

	@SuppressWarnings("unused")
	private final JSONObject settings;

	private final String storageID;

	private List<String>files;

	public ExtractMetadataAction(JSONObject settings, String storageID){
		this.settings=settings;
		this.storageID=storageID;
	}

	@Override
	public void setTarget(List<String>target) {
		this.files = target;
	}

	@Override
	public String run(IStorageAdapter storage, Client client, XNJS xnjs) throws Exception{
		logger.info("Launching extract metadata for <{}>", client.getDistinguishedName());
		Kernel kernel = xnjs.get(Kernel.class);
		MetadataManager mm = MetadataSupport.getManager(kernel, storage, storageID);
		mm.startAutoMetadataExtraction(files, null);
		return null;
	}

	@Override
	public String toString(){
		return "EXTRACT";
	}

}