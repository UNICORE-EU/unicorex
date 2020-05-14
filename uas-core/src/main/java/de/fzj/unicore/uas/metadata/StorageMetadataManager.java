package de.fzj.unicore.uas.metadata;

import de.fzj.unicore.xnjs.io.IStorageAdapter;

/**
 * a {@link MetadataManager} extension which uses a {@link IStorageAdapter} to
 * manage metadata on. All resource names passed to the methods in {@link MetadataManager}
 * are interpreted relative to the root of the storage
 *
 * @author schuller
 */
public interface StorageMetadataManager extends MetadataManager {

	/**
	 * set the storage adapter to be used for storing and retrieving metadata. Effectively
	 * this is the storage for which metadata is managed, since all lookup and extraction 
	 * operations are performed on this storage.
	 * 
	 * @param storage the {@link IStorageAdapter} to use
	 * @param unique - unique ID for disambiguating different storage instances.
	 */
	public void setStorageAdapter(IStorageAdapter storage, String unique);

}
