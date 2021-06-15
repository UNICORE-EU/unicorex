package de.fzj.unicore.uas.trigger.impl;

import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import de.fzj.unicore.uas.metadata.MetadataManager;
import de.fzj.unicore.uas.metadata.MetadataSupport;
import de.fzj.unicore.uas.trigger.MultiFileAction;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.security.Client;
import eu.unicore.services.Kernel;

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
	
	public ExtractMetadataAction(JSONObject settings, String storageID){
		this.settings=settings;
		this.storageID=storageID;
	}
	
	@Override
	public void fire(IStorageAdapter storage, String filePath, Client client, XNJS xnjs) throws Exception{
		fire(storage,Collections.singletonList(filePath), client, xnjs);
	}

	@Override
	public void fire(IStorageAdapter storage, List<String> files, Client client, XNJS xnjs) throws Exception{
		logger.info("Executing local script for <"+client.getDistinguishedName()+">");
		Kernel kernel = xnjs.get(Kernel.class);
		MetadataManager mm = MetadataSupport.getManager(kernel, storage, storageID);
		mm.startAutoMetadataExtraction(files, null);
	}
	
	public String toString(){
		return "EXTRACT";
	}
}
