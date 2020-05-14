package de.fzj.unicore.uas.client;

import java.util.Map;

/**
 * used by {@link StorageClient} to configure {@link FileTransferClient} instances after creation
 *
 * @author schuller
 */
public interface Configurable {

	/**
	 * invoked after creation of the transfer
	 */
	public void configure(Map<String,String>params);
	
}
