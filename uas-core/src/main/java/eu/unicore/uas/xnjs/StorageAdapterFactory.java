package eu.unicore.uas.xnjs;

import eu.unicore.uas.impl.BaseResourceImpl;
import eu.unicore.xnjs.io.IStorageAdapter;

/**
 * Used to create the correct IStorageAdapter for a given
 * Resource<br/>
 * 
 * <em>NOTE: implementations need a no-args constructor!</em>
 *
 * @author schuller
 */
public interface StorageAdapterFactory {

	public IStorageAdapter createStorageAdapter(BaseResourceImpl parent)throws Exception;

}
