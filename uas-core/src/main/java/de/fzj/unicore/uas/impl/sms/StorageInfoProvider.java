package de.fzj.unicore.uas.impl.sms;

import java.util.Map;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.FileSystemDocument;

import de.fzj.unicore.uas.StorageFactory;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.security.Client;

/**
 * provides detailed information about a storage backend for use by the {@link StorageFactory}
 * 
 * @author schuller
 */
public interface StorageInfoProvider {

	/**
	 * get info about the underlying backend - free space etc
	 * @param storageDesc - the storage description
	 * @param client - the client
	 * @param storage - the storage adapter (which may be null! if not available)
	 * @return JSDL FileSystemDocument for inclusion in the WSRF RP document
	 */
	public FileSystemDocument getInformation(StorageDescription storageDesc, Client client, IStorageAdapter storage);
	
	/**
	 * get name and description for each parameter settable by the user when 
	 * invoking the StorageFactory service
	 * 
	 * @param storageDescription
	 * @return a map (which can be <code>null</code>) where key=parameter name and value=parameter description  
	 */
	public Map<String,String> getUserParameterInfo(StorageDescription storageDescription);
	
}
